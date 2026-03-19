# Source Inputs
- 사용자 요청: 근로자 목록을 페이지네이션으로 구현
- 현재 근로자 목록 페이지/리스트 구조
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
  - `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
- 공용 스타일
  - `apps/dondone-web/src/shared/styles/global.css`

# Goal
근로자 목록에 실제 동작하는 페이지네이션을 도입하여 필터/검색 결과를 페이지 단위로 탐색할 수 있게 한다.

# In Scope
- 클라이언트 사이드 페이지네이션 상태(`currentPage`, `rowsPerPage`) 추가
- 필터/검색과 페이지네이션 연동
- 페이지 이동(이전/다음/숫자 버튼) 및 페이지당 행 수 변경 UI 추가
- 페이지네이션 UI 스타일 추가

# Out of Scope
- 서버 사이드 페이지네이션 API 연동
- URL 쿼리 동기화
- 정렬(sorting) 기능 추가

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-19-web-workers-pagination.md`

## Shared
- `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
- `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 없음 (프론트 로컬 상태/표시 로직만 변경)

# Security Notes
- 인증/인가/토큰 처리 변경 없음

# Maintainability Notes
- 페이지네이션 계산은 컨테이너(`WorkerSummaryPage`)에서 수행하고 리스트 컴포넌트는 표시/이벤트 전달 중심으로 유지
- 검색/필터/페이지당 행 수 변경 시 페이지 리셋 정책을 명시적으로 구현해 일관성 유지

# Implementation Steps
1. `WorkerSummaryPage`에 `rowsPerPage`, `currentPage` 상태와 페이지 계산 로직을 추가한다.
2. 검색/필터/페이지당 행 수 변경 시 `currentPage=1`로 리셋한다.
3. `WorkerSummaryList`에 페이지네이션 props와 하단 컨트롤(이전/다음/숫자/행 수 선택)을 추가한다.
4. `global.css`에 페이지네이션 UI 스타일을 추가한다.
5. `npm run build`로 검증한다.

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인
  - 검색/필터 후 페이지네이션 결과가 맞는지
  - 페이지당 행 수 변경 시 페이지 리셋/범위 표시가 맞는지
  - 이전/다음/페이지 번호 버튼 동작 및 비활성 상태가 맞는지

# Review Focus
- 페이지 인덱스 경계값(0건, 마지막 페이지, 행 수 변경 직후)
- 필터/검색과 페이지네이션 상태 동기화
- 기존 레이아웃/타이포와의 일관성

# Worktree Split Decision
Single lane

동일 화면(`workers`)의 상태/표시/스타일이 함께 바뀌는 단일 프론트 작업이라 병렬 분리 이점이 작고 충돌 위험이 크다.

# Commit Plan
1. `feat(web): add client-side pagination to workers list`
2. `style(web): add worker pagination controls`

# Open Questions
- 없음

# Assumptions
- 페이지네이션은 서버 연동 없이 현재 메모리 데이터에 대해 동작한다.
- 기본 페이지당 행 수는 데모 가시성을 위해 5로 설정한다.
