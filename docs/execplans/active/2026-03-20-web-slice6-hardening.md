# Source Inputs

## 2026-03-20 Addendum

- Slice 6 web hardening now also fixes shared wording between worker app and employer web.
- Reference policy doc: `docs/web/app-web-shared-policy.md`
- In-scope hardening additions:
  - issues queue pagination UI aligned with workers page
  - explicit separation between `정정 요청` and `검토 필요 기록`
  - attachment wording fixed as evidence metadata, not default attendance input
- Still deferred from this slice:
  - worker-side `기록 검토 요청` command
  - employer-side `검토 확인 / 검토 반려` command
  - attachment preview/download surface
  - `late / leave / absent`

- `docs/web/README.md`
- `docs/web/implementation-slices.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/correction-request-flow.md`
- `docs/web/shared-entity-validation.md`
- `docs/execplans/active/2026-03-19-web-correction-request-flow.md`
- `docs/reviews/active/2026-03-19-web-auth-profile-followups.md`
- `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`
- `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- `docs/reviews/active/2026-03-19-web-correction-request-followups.md`
- 현재 backend 구현/테스트 근거
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/service/EmployerAuthService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerWorkerReadModelService.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/EmployerAuthIntegrationTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerIssueReadModelIntegrationTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerCorrectionRequestIntegrationTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerWorkerReadModelIntegrationTest.java`

# Goal

Slice 2~5 employer backend 범위를 Slice 6 hardening 기준으로 재검토하고, 실제 contract drift/누락 테스트/정렬·필터링 correctness 문제만 최소 수정으로 닫은 뒤 각 follow-up 항목을 `fixed / accepted risk / rescope`로 정리한다.

# In Scope

- employer auth/profile follow-up 재검토
  - email canonical uniqueness 현재 보장 수준 확인
  - `IgnoreCase` 조회 유지 이유와 정리 시점 판단
- employer worker/dashboard follow-up 재검토
  - worker profile canonical source 부재 상태 재확인
  - `late/leave/absent` 미노출 정책 유지 여부 판단
  - Querydsl/read-model 분리 시점 판단
- employer correction/issues follow-up hardening
  - `GET /api/employer/issues` contract drift 확인 및 필요 시 수정
  - correction queue / issue queue 정렬·필터링·페이징 correctness 보강
  - attachment metadata contract, `NEEDS_REVIEW` resolve command, web issues wiring 분류
- targeted backend 테스트 추가/보강
- Slice 6 execplan/review 문서 갱신

# Out of Scope

- mobile 변경
- web 화면 구현 및 mock 제거
- multi-workplace switcher 구현
- 새 대규모 feature 추가
- `PATCH /api/workproof/{id}` 제거
- attachment download URL/storage 계약 추가 구현
- `NEEDS_REVIEW` resolve command 신규 구현
- worker profile canonical source를 위한 새 shared entity 도입

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/dto/request/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/service/EmployerAuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/repo/UserRepository.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerIssueReadModelIntegrationTest.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerCorrectionRequestIntegrationTest.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/EmployerAuthIntegrationTest.java`

## Mobile

- 없음

## Docs

- `docs/execplans/active/2026-03-20-web-slice6-hardening.md`
- `docs/web/implementation-slices.md`
- `docs/reviews/active/2026-03-19-web-auth-profile-followups.md`
- `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- `docs/reviews/active/2026-03-19-web-correction-request-followups.md`
- 필요 시 `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`

## Shared

- `docs/web/shared-entity-validation.md`

# Contract Changes

- `GET /api/employer/issues`가 문서 기준 `statuses` query parameter를 받아야 하는지 현재 코드와 문서를 다시 맞춘다.
- 필요 시 issue queue filtering contract를 `itemTypes + query + statuses + page + size`로 코드/테스트에 반영한다.
- correction request list의 정렬 순서를 문서 의도대로 최신순으로 고정하고 테스트로 닫는다.
- attachment/detail contract는 새 surface를 추가하지 않고 현재 안전 메타데이터 노출 범위를 유지한다.

# Security Notes

- `/api/employer/**`는 계속 `EMPLOYER` role + scoped entity 재검증을 유지한다.
- bare ID(`requestId`, `workProofId`, `workerId`)를 받는 read-model/command는 계속 company/workplace scope로 재검증한다.
- email uniqueness 보강 검토는 auth 동작을 넓히지 않고 canonicalization 회귀 위험만 줄이는 범위로 제한한다.
- `NEEDS_REVIEW` resolve command는 정책 미정 상태이므로 read-only surface 유지가 기본값이다.

# Maintainability Notes

- read-model hardening은 기존 service 조합 구조를 크게 뒤집지 않는다. Querydsl 전환은 실제 필요성이 확인될 때만 별도 slice로 분리한다.
- auth/profile hardening은 현재 `EmailNormalizer + normalized save + IgnoreCase query` 구조를 불필요하게 반쪽 리팩터링하지 않는다.
- follow-up 정리는 문서와 테스트를 함께 닫는다. 정책 미정 항목을 코드에 억지로 박지 않는다.
- correction/issues 쪽은 web wiring 미구현 상태를 backend contract 확장으로 우회하지 않는다.

# Implementation Steps

1. Slice 6 대상 follow-up를 코드 기준으로 `즉시 수정`, `accepted risk`, `rescope` 후보로 재분류한다.
2. `GET /api/employer/issues` 문서-코드 drift가 있으면 query DTO/service/test를 최소 수정으로 맞춘다.
3. correction request list 정렬/paging correctness를 점검하고 필요 시 수정 및 회귀 테스트를 추가한다.
4. auth/profile 쪽은 DB uniqueness 보강 여부를 판단하고, 이번 slice에서 코드 변경이 없다면 근거를 테스트/문서로 남긴다.
5. worker read-model의 canonical source 미정 항목은 구현 대신 유지 조건과 rescope 조건을 명확히 기록한다.
6. targeted backend tests를 실행해 회귀를 확인한다.
7. follow-up 문서를 `fixed / accepted risk / rescope` 상태로 갱신한다.

# Test Plan

- 우선 targeted integration tests
  - `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.EmployerIssueReadModelIntegrationTest --tests com.workproofpay.backend.employer.EmployerCorrectionRequestIntegrationTest --tests com.workproofpay.backend.employerauth.EmployerAuthIntegrationTest`
- 필요 시 employer 묶음 회귀
  - `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.*`
- 확인 항목
  - issue queue `itemTypes/query/statuses/page/size` contract
  - correction request list 최신순 정렬과 pagination
  - approve/reject 이후 request/workproof/audit 반영 유지
  - mixed-case employer email login/duplicate 방어 등 canonicalization 회귀 여부

# Review Focus

- issue queue hardening이 문서 계약과 실제 API를 다시 맞췄는지
- correction request list/query change가 scope/security를 깨지 않는지
- auth/profile follow-up 분류가 실제 코드 근거와 테스트 상태를 반영하는지
- worker read-model follow-up를 성급한 shared schema 변경 없이 적절히 accepted risk 또는 rescope로 닫았는지
- 새 수정이 mobile/web 미구현 범위를 backend에서 억지로 끌어안지 않았는지

# Worktree Split Decision

Single lane

이번 작업은 shared contract 판단, 테스트 보강, review follow-up 분류, backend read-model 수정이 서로 강하게 얽혀 있다. DTO/API/auth/test/docs가 동시에 움직일 수 있어 병렬 레인으로 나누면 중복 판정과 merge risk가 크다.

# Commit Plan

1. `docs: add slice6 employer backend hardening plan`
2. `fix: harden employer issue and correction read models`
3. `test: cover employer hardening regressions`

# Open Questions

- `GET /api/employer/issues`의 `statuses`는 문서대로 backend에 추가할지, 아니면 문서를 현재 구현에 맞춰 줄일지
- auth email uniqueness는 현재 normalized save에 의존한 exact unique + IgnoreCase query로 충분한지, 별도 DB migration이 필요한지
- workplace settings follow-up 중 timing/order 경합은 이번 slice에서 테스트로 닫을 수 있는지, 아니면 accepted risk로 남길지

# Assumptions

- Slice 6의 우선 목표는 새 기능 추가가 아니라 기존 employer backend contract와 테스트를 안정화하는 것이다.
- worker profile canonical source, `late/leave/absent`, `NEEDS_REVIEW` resolve command는 정책 미정이면 구현하지 않고 명시적으로 분류하는 편이 안전하다.
- `apps/dondone-web/package-lock.json` 변경은 계속 unrelated로 취급한다.
