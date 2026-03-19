# Source Inputs
- 사용자 요청: "여태까지 작성한 코드들중에서 불필요한 섹션 제거/미사용 코드 리팩토링"
- 프론트 현재 라우트 및 페이지 연결
  - `apps/dondone-web/src/app/router.tsx`
  - `apps/dondone-web/src/app/AppShell.tsx`
- 현재 사용 컴포넌트/모델 탐색 결과
  - `apps/dondone-web/src/pages/**`
  - `apps/dondone-web/src/shared/ui/icons.tsx`
  - `apps/dondone-web/src/mocks/employerConsoleData.ts`

# Goal
`apps/dondone-web`에서 실제 참조되지 않는 코드와 의미 없는 분기/필드를 제거해 가독성과 유지보수성을 높이고, 기존 화면 동작은 유지한다.

# In Scope
- 미사용 mock 데이터 모듈 제거 여부 정리 및 적용
- 실제 호출되지 않는 아이콘 export 제거
- 워커 목록 모델/컴포넌트에서 더 이상 쓰지 않는 필드/prop 제거
- 내비게이션 타입/분기에서 도달 불가 코드 제거
- 프론트 빌드 검증

# Out of Scope
- UI/UX 레이아웃 재설계
- 백엔드/모바일 코드 변경
- API/DTO 계약 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-19-web-unused-code-refactor.md`

## Shared
- `apps/dondone-web/src/app/AppShell.tsx`
- `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
- `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
- `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
- `apps/dondone-web/src/shared/ui/icons.tsx`
- `apps/dondone-web/src/shared/styles/global.css` (필요 시 미사용 클래스 정리)
- `apps/dondone-web/src/mocks/employerConsoleData.ts` (미사용 확인 시 제거)

# Contract Changes
- 없음 (프론트 표시/상태 구조 정리만 수행)

# Security Notes
- 인증/인가/토큰 처리 경로 변경 없음
- 외부 API 권한/보안 설정 변경 없음

# Maintainability Notes
- 현재 화면에서 참조되지 않는 데이터 필드/모듈은 그대로 두면 오해를 유발하므로 제거한다.
- 도달 불가 분기(`optional to` 기반 분기 등)는 타입으로 차단해 코드 의도를 명확히 한다.
- 숨김 렌더링(`display:none` 대상 데이터 전달)처럼 의미 없는 데이터 흐름은 끊는다.

# Implementation Steps
1. 코드 참조 검색으로 미사용 모듈/아이콘/필드 후보를 확정한다.
2. `AppShell`의 내비게이션 타입과 렌더 분기를 단순화한다.
3. 워커 목록 모델/리스트/페이지에서 미사용 필드와 prop 전달을 제거한다.
4. `icons.tsx`에서 미사용 export를 제거한다.
5. `employerConsoleData.ts`가 미참조임을 재확인 후 제거한다.
6. 필요 시 관련 CSS 미사용 선택자 정리 후 `npm run build`로 검증한다.

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인
  - 대시보드/근로자 목록/요청 관리/설정 라우팅 정상
  - 근로자 목록 검색/필터 정상
  - 요청 관리 필터/검색/수락/거절 정상

# Review Focus
- 삭제한 코드가 실제 참조 경로에 남아있지 않은지
- 타입 축소 후 컴파일 에러/런타임 에러 없는지
- 화면 동작/문구가 기존 사용자 요구 상태를 유지하는지

# Worktree Split Decision
Single lane

현재 작업은 동일 프론트 모듈(`dondone-web`)의 타입/컴포넌트/스타일을 동시에 정리하는 변경이라 파일 간 결합이 높다. 병렬 분리 이점보다 충돌 위험이 커 단일 레인으로 진행한다.

# Commit Plan
1. `refactor(web): remove dead branches and unused worker props`
2. `refactor(web): prune unused icon exports and mock module`

# Open Questions
- 없음

# Assumptions
- 사용자 요청의 "불필요한 섹션/미사용 코드"는 `apps/dondone-web` 기준으로 해석한다.
- 화면 결과를 바꾸는 대규모 CSS 정리는 이번 범위에서 제외하고, 기능 영향이 없는 안전한 제거만 수행한다.
