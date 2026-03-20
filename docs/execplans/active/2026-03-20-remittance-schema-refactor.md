# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- 기존 설계 문서:
  - `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`
  - `docs/execplans/active/2026-03-20-remittance-erd-review.md`
  - `docs/reviews/active/2026-03-18-remittance-async-design-note.md`
- 현재 구현 기준:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/resources/application.yml`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`

# Goal
remittance + jobs 스키마를 1차 리팩토링한다. 목표는 transfer snapshot 기반 이력 보존, recipient 중복 방지의 DB 책임 강화, remittance 운영 조회 인덱스 보강, JPA 조회 경로 단순화다. Flyway는 도입하지 않고, 개발 DB reset 가능 전제에서 목표 스키마를 먼저 고정한 뒤 엔티티와 서비스/테스트를 맞춘다.

# In Scope
- `transfers` 스키마 보강
  - recipient snapshot 컬럼 추가
  - 운영 조회용 상태 인덱스 보강
- `recipients` 스키마 보강
  - `(user_id, wallet_address)` unique 제약 추가
  - 사용자/정렬 조회에 맞는 인덱스 추가
- `user_wallets` 저장 방식 보정
  - assigned id 엔티티 저장 안정성 검토 반영
- transfer list/detail 응답을 recipient live join 의존에서 snapshot 중심으로 전환
- remittance/jobs 관련 수동 SQL baseline 파일 추가
- remittance 관련 테스트 보강

# Out of Scope
- 테이블/컬럼의 대규모 rename (`recipients` -> `recipient_contacts`) 
- Flyway/Liquibase 도입
- vault 도메인 정리
- 모바일/웹 remittance 화면 변경
- 전체 프로젝트 스키마를 `validate` 모드로 전환

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Transfer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Recipient.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/UserWallet.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/RecipientRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/TransferRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RecipientService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittanceOpsService.java`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`

## Mobile
- 직접 구현 없음
- transfer list/detail 응답 shape가 유지되는지 확인만 필요

## Docs
- `docs/execplans/active/2026-03-20-remittance-schema-refactor.md`
- `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`
- 수동 reset 기준 SQL: `deploy/sql/2026-03-20-remittance-jobs-baseline.sql`

## Shared
- `ApiResponse<T>` envelope 유지
- 기존 auth/security 계약 유지

# Contract Changes
- API 필드명은 가능하면 유지한다.
- `TransferListItemResponse.recipientAlias`, `TransferDetailResponse.recipientAlias`의 값 source가 current recipient가 아니라 transfer snapshot으로 바뀐다.
- `TransferDetailResponse.recipientId`는 계속 유지하되, recipient가 이후 수정되더라도 과거 alias/address는 변하지 않게 한다.
- 관리자 운영 조회 계약은 필드 shape 유지 우선, 내부 source만 정리한다.
- DB 계약 변경:
  - `transfers.recipient_alias_snapshot` 추가
  - `transfers.recipient_relation_snapshot` 추가
  - `recipients` unique/index 추가
  - `transfers` status/index 추가

# Security Notes
- remittance, admin remittance auth 규칙은 유지한다.
- encrypted private key, signed transaction 저장 방식은 변경하지 않는다.
- schema refactor 중에도 자기 자신 송금 금지, allowlist, idempotency, active transfer 직렬화 규칙은 유지해야 한다.
- 수동 SQL 적용 시 production용 무단 자동 실행은 피하고 개발 reset 기준으로만 사용한다.

# Maintainability Notes
- 1차 리팩토링에서는 테이블 rename이나 aggregate 재명명보다 snapshot/제약/조회 경로 정리에 집중한다.
- `Transfer`는 이력 aggregate, `Recipient`는 주소록 aggregate라는 경계를 코드에 반영한다.
- JPA 연관관계는 DB FK 생성 또는 명시적 탐색이 꼭 필요한 경우에만 최소로 추가한다.
- live recipient join 제거를 통해 transfer 응답 조립을 단순화하고, 이후 N+1 위험을 줄인다.

# Implementation Steps
1. remittance schema 1차 목표를 SQL 파일로 고정한다.
2. `Transfer`에 recipient snapshot 필드를 추가하고 factory/test를 갱신한다.
3. `Recipient`에 unique/index, 필요 최소 FK 힌트를 반영한다.
4. `UserWallet` assigned id 저장 보정이 필요하면 반영한다.
5. `TransferService` 목록/상세 응답을 snapshot 기반으로 바꾸고 recipient 추가 조회를 제거한다.
6. 운영 조회(`RemittanceOpsService`)가 새 인덱스와 필드를 자연스럽게 사용하도록 정리한다.
7. integration/unit test를 snapshot 회귀와 unique 제약 기준으로 보강한다.
8. remittance 관련 테스트를 실행한다.

# Test Plan
- `cd apps/dondone-backend && ./gradlew test --tests '*remittance*'`
- 추가/갱신 대상:
  - recipient 수정 후 기존 transfer list/detail alias 불변성
  - duplicate recipient wallet의 DB 제약 기반 충돌 처리
  - transfer list가 recipient live join 없이도 동일 계약을 유지하는지
  - 기존 admin summary/transfers/jobs 응답 회귀
  - transfer state machine factory 시그니처 변경 회귀

# Review Focus
- transfer snapshot이 실제로 recipient 수정 후에도 불변하게 유지되는지
- recipients unique/index와 transfers status/index가 현재 조회 패턴과 맞는지
- JPA 연관관계 추가가 불필요한 N+1 경로를 만들지 않았는지
- 수동 SQL baseline과 엔티티가 서로 어긋나지 않는지

# Worktree Split Decision
- Single lane

shared DTO, shared entities, admin/user remittance contracts가 함께 움직이므로 병렬 안전하지 않다. 스키마, 엔티티, 서비스, 테스트를 한 레인에서 맞추는 편이 안전하다.

# Commit Plan
1. 실행 계획 및 수동 SQL baseline 추가
2. remittance schema/entity/service refactor
3. remittance 테스트 보강

# Open Questions
- `user_wallets`의 surrogate key 전환은 다음 단계에서 할지 여부
- `jobs.reference_kind`를 remittance 외 다른 async 도메인과 공통 규칙으로 확장할지 여부

# Assumptions
- 개발 DB reset 가능
- Flyway는 도입하지 않음
- 1차 목표는 remittance 핵심 회귀 제거와 DB 책임 강화이며, 권장 ERD 전체 rename은 후속 단계다
