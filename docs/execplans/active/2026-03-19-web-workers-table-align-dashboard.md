# Source Inputs
- 사용자 요청: 근로자 목록 표를 대시보드 표와 동일 톤으로 정렬(검색/필터 위치, 폰트/타이포 포함)
- 현재 구현 파일
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
  - `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
  - `apps/dondone-web/src/shared/styles/global.css`
- 대시보드 표 참조
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`

# Goal
근로자 목록 페이지를 대시보드 표 UI 패턴과 동일한 정보 구조로 맞추고, 검색/필터 위치와 타이포그래피를 일관되게 통일한다.

# In Scope
- Worker 페이지 상단 액션을 대시보드식 툴바(검색 좌측, 필터 우측)로 변경
- 필터 드롭다운 및 선택 칩 동작 추가
- 근로자 표를 대시보드 표 톤과 유사한 클래스/레이아웃으로 정렬
- 빈 결과 상태 문구 처리

# Out of Scope
- 백엔드 연동
- 페이지네이션 서버 처리
- 근로자 상세 진입 로직 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-19-web-workers-table-align-dashboard.md`

## Shared
- `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
- `apps/dondone-web/src/pages/workers/components/WorkerSummaryList.tsx`
- `apps/dondone-web/src/pages/workers/model/workerSummaryData.ts`
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 없음 (UI 로컬 상태만 변경)

# Security Notes
- 인증/인가/토큰 경로 영향 없음
- 외부 입력은 검색어/필터 로컬 상태 처리만 수행

# Maintainability Notes
- 검색/필터 상태는 `WorkerSummaryPage` 컨테이너에서 관리하고, 리스트 컴포넌트는 표시 전용으로 유지
- 대시보드에서 이미 사용하는 공통 UI 클래스(`attendance-*`)를 재사용해 스타일 중복을 줄임
- 기존 worker 전용 스타일은 하위호환으로 두고, 필요 최소 범위만 override

# Implementation Steps
1. `WorkerSummaryPage.tsx`에 검색/필터 상태 및 필터링 로직 추가
2. worker 상단 헤더 액션을 대시보드식 툴바로 교체
3. `WorkerSummaryList.tsx`에서 칩 영역 제거(상위로 이동), 표 마크업을 대시보드 표 구조와 맞춤
4. 빈 결과 행 렌더링 추가
5. `global.css`에서 worker 툴바/표의 대시보드 톤 정렬용 보정 스타일 추가
6. `npm run build` 검증

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인
  - 근로자 목록: 검색 입력이 좌측, 필터 버튼이 우측
  - 필터 선택 시 칩 표시/해제 동작
  - 검색+필터 조합 시 표 데이터 필터링
  - 결과 0건 시 빈 상태 문구

# Review Focus
- 검색/필터 상태와 테이블 표시 일치 여부
- 대시보드/근로자 목록 간 타이포 일관성
- 모바일 반응형에서 툴바 줄바꿈/정렬 안정성

# Worktree Split Decision
Single lane

동일 프론트 화면 내 컨테이너/프리젠테이션/CSS가 함께 변경되며 클래스 재사용이 있어 충돌 가능성이 높다. 단일 레인에서 구현과 검증을 함께 처리한다.

# Commit Plan
1. `feat(web): align workers toolbar and table with dashboard layout`
2. `style(web): unify workers typography and filter interactions`

# Open Questions
- 없음

# Assumptions
- "대시보드 페이지의 표와 똑같이"는 데이터 컬럼 동일화가 아니라, 검색/필터 위치와 표 시각 톤/타이포 정렬로 해석한다.
- 필터는 현재 mock 데이터 기준 `PDF`, `DOC` 타입 필터로 제공한다.
