# Source Inputs

## Progress Update

- completed in this turn
  - employer web auth/session moved from local role mock to backend login + invitation accept flow
  - dashboard, workers, issues, and settings now read/write employer backend APIs
  - issues screen now mixes correction requests and review-required records and can load detail
  - correction approve/reject commands are wired for pending correction requests
- still deferred
  - dedicated employer profile page route
  - `NEEDS_REVIEW` resolve command surface
  - attachment download capability beyond metadata display
  - mobile worker correction flow migration
- verification
  - `.\node_modules\.bin\tsc.cmd -b` passed in `apps/dondone-web`
  - `npm.cmd run build` still fails in sandbox because Vite/esbuild child process spawn is blocked (`spawn EPERM`)

- 사용자 요청
  - web 기능 테스트
  - app/web 연동 지점 일관성 정리
  - 추가되어야 할 내용/페이지 후보 식별
- `docs/web/README.md`
- `docs/web/implementation-slices.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/correction-request-flow.md`
- `docs/web/shared-entity-validation.md`
- `docs/execplans/active/2026-03-20-web-slice6-hardening.md`
- 현재 코드 근거
  - `apps/dondone-web/package.json`
  - `apps/dondone-web/src/app/router.tsx`
  - `apps/dondone-web/src/shared/auth/session.ts`
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/**`
  - `apps/dondone-mobile/android/**`
  - `apps/dondone-mobile/mockup/**`

# Goal

현재 web 기능을 실제 실행 가능한 수준으로 검증하고, backend/mobile과 공유되는 계약 및 정책이 어긋난 지점을 최소 변경으로 정리한 뒤, 아직 mock/local-only 상태라 이번 범위에서 닫을 수 없는 페이지·기능 후보를 분리한다.

# In Scope

- `apps/dondone-web` build 및 현재 실행 가능성 검증
- web auth/profile/dashboard/workers/issues/settings의 실제 backend 연동 상태 점검
- mobile/backend/web 공통 계약 drift 확인
  - auth/profile role/session
  - employer issues/correction queue
  - worker/workproof correction flow
  - workplace settings
- 현재 범위에서 닫을 수 있는 최소 일관성 수정
- 필요한 추가 페이지/기능 후보를 `즉시 구현`이 아니라 backlog로 정리

# Out of Scope

- mobile UI 대규모 구현 변경
- web 전체 mock 제거 및 모든 페이지 실연동
- multi-workplace switcher
- 신규 대형 feature 추가
- unrelated `apps/dondone-web/package-lock.json` 정리

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/**`

## Mobile

- `apps/dondone-mobile/android/app/src/**`
- `apps/dondone-mobile/mockup/**`

## Docs

- `docs/execplans/active/2026-03-20-web-mobile-backend-consistency-hardening.md`
- 필요 시 관련 follow-up / review note

## Shared

- `docs/web/employer-web-api-map.md`
- `docs/web/correction-request-flow.md`
- `docs/web/shared-entity-validation.md`

# Contract Changes

- 우선 현재 코드 기준 drift만 닫는다.
- web이 아직 local/session mock에 머무는 contract는 backend와 억지로 섞지 않고 문서로 분리한다.
- 실제 수정은 다음 중 최소 범위로 제한한다.
  - backend/web query/response drift
  - mixed role/session 해석 drift
  - worker correction / employer issue queue naming drift

# Security Notes

- web이 localStorage role mock을 쓰더라도 backend 권한 검증을 대체하지 못한다는 점을 유지해야 한다.
- employer route는 `/api/employer/**` scope 검증을 계속 source-of-truth로 사용한다.
- mobile/web 일관성 수정 시 worker 전용 API와 employer 전용 API 경계를 흐리지 않는다.

# Maintainability Notes

- 이번 작업은 “모든 web 페이지를 한 번에 실연동”이 아니라 현재 mock/local-only 경계를 명확히 하고 drift만 닫는 데 집중한다.
- web에서 실제 API client가 거의 없는 상태이므로, 반쪽 연결을 여러 페이지에 흩뿌리는 식의 부분 구현은 피한다.
- mobile/backend/web이 공유하는 정책은 문서와 테스트를 우선 맞추고, UI wiring은 범위가 열릴 때 별도 slice로 연다.

# Implementation Steps

1. web build를 실행해 현재 frontend 자체가 깨지지 않는지 먼저 확인한다.
2. web source에서 실제 backend 호출 유무와 mock/local state 지점을 분류한다.
3. mobile/android 및 mockup에서 backend contract와 맞물리는 flow를 확인한다.
4. app/web/backend 공통 계약 drift 중 이번 턴에 닫을 수 있는 최소 수정만 반영한다.
5. 필요한 추가 페이지/기능/연동 후보를 `now / follow-up / rescope`로 정리한다.

# Test Plan

- web
  - `cd apps/dondone-web && npm run build`
- backend
  - 변경 시 targeted employer/workproof tests
- mobile
  - 변경 범위가 생길 때만 최소 gradle task 또는 compile 검토
- 수동 확인
  - 현재 web 로그인/라우팅이 backend auth가 아니라 local session mock인지 여부
  - issues/workers/dashboard/settings가 API 연동인지 mock 데이터인지 여부

# Review Focus

- 실제로 닫은 항목과 여전히 mock/local-only인 항목이 섞여 보이지 않게 정리되었는지
- web/mobile/backend 경계가 더 흐려지지 않았는지
- backend contract 수정이 mobile worker flow를 깨지 않는지
- 추가 페이지/기능 제안이 현재 Slice 6 hardening 범위를 불필요하게 확장하지 않는지

# Worktree Split Decision

Single lane

이번 작업은 web build 검증, mobile/web/backend 계약 비교, 최소 수정, backlog 정리가 한 흐름으로 묶여 있다. 각 레이어를 병렬로 크게 수정할 단계가 아니라서 단일 레인으로 유지한다.

# Commit Plan

1. `docs: scope web-mobile-backend consistency hardening`
2. `fix: align web and backend contracts where already implemented`
3. `test: verify web build and targeted employer backend regressions`

# Open Questions

- 이번 턴에서 web mock 상태를 어느 페이지까지 실연동으로 당길지
- mobile/android compile까지 실제로 돌릴지, 아니면 contract 점검까지만 할지
- 추가 페이지/기능 후보를 바로 구현할지 다음 대화 backlog로만 정리할지

# Assumptions

- 현재 web은 mock/local-only 비중이 높아서 “기능 다 잘 되는지 테스트”는 build + 현재 연결 상태 점검이 먼저다.
- backend가 이미 준비된 employer API는 문서/테스트 기준으로 우선 정합성을 맞추고, web wiring은 범위가 허용되는 만큼만 진행한다.
