# Source Inputs
- 사용자 요청: `apps/dondone-web` 디렉토리 구조를 페이지 단위로 완전 분해 (`pages/*/components`, `pages/*/model`)하고 `mocks` 단일 파일 의존 제거.
- 현재 코드 상태 점검:
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/mocks/employerConsoleData.ts`
  - `apps/dondone-web/src/app/router.tsx`

# Goal
`apps/dondone-web`에서 페이지별 모델 소유권을 명확히 하고, `src/mocks/employerConsoleData.ts` 단일 파일 결합을 제거해 UI 변경 시 영향 범위를 축소한다.

# In Scope
- `dashboard`, `workers`, `issues` 페이지별 `model`에 타입/목업 데이터 이동.
- 페이지별 `components`에서 모델 타입을 직접 사용하도록 import 정리.
- 기존 화면 동작/레이아웃 유지 (기능 추가 없음).
- `mocks` 파일 및 참조 제거.

# Out of Scope
- API 연동/서버 통신 도입.
- 라우팅 정책 변경 (`/issues` 유지 여부 등 제품 결정).
- 디자인 재작업(간격/폰트/색상 개편).

# Affected Modules
## Backend
- 없음.

## Mobile
- 없음.

## Docs
- 본 실행 계획 문서 추가.

## Shared
- `apps/dondone-web/src/pages/**`
- `apps/dondone-web/src/mocks/**` (삭제 대상)

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음.
- 내부 프론트엔드 모듈 계약만 변경:
  - `pages/*/model`이 페이지 데이터 타입/샘플을 소유.
  - `pages/*/components`는 해당 모델 타입에 의존.

# Security Notes
- 인증/인가, 토큰, 민감정보 경로 변경 없음.
- 정적 목업 데이터 이동만 수행.

# Maintainability Notes
- 페이지 단위 데이터/컴포넌트 소유권을 분리해 수정 충돌을 줄인다.
- 공용으로 재사용되지 않는 타입은 `shared`로 올리지 않고 페이지 모델에 유지한다.
- 향후 API 연동 시 `model` 파일 교체로 범위를 한정할 수 있게 구조를 고정한다.

# Implementation Steps
1. `pages/dashboard|workers|issues/model`에 필요한 타입/데이터를 독립 정의한다.
2. `pages/.../components`가 `../../../mocks/*` 대신 로컬 `model`을 참조하도록 변경한다.
3. 각 `Page.tsx`에서 새 모델/컴포넌트 import를 사용하도록 정리한다.
4. `src/mocks/employerConsoleData.ts` 참조가 0건임을 확인 후 파일/디렉토리 삭제.
5. `npm run build`로 타입/번들 검증.

# Test Plan
- `cd apps/dondone-web && npm run build`
- 정적 확인:
  - `Select-String`으로 `src/mocks/employerConsoleData` 참조 0건 확인
  - 라우트(`/`, `/workers`, `/issues`) 컴파일 오류 없음 확인

# Review Focus
- 모델 분리 후 import 경로 깨짐 여부.
- 데이터/타입 이동 중 누락 필드 여부(`statusTone`, `chips`, `searchPlaceholders` 등).
- 페이지 UI 렌더 결과가 기존과 동일한지.

# Worktree Split Decision
Single lane

이번 작업은 동일한 페이지 파일/모델 파일을 연쇄적으로 수정하며, 분리 중간 상태에서 import가 쉽게 깨진다. 병렬 분할 이득보다 병합 충돌/컴파일 리스크가 높아 단일 레인으로 진행한다.

# Commit Plan
1. `refactor: 페이지별 model/components 구조로 웹 데이터 분리`
2. `chore: legacy mocks 파일 제거`

# Open Questions
- `/issues` 페이지를 메뉴에서 숨긴 상태로 유지할지, 라우트 자체도 제거할지 제품 결정 필요.

# Assumptions
- 현재 요청의 목적은 “구조 분해”이며 기능/디자인 변경은 포함하지 않는다.
- 페이지별 데이터 중복은 당분간 허용하고, 공용화는 API 연동 단계에서 재평가한다.
