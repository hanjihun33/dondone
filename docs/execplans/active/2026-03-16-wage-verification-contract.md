# Source Inputs
- Root guidance:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
- Private context:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/context/ROADMAP.md`
  - `.private/kwanwoo/decisions/DECISIONS.md`
  - `.private/kwanwoo/features/wage.md`
- Product docs:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
  - `docs/DonDone_P0_Functional_Spec_v0.md`
- Existing plans:
  - `docs/execplans/active/2026-03-13-workproof-wage-advance-backend-start.md`
  - `docs/execplans/active/2026-03-16-wage-lane1-skeleton.md`
- Current backend code:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageDeposit.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

# Goal
`feature/wage-verification-contract` 브랜치에서 Wage Shield의 핵심 액션인 `POST /api/wage/verifications`와 `GET /api/wage/verifications/{verificationId}` 계약을 먼저 고정한다. 이번 슬라이스는 "실제 받은 돈 확인 -> 참고용 예상 금액 비교 -> 확인 필요 상태와 근거 snapshot 저장"까지를 구현 가능한 수준으로 정리하고, 기존 `deposits/summary` 흐름과의 호환 전략도 함께 정한다.

# In Scope
- `POST /api/wage/verifications` request/response/validation/error code 확정
- `GET /api/wage/verifications/{verificationId}` response/ownership rule 확정
- verification 생성 시 저장할 snapshot 범위 결정:
  - actual deposit input
  - estimate snapshot
  - difference snapshot
  - threshold snapshot
  - evidence snapshot
  - possible cause snapshot
- verification 생성 시 사용하는 upstream 입력축 확정:
  - WorkProof `monthly-summary`
  - WorkProof `records`
  - WorkProof `contracts/current`
- 기존 `POST /api/wage/deposits`, `GET /api/wage/summary`와의 transition 전략 확정
- 최소 backend 구현 범위 정의:
  - controller
  - service
  - model
  - repo
  - DTO
  - integration test

# Out of Scope
- 회사 확인 요청, 회사 답변, 해결 완료 같은 employer confirmation workflow 구현
- Documents / Claim API 구현 또는 문서 생성 job 연결
- 급여명세서/계약서 파싱
- 기존 `GET /api/wage/summary` 제거
- WorkProof lane 2 (`attachments`, `missing`, `modifications`) 구현 확장
- ERD 확정본 / migration SQL 도입
- mobile UI 변경

# Affected Modules
## Backend
- Existing:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageDeposit.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- New candidates:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/request/CreateWageVerificationRequest.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageVerificationCreatedResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageVerificationDetailResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageVerification.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageVerificationStatus.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageVerificationResolutionStage.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/repo/WageVerificationRepository.java`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-16-wage-verification-contract.md`
- 필요 시:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/logs/2026-03-16.md`

## Shared
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- 공통 `ApiResponse<T>` envelope 유지

# Contract Changes
- 신규 보호 endpoint 2개를 추가한다.
  - `POST /api/wage/verifications`
  - `GET /api/wage/verifications/{verificationId}`
- `POST /api/wage/verifications` request는 아래 필드로 고정한다.
  - `month`
  - `workplaceId`
  - `actualDepositAmount`
  - `deductionsKnown`
  - `memo` (선택)
- request validation 원칙:
  - `month`: `YYYY-MM`
  - `workplaceId`: 양수
  - `actualDepositAmount`: 필수, `0` 이상 허용
  - `memo`: 선택, 짧은 worker note 용도
- `POST /api/wage/verifications` response는 공유 API 문서 기준 필드를 유지한다.
  - `verificationId`
  - `status`
  - `resolutionStage`
  - `estimatedTotal`
  - `actualDepositAmount`
  - `differenceAmount`
  - `differenceRate`
  - `threshold`
  - `possibleCauses[]`
  - `evidence`
  - `nextActions[]`
- `GET /api/wage/verifications/{verificationId}` response는 공유 API 문서 기준 필드를 유지한다.
  - `verificationId`
  - `month`
  - `workplaceId`
  - `status`
  - `resolutionStage`
  - `estimated`
  - `actual`
  - `difference`
  - `threshold`
  - `possibleCauses[]`
  - `evidence`
  - `employerSupport`
  - `relatedActions`
- transition 전략:
  - 외부 contract 기준으로는 기존 `POST /api/wage/deposits`, `GET /api/wage/summary`를 유지한다.
  - 이번 슬라이스에서는 verification 생성 결과를 기존 `WageDeposit`에 자동 backfill하지 않는다.
  - 이유: legacy deposit 흐름은 `depositDate`를 의미 있게 사용하고, verification request에는 그 필드가 없다.
- upstream ownership 전략:
  - 이번 슬라이스에서는 WorkProof monthly summary DTO를 넓히지 않는다.
  - `includedRecordIds`, `excludedPendingRecordCount`, evidence용 `recordIds`는 `WageService`가 `WorkProofLane1Service.getRecords(...)` 결과를 조합해 만든다.
  - 대신 verification 엔티티에 evidence snapshot을 저장해 downstream drift를 막는다.

# Security Notes
- 신규 endpoint는 모두 JWT 보호 대상으로 둔다.
- WorkProof 쪽 소유권 검증은 기존 `WorkProofLane1Service` 경로를 재사용해 `404` 은닉 규칙을 유지한다.
- `GET /api/wage/verifications/{verificationId}`는 본인 소유 verification만 조회 가능해야 하며, 타인 리소스는 `404`로 숨긴다.
- 이번 슬라이스는 worker self-check 단계만 다루므로 employer participation은 응답 상태값으로만 표현하고 별도 공개 경로를 열지 않는다.
- `Idempotency-Key`는 이번 endpoint에 도입하지 않는다.
  - 이유: verification은 worker가 의도적으로 다시 확인해 새 snapshot을 남길 수 있는 append-only 성격이 더 가깝다.

# Maintainability Notes
- verification은 `WageDeposit`의 단순 확장 필드로 억지로 넣지 말고 별도 aggregate로 둔다.
  - `WageDeposit`는 기존 `summary` 흐름 전용 입력 기록으로 남기고,
  - `WageVerification`은 Documents/Claim로 이어질 snapshot anchor로 취급한다.
- verification 생성 시 `WageDeposit`까지 자동 저장하면 `depositDate` 의미가 흐려진다. 이 브랜치에서는 두 흐름을 느슨하게 공존시키고, legacy 제거 시점에만 통합 여부를 다시 판단한다.
- `WageSummaryCalculator`의 기존 estimate/threshold/reason 규칙을 재사용하되, verification 응답용 mapper를 controller에 두지 말고 service 또는 calculator 보조 메서드로 모은다.
- WorkProof contract를 지금 다시 넓히면 shared DTO drift가 커진다. 이번 브랜치에서는 Wage layer composition을 유지하고, evidence snapshot 저장으로 downstream 일관성을 확보한다.
- create response와 detail response는 shape가 비슷해 보여도 무리하게 하나의 DTO로 합치지 않는다.
  - create는 "행동 결과 요약"
  - detail은 "문서/Claim 입력용 상세 snapshot"
- verification은 append-only를 기본으로 두고, 수정/삭제/재확인 최신본 조회는 후속 요구가 생길 때 별도 endpoint로 다룬다.

# Implementation Steps
1. `POST /api/wage/verifications`, `GET /api/wage/verifications/{verificationId}` DTO 이름과 response shape를 공유 API 문서 기준으로 고정한다.
2. `WageVerification` aggregate와 repository를 추가한다.
3. verification 생성 시 필요한 내부 snapshot 구조를 정한다.
   - estimate
   - actual
   - difference
   - threshold
   - evidence
   - possible causes
4. `WageService`에 verification create/detail 메서드를 추가한다.
5. verification create는 아래 순서로 동작하게 한다.
   - month/workplace validation
   - WorkProof summary/current contract/records 조회
   - estimate 계산
   - threshold/difference/status/resolutionStage 계산
   - evidence/possibleCauses snapshot 생성
   - `WageVerification` 저장
6. `WageController`에 create/detail endpoint를 추가한다.
7. `ErrorCode`에 verification 전용 코드를 추가한다.
   - 예: `ACTUAL_DEPOSIT_REQUIRED`
   - 필요 시 `WAGE_VERIFICATION_NOT_FOUND`
8. `WageDemoIntegrationTest` 또는 분리된 verification integration test에서 아래를 검증한다.
   - happy path
   - validation failure
   - 타인 verification 은닉
   - legacy summary 호환
9. 구현 후 `.private/kwanwoo/context/CURRENT.md`, `NEXT.md`, 로그를 verification 기준으로 동기화한다.

# Test Plan
- `./gradlew.bat test --tests com.workproofpay.backend.wage.WageDemoIntegrationTest`
- 필요 시 신규 테스트 분리:
  - `./gradlew.bat test --tests com.workproofpay.backend.wage.WageVerificationIntegrationTest`
- validation이 DTO 중심으로 분리되면 좁은 단위 테스트를 추가 검토한다.
- 실행 환경 blocker가 있으면 Java / Docker / Testcontainers 여부를 구분해 명시한다.

# Review Focus
- verification create/detail contract가 `docs/DonDone_P0_API_Contract_v0.md`와 drift 없이 맞는지
- verification이 `최종 급여 확정`처럼 보이지 않고 `확인 필요 상태 + 근거` 포지셔닝을 유지하는지
- `WageVerification` snapshot이 Documents/Claim 입력으로 재사용 가능한 수준으로 저장되는지
- legacy `deposits/summary` 흐름과 새 verification 흐름이 같은 worker input에서 충돌하지 않는지
- `WorkProofLane1Service`를 통해 소유권/404 은닉 규칙을 우회하지 않는지
- `includedRecordIds`/evidence source 책임이 이번 브랜치에서 불필요하게 WorkProof DTO까지 흔들지 않는지

# Worktree Split Decision
- Single lane

Wage verification은 shared response contract, WorkProof upstream composition, legacy `deposits/summary` 호환, downstream Documents/Claim 입력축이 함께 움직인다. DTO와 응답 의미가 아직 고정 단계이므로 병렬 lane으로 나누면 merge risk가 높고, contract drift가 생기기 쉽다.

# Commit Plan
- `docs: add wage verification contract execplan`
- `feat: add wage verification create and detail endpoints`
- `test: cover wage verification contract and access rules`

# Open Questions
- worker가 같은 `month + workplaceId`로 여러 번 verification을 생성할 때 최신본 조회/list endpoint가 필요한지
- `relatedActions`를 "도메인상 다음 액션 가능"으로 볼지, "실제 downstream 구현 완료 여부"로 볼지
- employer link가 아직 없을 때 `employerSupport.available` 기본값을 `false`로 고정할지, 더미 추천 상태를 둘지
- verification snapshot에 contract 세부값을 detail response에도 바로 노출할지, 현재 문서처럼 estimate/evidence 중심으로 유지할지

# Assumptions
- 이번 브랜치에서는 verification을 append-only로 구현한다. update/delete는 다루지 않는다.
- `actualDepositAmount`는 미지급/전액 누락 케이스를 표현할 수 있게 `0`도 허용한다.
- verification 생성 시점의 estimate/difference/threshold/evidence는 snapshot으로 저장해, 이후 WorkProof 수정이나 계약 변화가 생겨도 Documents/Claim 입력이 흔들리지 않게 한다.
- `possibleCauses[]`는 기존 `WageSummaryCalculator`의 reason 규칙을 재사용해 시작한다.
- `resolutionStage`는 P0 첫 슬라이스에서 `MATCHED -> SELF_CHECK`, `CHECK_REQUIRED -> EMPLOYER_CONFIRMATION_RECOMMENDED` 정도로 먼저 고정한다.
- `relatedActions`는 실제 문서 생성 완료 여부가 아니라, verification 결과를 바탕으로 다음 단계로 진행 가능한지 보여주는 readiness 신호로 해석한다.
