# Source Inputs
- 사용자 요청:
  - Slice 3 `Workplace settings` backend foundation 진행
  - `EmployerAccessScope`와 `EmploymentMembership` 재사용 기준 고정
  - read/write DTO, API, service, validation, test 뼈대 구현
  - 설정 변경 효력 시점과 과거 `WorkProof` 영향 규칙을 문서/코드 기준으로 정리
- 기준 문서:
  - `docs/web/README.md`
  - `docs/web/implementation-slices.md`
  - `docs/web/workplace-settings-contract.md`
  - `docs/web/shared-entity-validation.md`
  - `docs/web/employer-worker-domain-map.md`
  - `docs/web/employer-web-api-map.md`
  - `docs/web/auth-and-role-policy.md`
- 현재 코드 근거:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerAccessScope.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerAccessScopeService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/EmploymentMembership.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerAccessScopeServiceTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/EmployerAuthIntegrationTest.java`

# Goal
Slice 2에서 만든 employer auth/profile foundation 위에 `GET/PUT /api/employer/workplace-settings` 최소 backend 계약을 추가하고, settings 변경이 `WorkProof` 판정에 미치는 범위를 MVP 기준으로 고정한다.

# Status
- 상태: `done`
- 완료일: `2026-03-19`
- 검증: `apps/dondone-backend`에서 `.\gradlew.bat test` 통과

# In Scope
- backend employer settings endpoint 추가
  - `GET /api/employer/workplace-settings`
  - `PUT /api/employer/workplace-settings`
- `EmployerAccessScope` 기반으로 default workplace 1건을 해석하는 settings authz service 추가
- `EmploymentMembership`를 scope source of truth로 재사용하는 검증 경계 고정
  - employer scope 자체는 `EmployerProfile.defaultWorkplaceId`
  - downstream worker/read-model 영향 범위 설명은 `EmploymentMembership.companyId/workplaceId`
- settings read/write DTO 추가
- request validation 및 domain validation 추가
- settings 변경 효력 시점과 과거 `WorkProof` 비소급 규칙을 문서/코드에 반영
- backend 테스트 추가

# Out of Scope
- 기존 `/api/workproof/*`, `/api/auth/*`, `/api/wage/*` 변경
- dashboard/workers/issues/settings read-model 본구현
- correction request 본구현
- mobile 변경
- workplace switcher, multi-company, multi-workplace 선택 UI
- 과거 `WorkProof` 재판정 배치/운영 도구 구현

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkplaceRepository.java`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`

## Mobile
- 없음

## Docs
- `docs/web/implementation-slices.md`
- `docs/web/workplace-settings-contract.md`
- `docs/web/shared-entity-validation.md`
- 본 실행계획 문서

## Shared
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/**`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/**`

# Contract Changes
- 신규 read DTO
  - `workplaceId`
  - `workplaceName`
  - `companyId`
  - `companyName`
  - `address`
  - `detailAddress`
  - `latitude`
  - `longitude`
  - `allowedRadiusMeters`
  - `effectiveFrom`
  - `updatedAt`
  - `updatedByAccountId`
- 신규 write DTO
  - `address`
  - `detailAddress`
  - `latitude`
  - `longitude`
  - `allowedRadiusMeters`
- request에는 `companyId`, `workplaceId`, `effectiveFrom`를 받지 않는다.
- `detailAddress`는 현재 코드의 `Workplace.mapLabel`을 employer settings 용어로 노출한다.
- `effectiveFrom`는 MVP에서 `PUT` 처리 시점의 server time으로 고정한다.

# Security Notes
- `/api/employer/**` role 제한은 기존 `SecurityConfig`를 그대로 사용한다.
- settings 대상 workplace는 request 값이 아니라 `EmployerAccessScope.defaultWorkplaceId`로만 해석한다.
- `EmploymentMembership`는 employer가 영향 줄 수 있는 worker scope의 source of truth로 유지한다.
- 레거시 `Workplace.user`, `WorkProof.user`는 settings authz source로 사용하지 않는다.
- cross-company, cross-workplace 위조는 `Workplace.companyId` binding으로 막는다.

# Maintainability Notes
- worker `WorkplaceResponse`를 재사용하지 말고 employer 전용 DTO를 분리한다.
- settings 변경 로직은 controller에 두지 않고 service/aggregate 메서드 한 곳에 모은다.
- `detailAddress`는 기존 `mapLabel` 저장소를 재사용해 schema churn을 줄인다.
- 과거 `WorkProof` 재판정 로직은 이번 slice에 섞지 않는다. 비소급 정책만 문서와 코드 주석으로 고정한다.

# Implementation Steps
1. Slice 3 active execplan을 추가하고 계약/가정/리스크를 문서화한다.
2. `Workplace`에 employer settings 갱신 메서드와 최소 metadata 필드를 추가한다.
3. employer settings read/write DTO와 controller/service를 추가한다.
4. `EmployerAccessScopeService`에 settings 대상 workplace 해석/검증 helper를 추가한다.
5. `EmploymentMembershipRepository`에 scope 영향 확인용 최소 query를 추가한다.
6. request validation, 반경/좌표/domain validation, company-workplace binding validation을 연결한다.
7. integration/unit 테스트로 조회/수정 성공, validation 실패, role/authz 경계, membership/legacy-owner 경계 회귀를 닫는다.
8. Slice 3 관련 문서 상태를 `in_progress`로 갱신하고 효력 규칙을 코드 기준으로 정리한다.

# Outcome Summary
- `GET /api/employer/workplace-settings`, `PUT /api/employer/workplace-settings`를 backend에 추가했다.
- `EmployerAccessScope.defaultWorkplaceId` 기준 대상 해석과 `EmploymentMembership` 기반 active membership 집계를 response에 반영했다.
- `Workplace`에 `settingsEffectiveFrom`, `settingsUpdatedByAccountId` additive metadata를 추가했다.
- `detailAddress`는 현재 `Workplace.mapLabel` 저장소를 재사용했다.
- 리뷰에서 발견된 과거 `WorkProof`/PDF snapshot drift를 막기 위해 `WorkProof`에 workplace snapshot을 고정 저장하도록 보강했다.
- settings 변경은 미래 `check-in/check-out`부터만 적용하고 기존 완료 `WorkProof`는 자동 재판정하지 않는 정책을 문서/코드에 맞췄다.

# Test Plan
- `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.EmployerWorkplaceSettings* --tests com.workproofpay.backend.employer.EmployerAccessScopeServiceTest --tests com.workproofpay.backend.employerauth.EmployerAuthIntegrationTest`
- 필요 시 전체 회귀:
  - `cd apps/dondone-backend && .\\gradlew.bat test`
- 검증 항목:
  - employer가 default workplace settings 조회 성공
  - employer가 settings 수정 성공
  - 잘못된 반경/좌표/detailAddress validation 실패
  - worker token으로 employer settings 접근 차단
  - company-workplace binding mismatch 차단
  - settings 변경 후 `effectiveFrom`가 server time으로 갱신
  - 과거 `WorkProof`를 자동 재판정하지 않는 정책이 문서/코드에 남아 있는지 확인

# Review Focus
- employer settings DTO가 worker DTO와 섞이지 않는지
- `detailAddress -> mapLabel` 매핑이 기존 worker lane 1 흐름을 깨지 않는지
- `effectiveFrom`와 `updatedAt` 의미가 혼동되지 않는지
- settings authz가 `EmployerAccessScope + EmploymentMembership` 경계를 벗어나지 않는지
- validation 메시지와 상태코드가 일관적인지

# Worktree Split Decision
Single lane

이번 작업은 `Workplace` 공유 엔티티, employer DTO/API, authz helper, validation, 테스트, 문서 계약이 함께 움직인다. shared entity와 authz source of truth가 동시에 바뀌므로 병렬 레인으로 나누면 merge risk가 높다.

# Commit Plan
1. `docs: scope slice3 workplace settings foundation`
2. `feat: add employer workplace settings foundation`
3. `test: cover employer workplace settings authz and validation`

# Open Questions
- `updatedBy`를 display name까지 저장할지 account ID만 저장할지
- 추후 다중 workplace를 열 때 `defaultWorkplaceId` 외 선택 workplace 계약을 어떤 DTO로 확장할지

# Remaining Follow-ups
- `detailAddress -> Workplace.mapLabel` 재사용은 schema churn을 줄였지만 의미 분리가 완전히 끝난 것은 아니다. shared workplace contract를 다시 건드릴 때 독립 필드 승격 여부를 재평가한다.
- setting 저장 시점과 check-in/check-out 동시성 edge case는 foundation 범위에서는 정책만 고정했고, stress/ordering 테스트는 Hardening에서 다시 확인한다.
- multi-workplace switcher, 관리자 재계산 도구, 예약 효력 시점은 이번 slice 범위 밖이다.
- 이 항목들은 `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`에 Hardening backlog로도 남긴다.

# Assumptions
- Slice 3 MVP는 employer 1명당 default workplace 1건만 직접 수정한다.
- `detailAddress`는 별도 판정 입력이 아니라 표시/설명용 필드다.
- settings 변경은 저장 완료 이후 새 `check-in/check-out`부터만 적용되고, 기존 완료 `WorkProof`는 자동 재판정하지 않는다.
- settings 변경 영향 범위는 향후 `EmploymentMembership.companyId/workplaceId` 기준 read-model과 correction flow가 해석한다.
