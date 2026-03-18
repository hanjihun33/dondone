# 목표
- 기존 `ddl-auto` 산출물을 버리고, 현재 프로젝트 구조와 P0 문서를 기준으로 재정의한 baseline 생성 스키마 SQL을 작성한다.
- baseline 스키마 기준으로 현재 JPA 엔티티와 목표 ERD의 차이표를 정리한다.

# 범위
- `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `jobs` 도메인의 이상적 테이블 구조를 정의한다.
- 현재 엔티티/Repository 구조와 PRD/API/기능 명세를 대조해 누락 도메인과 과도하게 임시적인 구조를 정리한다.
- PostgreSQL 기준 생성 스키마 SQL을 작성한다.
- 설계 의도, 포함/제외 기준, 주요 인덱스/트랜잭션 포인트를 설명하는 문서를 작성한다.
- 현재 JPA 엔티티 / baseline ERD 차이표 문서를 작성한다.

# 범위 제외
- Demo Time Travel 관련 테이블
- demo seed/state/asOf 재현용 테이블
- employer support, worker summary, integrity anchor의 구체 스키마
- Flyway/Liquibase 실제 도입 및 애플리케이션 연동
- JPA 엔티티/Repository 코드 리팩터링

# 영향 모듈
## 문서
- `docs/DonDone_PRD_v1.5.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/DonDone_P0_Functional_Spec_v0.md`
- `docs/infra/DB_INVENTORY.md`
- 신규: `docs/infra/DB_BASELINE_SCHEMA.md`
- 신규: `docs/infra/DB_JPA_ERD_DIFF.md`

## SQL
- 신규: `deploy/sql/001_baseline_schema.sql`

# 계약 변경
- 런타임 API 변경은 없음
- 이번 작업은 baseline 스키마 산출물 작성이다
- 현재 JPA 매핑과는 일부 테이블명/정규화 기준이 달라질 수 있다

# 보안 메모
- 모든 사용자 소유 리소스는 `user_id` 또는 소유권 체인으로 조회 가능해야 한다
- 문서 다운로드는 파일 직접 노출 대신 별도 URL 발급을 전제로 저장 구조를 설계한다
- 송금은 allowlist, cooldown, idempotency, 비동기 처리 전제를 스키마에 반영한다

# 구현 단계
1. PRD/API/기능 명세에서 P0 persistence 대상 도메인과 제외 범위를 확정한다.
2. 현재 코드 구조와 문서 차이를 정리해, 유지할 aggregate와 재설계할 aggregate를 구분한다.
3. 테이블/관계/상태/인덱스/비동기 잡 기준을 포함한 baseline 설계 원칙을 확정한다.
4. 설계 메모 문서를 작성한다.
5. 실제 PostgreSQL 생성 스키마 SQL을 작성한다.
6. 문서와 SQL을 교차 검토해 누락, 과도한 추측, 인덱스 누락을 수정한다.
7. 현재 JPA 엔티티와 baseline ERD 차이표를 정리한다.

# 테스트 계획
- 문서/SQL 작업이므로 애플리케이션 테스트는 생략한다
- 수동 검증:
  - API 계약에 나온 persistence 도메인이 모두 반영되었는지 확인
  - 제외 대상이 SQL에 섞여 들어가지 않았는지 확인
  - idempotency, async job, ownership, lookup 패턴을 만족하는 인덱스가 들어갔는지 확인

# 리뷰 포인트
- 현재 엔티티의 임시 구조를 그대로 복제하지 않았는지
- 문서에 없는 speculative 테이블을 과도하게 넣지 않았는지
- WorkProof 수정/첨부/감사 흐름이 추적 가능하게 분리되었는지
- Documents/Remittance async 흐름이 jobs와 함께 일관되게 설계되었는지
- month, timestamp, money, token amount 타입 선택이 일관적인지

# 워크트리 분리 결정
- 단일 레인 작업
- SQL과 설계 메모가 강하게 연결되어 있어 병렬 분리는 부적절하다

# 커밋 계획
1. 실행 계획을 baseline schema 작성 범위로 갱신
2. baseline schema 설계 메모 추가
3. PostgreSQL baseline 생성 스키마 SQL 추가
4. JPA 엔티티 / baseline ERD 차이표 문서 추가
