# Remittance ERD Refactor Note

## 목적
- 현재 remittance JPA 엔티티가 실제로 어떤 테이블 구조를 만들고 있는지 텍스트 ERD로 정리한다.
- 현재 구조의 문제를 명확히 적고, 권장 ERD와 리팩토링 고려사항을 한 문서에 남긴다.
- 이후 migration, entity refactor, repository query 정리를 위한 기준점으로 사용한다.

## 범위와 전제
- 기준 코드:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- 현행 설정은 `spring.jpa.hibernate.ddl-auto=update` 이다.
- Flyway는 도입하지 않았고, 개발 DB reset 또는 수동 SQL 적용을 전제로 schema-first 기준을 맞춘다.
- 아래 "현재 ERD"는 현재 엔티티와 `deploy/sql/2026-03-20-remittance-jobs-baseline.sql` 기준의 1차 refactor 상태를 설명한다.

## 현재 ERD

### 현재 reset 기준 ERD
```text
users
  PK id
  email
  password_hash
  name
  phone_number
  role
  created_at
  updated_at

user_wallets
  PK user_id
  FK user_id -> users.id
  wallet_address
  encrypted_private_key
  funding_status
  funding_failure_reason
  funded_at
  created_at
  updated_at

recipients
  PK recipient_id
  FK user_id -> users.id
  FK target_user_id -> users.id nullable
  alias
  relation
  wallet_address
  allowed
  created_at
  updated_at
  UQ user_id + wallet_address

transfers
  PK transfer_id
  FK user_id -> users.id
  FK recipient_id -> recipients.recipient_id
  asset_symbol
  amount_atomic
  sender_address
  recipient_address
  recipient_alias_snapshot
  recipient_relation_snapshot
  recipient_target_user_id_snapshot nullable
  status
  idempotency_key
  high_amount_confirmed
  recent_recipient_confirmed
  tx_hash
  signed_transaction
  failure_code
  created_at
  updated_at

jobs
  PK id
  job_type
  reference_kind
  reference_id
  active_key
  status
  attempt_count
  run_at
  last_error
  created_at
  updated_at

logical relationships
  users 1 --- 0..1 user_wallets
  users 1 --- N recipients as owner
  users 1 --- 0..N recipients as target
  users 1 --- N transfers
  recipients 1 --- N transfers
  transfers 1 --- 0..N jobs   (generic queue uses reference_kind + reference_id)
```

### 현재 실제 DB 관점의 해석
- 개발 DB를 reset 하거나 baseline SQL을 수동 적용하면 `user_wallets`, `recipients`, `transfers`의 핵심 FK와 unique/index를 맞출 수 있다.
- 현재 코드 기준으로 recipient 중복 방지, transfer snapshot 보존, job reference kind 구분은 DB 제약과 서비스 로직이 함께 책임지는 상태다.
- 다만 `ddl-auto=update` 이므로 reset 없이 오래 살아 있는 개발 DB는 엔티티 의도와 실제 스키마가 어긋날 수 있다.
- `jobs.reference_id` 는 여전히 polymorphic 문자열이라 DB 차원 FK를 강하게 둘 수 없다.

## 현재 구조의 핵심 문제

### 1. 1차 snapshot 보강은 됐지만 aggregate 경계는 아직 완전하지 않다
- `TransferService` 목록/상세 응답은 이제 `recipient_alias_snapshot`, `recipient_address` 를 기준으로 조립한다.
- 따라서 recipient 수정 후 과거 송금 alias/address가 바뀌는 문제는 1차로 해소됐다.
- 하지만 `transfers.recipient_id` 는 여전히 필수 FK이고 recipient 삭제 정책도 정리되지 않았다.
- 즉, transfer history가 주소록과 느슨하게 연결되는 최종 단계까지는 아직 남아 있다.

### 2. user 중심 ownership 과 transfer 중심 history 의 경계가 부분적으로만 정리됐다
- `user_id` 를 기준으로 권한과 목록을 관리하는 것은 유지된다.
- `Transfer` 는 snapshot을 갖는 history aggregate 쪽으로 옮겨졌지만, `Recipient` 명칭과 키 구조는 여전히 주소록/연락처 aggregate로 명확히 드러나지 않는다.
- 추후 `recipient_contacts`, `owner_user_id`, optional `recipient_contact_id` 같은 명시적 모델로 더 정리할 여지가 있다.

### 3. 무결성이 개선됐지만 reset discipline 없이 유지되지는 않는다
- recipient 중복 방지는 `(user_id, wallet_address)` unique 제약으로 DB가 함께 책임진다.
- transfer idempotency, active job dedupe도 unique/active key와 서비스 로직을 함께 쓴다.
- 다만 현재는 migration tool 없이 `ddl-auto=update`를 유지하므로, 개발 DB reset이나 baseline SQL 적용 없이 스키마가 항상 같은 상태라고 가정할 수는 없다.

### 4. JPA 읽기 경로는 단순해졌지만 연관 추가에 따른 N+1 위험은 남아 있다
- 현재 hot path는 transfer snapshot과 스칼라 FK 위주라 송금 목록/상세에서 live recipient join이 빠졌다.
- 동시에 `UserWallet.user`, `Recipient.user/targetUser`, `Transfer.user/recipient` 같은 읽기 전용 `LAZY` 연관이 추가됐다.
- 지금 DTO 조립은 이 연관을 직접 따라가지 않아서 안전하지만, 이후 관리자 화면이나 목록 매핑에서 무심코 getter를 열면 N+1 또는 `LazyInitializationException`이 바로 생길 수 있다.

## 권장 설계 원칙
- 권한, 테넌시, 목록 필터링 기준은 `user` 중심으로 유지한다.
- 주소록(`recipient contact`)과 송금 이력(`transfer history`)은 분리한다.
- `transfer` 는 송금 시점의 핵심 정보를 snapshot 으로 보존한다.
- FK와 unique, index는 DB가 책임지고, 서비스는 도메인 규칙을 책임진다.
- JPA 엔티티 그래프를 크게 만들기보다, 쓰기 모델과 읽기 모델을 구분한다.

## 권장 ERD

### 권장 논리 ERD
```text
users
  PK id
  email
  password_hash
  name
  phone_number
  role
  created_at
  updated_at

user_wallets
  PK wallet_id
  FK user_id -> users.id
  chain_type
  wallet_address
  encrypted_private_key
  funding_status
  funding_failure_reason
  funded_at
  created_at
  updated_at
  UQ user_id              -- MVP에서 사용자당 주 송신 지갑 1개
  UQ wallet_address

recipient_contacts
  PK recipient_contact_id
  FK owner_user_id -> users.id
  FK target_user_id -> users.id  nullable
  alias
  relation
  wallet_address
  allowed
  created_at
  updated_at
  UQ owner_user_id + wallet_address

transfers
  PK transfer_id
  FK sender_user_id -> users.id
  FK sender_wallet_id -> user_wallets.wallet_id
  FK recipient_contact_id -> recipient_contacts.recipient_contact_id  nullable, on delete set null
  asset_symbol
  amount_atomic
  sender_address_snapshot
  recipient_wallet_address_snapshot
  recipient_alias_snapshot
  recipient_relation_snapshot
  recipient_target_user_id_snapshot  nullable
  status
  idempotency_key
  high_amount_confirmed
  recent_recipient_confirmed
  tx_hash
  signed_transaction
  failure_code
  created_at
  updated_at
  UQ sender_user_id + idempotency_key
  UQ tx_hash nullable

jobs
  PK id
  job_type
  reference_kind
  reference_id
  active_key
  status
  attempt_count
  run_at
  last_error
  created_at
  updated_at

relationships
  users 1 --- N user_wallets
  users 1 --- N recipient_contacts as owner
  users 1 --- N recipient_contacts as target (optional)
  users 1 --- N transfers as sender
  user_wallets 1 --- N transfers
  recipient_contacts 1 --- 0..N transfers
  transfers 1 --- 0..N jobs   (jobs is still polymorphic, use reference_kind + reference_id)
```

### 권장 이유
- `recipient_contacts` 는 주소록이다.
- `transfers` 는 감사 가능한 불변 이력이다.
- 과거 이력은 snapshot 컬럼으로 보존하고, 현재 주소록과는 약하게 연결한다.
- `recipient_contact_id` 는 편의 연결일 뿐 필수 source of truth가 아니다.
- recipient를 수정하거나 삭제해도 과거 transfer 상세는 그대로 유지된다.

## 왜 user 중심만으로 밀면 안 되는가
- 송금은 user 소유 리소스이지만 동시에 audit/history 리소스다.
- 따라서 "모든 걸 user 하위 테이블"처럼만 보면 과거 이력의 불변성과 복구 추적성이 약해진다.
- 올바른 방향은 다음 조합이다.
  - access control: `sender_user_id`
  - write ownership: `sender_user_id`, `sender_wallet_id`
  - history integrity: snapshot columns
  - address book convenience: `recipient_contact_id`

## 테이블별 권장 제약과 인덱스

### `user_wallets`
- `unique(user_id)` if MVP에서 사용자당 1개
- `unique(wallet_address)`
- `index(funding_status, updated_at desc)`

### `recipient_contacts`
- `unique(owner_user_id, wallet_address)`
- `index(owner_user_id, updated_at desc, recipient_contact_id desc)`
- `index(target_user_id)` nullable

### `transfers`
- `unique(sender_user_id, idempotency_key)`
- `unique(tx_hash)` nullable
- `index(sender_user_id, created_at desc, transfer_id desc)`
- `index(sender_user_id, status)`
- `index(status, updated_at desc, transfer_id desc)`
- `index(recipient_contact_id, created_at desc)` optional

### `jobs`
- generic queue 를 유지한다면:
  - `index(status, run_at, id)`
  - `index(reference_kind, reference_id, job_type)`
  - `unique(active_key)`
- 특정 도메인 비중이 커지면 remittance 전용 job table 분리도 검토 가능

## JPA 관점 설계 가이드

### 기본 원칙
- FK를 추가해도 엔티티를 무조건 양방향 그래프로 만들 필요는 없다.
- hot path 에서는 `@ManyToOne(fetch = LAZY)` 와 projection/query DTO를 우선 사용한다.
- `@OneToMany` 컬렉션은 목록 API에 거의 필요 없으므로 가능한 한 만들지 않는다.
- 기본 fetch 전략을 EAGER로 두지 않는다.

### N+1 관점에서 권장하는 방향
- `Transfer -> RecipientContact`, `Transfer -> UserWallet`, `RecipientContact -> User` 정도만 단방향 `LAZY` 를 검토한다.
- 목록 조회는 projection 또는 JPQL/Querydsl DTO 조회로 한 번에 가져온다.
- 상세 조회에서만 필요한 연관은 `fetch join` 또는 `@EntityGraph` 로 명시적으로 연다.
- mapper/response 조립 시 루프 안에서 연관 getter 를 반복 호출하지 않는다.
- `hibernate.default_batch_fetch_size` 또는 `@BatchSize` 는 보조 수단으로만 쓰고, 주된 해결책은 아니다.

### 현재 코드와 연결한 해석
- transfer 목록/상세는 이미 snapshot만 사용하므로 recipient 재조회 N+1 경로는 제거됐다.
- recipient 목록과 phone search는 여전히 단일 테이블 또는 명시적 조합 조회로 유지하는 편이 안전하다.
- 앞으로 현재 주소록 상태가 꼭 필요한 경우에만 projection query 또는 명시적 fetch join을 사용하고, 기본 응답은 snapshot/source of truth를 섞지 않는 방향이 낫다.

### 추천 조회 패턴
- 송금 목록: `transfers` snapshot 기반 단일 조회
- 송금 상세: `transfers` 단일 조회, 필요 시 recipient contact를 선택적으로 join
- recipient 목록: `recipient_contacts` 단일 조회
- 운영 stuck transfer 목록: `transfers` 상태 인덱스 기반 조회
- phone search: `users + user_wallets` projection query

## ERD 리팩토링 과정에서 고려할 것

### 1. reset 기준 schema-first 운영
- Flyway 없이 가더라도 기준 DDL은 `deploy/sql/2026-03-20-remittance-jobs-baseline.sql` 하나로 고정하는 편이 낫다.
- 개발 DB는 reset 후 baseline 또는 엔티티 기준으로 다시 세우고, drift 상태에서 부분 제약만 덧대는 방식은 피해야 한다.

### 2. 데이터 정리 선행
- 이미 존재할 수 있는 중복 recipient, orphan transfer, orphan wallet 을 먼저 점검해야 한다.
- unique/FK 추가 전에 cleanup SQL 이 필요할 수 있다.

### 3. 단계적 롤아웃
- 한 번에 컬럼 rename 과 not null 을 모두 넣지 않는다.
- 권장 순서:
  1. 기준 DDL 확정과 reset
  2. snapshot/FK/unique/index 반영
  3. 읽기 코드 전환
  4. 테스트로 회귀 고정
  5. 후속 rename 또는 선택적 FK 완화 검토

### 4. delete policy
- `recipient_contact` 삭제가 과거 `transfer` 삭제로 이어지면 안 된다.
- 현재 `transfers.recipient_id` 필수 FK 구조에서는 recipient hard delete가 어렵다.
- 장기적으로는 `optional recipient_contact_id + snapshot` 또는 soft delete 정책 중 하나를 명시해야 한다.

### 5. enum 안정성
- `status`, `failure_code`, `relation`, `funding_status` 는 DB 문자열 enum 성격으로 쓰인다.
- enum rename 은 곧 데이터 마이그레이션이므로 릴리즈 절차를 같이 잡아야 한다.

### 6. 금액/시간 표현
- 토큰 금액은 계속 atomic unit 정수로 유지하는 편이 안전하다.
- timestamp 는 `LocalDateTime` 유지 여부를 점검하고, 운영/감사 요구가 있으면 timezone 정책을 명확히 해야 한다.

### 7. 민감정보
- `encrypted_private_key`, `signed_transaction` 은 column length, 암호화 방식, 로그 노출 금지 기준을 함께 검토해야 한다.
- 스키마 리팩토링 중에도 dump/log/test fixture 에 민감정보가 새지 않게 해야 한다.

### 8. 동시성
- recipient 중복 등록, idempotency replay, active transfer 차단은 DB 제약과 락이 함께 맞물려야 한다.
- `exists -> insert` 패턴만으로는 충분하지 않다.

### 9. 테스트 전략
- recipient 중복 등록 동시성 테스트
- recipient 수정 후 transfer snapshot 응답 회귀 테스트
- FK/unique 위반 시 에러 맵핑 테스트
- recipient 수정/삭제 후 과거 transfer 상세 불변성 테스트
- status/index 기반 운영 조회 성능 smoke test

## 권장 리팩토링 순서
1. 현재 reset 기준 baseline SQL과 엔티티를 일치시킨다.
2. `Transfer` snapshot 응답 경로와 idempotency/job dedupe를 테스트로 고정한다.
3. `Recipient`의 target user 연결과 unique 제약을 유지한 채 주소록 aggregate 역할을 분리한다.
4. `user_wallets` surrogate key 도입 여부를 결정하고, 필요하면 `wallet_id` 를 후속 단계에서 추가한다.
5. `Recipient` rename (`recipient_contacts`) 과 ownership 명시 (`owner_user_id`) 는 2차 리팩토링으로 검토한다.
6. `transfers.recipient_id` 를 optional contact FK로 바꿀지, soft delete 정책으로 갈지 결정한다.
7. jobs가 remittance 외 도메인으로 확대되면 `reference_kind` 기반 generic queue 유지 여부를 다시 검토한다.

## 한 줄 결론
- 조회 권한은 user 중심으로 유지한다.
- 주소록은 `recipient_contacts`, 송금은 `transfers` 불변 이력으로 분리한다.
- JPA 리팩토링은 "연관관계 추가"보다 "snapshot, FK, unique, index, projection query" 순서로 접근하는 편이 안전하다.
