# Source Inputs
- 사용자 요청:
  - `dondone-web` 화면 기준으로 필요한 백엔드 기능 정리
  - 웹 전용 API 분리 방향 검토
  - `docs/web/` 기준 문서 공간 신설 검토
- 리포지토리 규칙:
  - `AGENTS.md`
  - `docs/CODEX_WORKFLOW.md`
  - `docs/execplans/README.md`
  - `docs/reviews/README.md`
- 기존 웹 관련 계획/구현 근거:
  - `docs/execplans/active/2026-03-17-dondone-web-bootstrap.md`
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`
- 기존 백엔드 구조 근거:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`

# Goal
`docs/web/` 아래에 웹 전용 기준 문서 세트를 만들고, 이후 웹 전용 API와 고용주-근로자 연동 검증을 같은 방향으로 진행할 수 있는 공용 문서 구조를 고정한다.

# In Scope
- `docs/web/` 디렉터리 신설
- 웹 방향성, API 맵, 공유 도메인, 엔티티 검증, 정정 요청 흐름, 인증 정책, 구현 순서 문서 초안 작성
- `docs/web/README.md`에 권장 확인 순서와 문서 역할 정리
- 이 문서 세트와 `docs/execplans`, `docs/reviews`의 역할 분리 명시

# Out of Scope
- 실제 웹/백엔드 코드 구현
- DTO/DB 스키마 확정
- 기존 앱 API 계약 변경
- 리뷰 노트 작성

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/web/README.md`
- `docs/web/employer-web-direction.md`
- `docs/web/employer-web-api-map.md`
- `docs/web/employer-worker-domain-map.md`
- `docs/web/shared-entity-validation.md`
- `docs/web/correction-request-flow.md`
- `docs/web/workplace-settings-contract.md`
- `docs/web/auth-and-role-policy.md`
- `docs/web/implementation-slices.md`
- `docs/execplans/active/2026-03-19-web-employer-doc-foundation.md`

## Shared
- 없음

# Contract Changes
- 현재 단계에서 API/DTO/DB 계약 확정은 하지 않는다.
- 문서상으로만 웹 전용 API namespace, 공유 도메인 검증 포인트, 권한 분리 원칙을 고정한다.

# Security Notes
- 웹/앱 계정 경계를 혼동하지 않도록 문서에서 `공통 인증 인프라`와 `역할별 도메인/API`를 분리해 서술한다.
- 사업주가 조회 가능한 근로자 범위, 사업장 범위, 정정 요청 처리 권한을 별도 문서에서 검증 대상으로 고정한다.

# Maintainability Notes
- `docs/web/`는 장기 기준 문서만 두고, 실행 계획과 리뷰 이력은 계속 `docs/execplans`와 `docs/reviews`에 둔다.
- 같은 내용을 `docs/web/`와 실행계획 문서에 중복 서술하지 않는다.
- 문서 간 참조 순서를 명확히 두어, 구현 중 문서 찾기 비용을 줄인다.

# Implementation Steps
1. `docs/web/` 문서 세트의 최소 구성과 역할을 확정한다.
2. `README`에 우선 확인 순서와 문서 간 관계를 정의한다.
3. 방향성, API, 도메인, 검증, 흐름, 인증, 구현 순서 문서 초안을 작성한다.
4. 각 문서가 `웹 전용 기준 문서`인지 `공유 도메인 검증 문서`인지 구분한다.
5. 이후 실제 구현 시 참조할 핵심 문서를 사용자에게 정리한다.

# Test Plan
- 문서 경로와 파일명 규칙이 기존 `docs` 구조와 충돌하지 않는지 확인
- `docs/web/README.md`만 읽어도 어떤 문서를 먼저 봐야 하는지 파악 가능한지 검토
- 문서 간 중복이 과도하지 않은지 수동 검토

# Review Focus
- `docs/web/`가 `docs/execplans`와 역할 충돌 없이 공존하는지
- 웹 전용 API 분리 원칙과 공유 도메인 검증 포인트가 빠지지 않았는지
- 구현 전에 자주 확인해야 하는 항목이 실제로 접근하기 쉬운 구조인지

# Worktree Split Decision
Single lane

이번 작업은 코드 변경이 아니라 문서 구조와 기준 문서 초안을 한 번에 정렬하는 작업이다. 문서 간 참조 관계와 역할 분리가 동시에 움직이므로 한 레인에서 일관되게 정리하는 편이 안전하다.

# Commit Plan
1. `docs: add web documentation foundation`

# Open Questions
- `docs/web/` 아래에서 이후 화면별 세부 계약 문서를 추가로 분리할지 여부
- 공유 도메인 검증 결과를 계속 `docs/reviews/active/`에 누적할지, 별도 review 문서를 태스크 단위로 분리할지 여부

# Assumptions
- 현재 사용자가 원하는 것은 실제 구현보다 먼저 볼 수 있는 기준 문서 세트의 정리다.
- `docs/web/`는 실행계획/리뷰를 대체하지 않고, 장기 참조용 기준 문서 공간으로 사용한다.

# Validation Update
- 검증 페르소나 리뷰 결과를 `docs/reviews/active/2026-03-19-web-employer-doc-validation-review.md`에 기록했다.
- review 반영으로 employer invitation token 계약, employer auth API 표면, employer endpoint scope 규칙, legacy owner 컬럼 비권한화 원칙을 `docs/web/*`에 추가했다.
- 현재 문서 단계의 다음 작업은 Slice 2 `Auth and profile foundation` 범위를 구현 단위로 다시 자르는 것이다.
