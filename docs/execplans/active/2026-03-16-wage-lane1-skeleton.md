# Source Inputs
- `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`
- `.private/kwanwoo/context/CURRENT.md`
- `.private/kwanwoo/context/NEXT.md`
- `.private/kwanwoo/context/ROADMAP.md`
- `docs/execplans/active/2026-03-13-workproof-wage-advance-backend-start.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/DonDone_P0_Functional_Spec_v0.md`
- `docs/DonDone_PRD_v1.5.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofLane1IntegrationTest.java`

# Goal
WorkProof lane 1 handoff 직후, `GET /api/wage/monthly-summary`와 `GET /api/wage/estimate`를 열 수 있는 DTO / controller / service skeleton을 먼저 고정한다. 이번 슬라이스는 WorkProof monthly summary와 current contract를 Wage 입력축으로 연결하고, verification 생성은 후속으로 미룬다.

# In Scope
- `GET /api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}` 신규 controller/service/response DTO 초안
- `GET /api/wage/estimate?month=YYYY-MM&workplaceId={id}` 신규 controller/service/response DTO 초안
- Wage service가 `WorkProofLane1Service`의 `monthly-summary`, `contracts/current`, `records` 결과를 조합하는 흐름 추가
- `month`, `workplaceId` query validation 반영
- 신규 response shape를 검증하는 최소 통합 테스트 추가 또는 기존 Wage 테스트 확장
- 관련 active execplan, private context 문서 동기화

# Out of Scope
- `POST /api/wage/verifications`
- 기존 `POST /api/wage/deposits`, `GET /api/wage/summary` 제거 또는 대체
- WorkProof lane 2 (`attachments`, `missing`, `modifications`)
- DB schema 변경
- SecurityConfig 변경
- Mobile UI 변경

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java` 또는 기존 lane 1 DTO 재사용 지점
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-16-wage-lane1-skeleton.md`
- 필요 시 `.private/kwanwoo/context/CURRENT.md`
- 필요 시 `.private/kwanwoo/context/NEXT.md`

## Shared
- 기존 `ApiResponse<T>` envelope 유지
- 기존 JWT 보호 규칙 유지

# Contract Changes
- 신규 보호 endpoint 2개를 추가한다.
  - `GET /api/wage/monthly-summary`
  - `GET /api/wage/estimate`
- query contract는 `month`(`YYYY-MM`)와 `workplaceId`(positive)로 고정한다.
- `GET /api/wage/monthly-summary` response는 아래 필드를 가진다.
  - `month`, `workplaceId`, `contractId`, `payUnit`, `normalizedHourlyWage`
  - `workDayCount`, `verifiedWorkMinutes`, `overtimeMinutes`, `nightMinutes`, `modifiedRecordCount`
  - `includedRecordIds`, `excludedPendingRecordCount`
- `GET /api/wage/estimate` response는 아래 필드를 가진다.
  - `month`, `workplaceId`
  - `contract`
  - `summary`
  - `estimate`
  - `disclaimer`, `ruleVersion`
- 기존 `POST /api/wage/deposits`, `GET /api/wage/summary` contract는 이번 슬라이스에서 유지한다.

# Security Notes
- 신규 Wage endpoint는 모두 JWT 보호 대상으로 둔다.
- WorkProof 소유권 검증은 `WorkProofLane1Service`의 기존 `404` 은닉 규칙을 그대로 따른다.
- 토큰 처리, 공개 경로, 필터 exclusion list는 변경하지 않는다.
- 이번 슬라이스는 읽기 API만 추가하므로 `Idempotency-Key`는 도입하지 않는다.

# Maintainability Notes
- Wage 신규 계약은 기존 `GET /api/wage/summary`를 바로 치환하지 말고 병행 유지한다. verification 전환 전까지는 호환성 위험을 줄이는 편이 낫다.
- controller는 query validation과 endpoint routing만 맡고, WorkProof contract 조합과 계산 근거 매핑은 `WageService`와 calculator 계층에 둔다.
- WorkProof lane 1 response DTO를 Wage 전용 DTO로 그대로 복제하지 말고, Wage 응답 shape에서 필요한 최소 필드만 조합한다.
- 현재 branch는 `feature/workproof-lane1-dto-validation`이지만 작업 범위는 Wage lane 1 skeleton 단일 lane으로 본다. shared DTO와 응답 계약이 움직이는 동안 병렬 분리는 금지한다.

# Implementation Steps
1. Wage API 계약과 현재 `WageController` / `WageService` 차이를 정리하고 신규 response DTO 이름을 고정한다.
2. active execplan을 추가하고 이번 슬라이스의 가정과 호환성 원칙을 문서화한다.
3. `WageController`에 `monthly-summary`, `estimate` GET endpoint를 추가하고 query validation을 반영한다.
4. `WageService`에 WorkProof lane 1 입력을 조합하는 read methods를 추가한다.
5. `WageSummaryCalculator` 또는 보조 calculator에서 monthly summary 기반 estimate snapshot 계산을 분리한다.
6. Wage 통합 테스트에 신규 endpoint happy path와 기본 validation 케이스를 추가한다.
7. `.private/kwanwoo/context/CURRENT.md`, `.private/kwanwoo/context/NEXT.md`를 Wage skeleton 진행 상태로 갱신한다.

# Test Plan
- `./gradlew.bat test --tests com.workproofpay.backend.wage.WageDemoIntegrationTest`
- 필요 시 `./gradlew.bat test --tests com.workproofpay.backend.workproof.WorkProofLane1DraftValidatorTest`
- 가능하면 `./gradlew.bat integrationTest --tests "com.workproofpay.backend.workproof.WorkProofLane1IntegrationTest"` 재실행 여부를 판단하되, 이번 슬라이스가 WorkProof behavior를 직접 바꾸지 않으면 생략 가능
- 실행 불가 시 Java/Testcontainers/Docker blocker를 명시한다

# Review Focus
- Wage 신규 endpoint가 PRD/API contract의 `month + workplaceId` 구조를 따르는지
- `WorkProofLane1Service` 조합이 소유권/404 은닉 규칙을 우회하지 않는지
- 기존 `GET /api/wage/summary`와 공존하면서 응답 의미가 충돌하지 않는지
- response 필드가 verification 후속 구현에 재작업을 크게 늘리지 않는지
- estimate 계산이 `reference-only`와 `evidence-first` 포지셔닝을 유지하는지

# Worktree Split Decision
- Single lane

WorkProof monthly summary, current contract, Wage response DTO가 같은 계약 축을 공유한다. shared response contract가 아직 고정 단계이므로 병렬 lane으로 나누면 merge risk와 계약 drift 가능성이 높다.

# Commit Plan
- `docs: add wage lane1 skeleton execplan`
- `feat: add wage monthly summary and estimate skeleton`
- `test: cover wage lane1 summary and estimate endpoints`

# Open Questions
- `includedRecordIds`를 WorkProof monthly summary 자체에 추가할지, Wage service가 records list에서 보강할지
- `excludedPendingRecordCount`는 `PENDING`만 셀지, `NEEDS_REVIEW`와 `EXCLUDED`까지 함께 셀지
- estimate 계산을 기존 `WageSummaryCalculator`에 확장할지, lane 1용 snapshot 메서드를 분리할지
- 기존 `GET /api/wage/summary`를 언제 deprecated 처리할지

# Assumptions
- 이번 슬라이스는 branch 전환 없이 현재 workspace에서 구현한다.
- WorkProof lane 1 monthly summary와 current contract는 Wage skeleton의 신뢰 가능한 upstream으로 간주한다.
- verification 생성 전까지는 실제 입금액 비교 로직보다 read-only summary/estimate 축을 먼저 고정하는 편이 리스크가 낮다.
- 신규 endpoint는 demo/testnet 정책과 `reference-only` 디스클레이머를 그대로 따른다.
