# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- 사전 설계 문서:
  - `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`
  - `docs/execplans/active/2026-03-20-remittance-erd-review.md`
- 현재 구현:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
  - `apps/dondone-backend/src/main/resources/application.yml`
- 테스트:
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`

# Goal
DonDone remittance + jobs 도메인을 schema-first 기준으로 정리한다. 이번 작업은 개발 DB reset 가능, Flyway 미사용 전제를 둔다. 목표는 transfer history snapshot 보존, recipient 중복 방지와 내부 사용자 연결 보존, jobs reference 구분 명확화, 조회 패턴에 맞는 인덱스 보강이다.

# In Scope
- remittance entity/repository/service/query 리팩토링
- jobs entity/repository/service/worker 내부 reference 구조 리팩토링
- recipient snapshot/unique/FK/index 강화
- transfer snapshot/조회 방식 정리
- user_wallet assigned-id 저장 보정
- 관련 DTO/응답 조립 로직 정리
- remittance/jobs 테스트 보강
- 관련 설계 문서 갱신

# Out of Scope
- Flyway/Liquibase 도입
- 전체 도메인 스키마 baseline 생성
- mobile/web 계약 변경
- vault 도메인 구현 또는 병합
- remittance endpoint 추가/삭제

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/dto/response/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`

## Mobile
- 직접 변경 없음
- 기존 remittance 응답 shape 유지가 우선

## Docs
- `docs/execplans/active/2026-03-20-remittance-jobs-schema-first-refactor.md`
- `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`

## Shared
- `ApiResponse<T>` envelope 유지
- JWT auth 흐름 유지

# Contract Changes
- 외부 요청 DTO 유지
- 외부 응답 DTO는 기존 shape를 최대한 유지
- 내부 persistence 계약 변경
  - `Recipient` 에 `target_user_id` 저장
  - `Transfer` 에 recipient snapshot 컬럼 추가
  - `Job` 에 reference kind/type 구분 컬럼 추가
- 동작 계약 변경
  - transfer 목록/상세는 current recipient 조회보다 transfer snapshot을 우선 사용
  - recipient 수정이 과거 transfer alias/address 의미를 바꾸지 않음

# Security Notes
- remittance endpoint auth/authz 범위 변경 없음
- 기존 `404` 은닉 규칙 유지
- private key / signed tx 컬럼은 기존 민감정보 보호 방식을 유지
- schema refactor 중에도 logs/response에 민감정보 추가 노출 금지

# Maintainability Notes
- recipient address book 과 transfer history 의 ownership을 분리한다.
- DB FK/unique/index 강화와 JPA 연관관계 추가를 같은 의미로 취급하지 않는다.
- hot path 조회는 snapshot 또는 projection 기반으로 유지하고, 양방향 연관은 도입하지 않는다.
- jobs 는 generic queue 구조를 유지하되 reference 의미를 명확히 해 future domain 확장 시 충돌을 줄인다.

# Implementation Steps
1. remittance/jobs 목표 스키마를 엔티티 수준에서 확정한다.
2. `Recipient` 에 내부 사용자 연결 컬럼과 unique/index/FK를 반영한다.
3. `Transfer` 에 recipient snapshot 컬럼과 상태/운영 조회 인덱스를 반영한다.
4. `TransferService` 를 snapshot 기반 응답 조립으로 바꾸고 current recipient 재조회 의존을 제거한다.
5. `UserWallet` 에 assigned-id 저장 보정을 반영한다.
6. `Job` 에 reference kind/type 구분과 인덱스/active key 구성을 반영한다.
7. `JobService`, `RemittanceJobWorker`, `RemittanceOpsService`, 관련 repository 메서드를 새 job reference 구조에 맞춘다.
8. remittance integration/unit 테스트를 갱신하고 snapshot/중복/직렬화 회귀를 보강한다.
9. 문서와 리스크 메모를 현재 구현 기준으로 갱신한다.

# Test Plan
- `cd apps/dondone-backend && ./gradlew test --tests '*remittance*'`
- 필요 시 narrow unit tests:
  - `./gradlew test --tests '*JobServiceTest'`
  - `./gradlew test --tests '*TransferStateMachineTest'`
- 회귀 확인 대상
  - recipient 수정 후 기존 transfer 상세/목록 snapshot 불변
  - duplicate recipient wallet address 차단
  - idempotency replay 유지
  - active transfer 직렬화 유지
  - job enqueue dedupe 유지

# Review Focus
- snapshot 컬럼이 실제 응답 생성에 반영됐는지
- recipient/user FK와 unique/index가 조회 패턴과 맞는지
- jobs reference 구분이 과도한 범위 확장을 만들지 않는지
- JPA 연관관계가 N+1 위험만 늘리고 있지 않은지
- 기존 remittance API shape가 불필요하게 깨지지 않았는지

# Worktree Split Decision
- Single lane

공유 DTO, remittance entity, jobs queue, 테스트가 동시에 움직여야 하므로 병렬 분리는 merge risk가 높다. schema-first refactor는 단일 구현 흐름으로 묶는 편이 안전하다.

# Commit Plan
1. execplan / 설계 메모 갱신
2. remittance entity/repository/service schema-first refactor
3. jobs reference structure refactor
4. tests and docs follow-up

# Open Questions
- `Job` reference 구분 명칭은 `referenceKind` 로 확정한다
- `transfers.recipient_id` 를 이번 단계에서 nullable 로 열지, 현 API 안정성을 위해 유지할지

# Assumptions
- 개발 DB reset 가능
- Flyway 도입 없이 현재는 JPA 엔티티와 테스트로 schema-first 의도를 반영한다
- remittance 외부 API 요청/응답 필드명은 가능하면 유지한다
- jobs 는 remittance용 generic queue 로 계속 운용하되 future-safe metadata는 허용한다
