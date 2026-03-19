# Source Inputs
- 사용자 요청: 첨부 이미지의 타이포/아이콘 분위기를 참고해 대시보드 탭을 더 유려하게 리디자인
- 현재 대시보드 구성:
  - 요약 카드 + 검색/필터 툴바 + 주간 근태 테이블
- 참고 파일:
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/pages/dashboard/components/DashboardSummaryStats.tsx`
  - `apps/dondone-web/src/pages/dashboard/components/DashboardRequestTable.tsx`
  - `apps/dondone-web/src/pages/dashboard/model/dashboardData.ts`
  - `apps/dondone-web/src/shared/styles/global.css`

# Goal
기능/데이터 구조는 유지하면서, 대시보드의 타이포 스케일, 아이콘 표현, 여백 밀도, 카드/테이블 톤을 이미지 레퍼런스 느낌으로 재정렬한다.

# In Scope
- 대시보드 상단 컨텍스트/타이틀 영역 시각 개선
- 요약 카드 스타일 및 아이콘 톤 정리
- 보드(툴바/칩/테이블) 톤 및 간격 리디자인
- 반응형에서 레이아웃 안정성 유지

# Out of Scope
- 백엔드/API 연동
- 데이터 구조 대규모 변경
- 근태 계산 로직 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
- `apps/dondone-web/src/pages/dashboard/components/DashboardSummaryStats.tsx`
- `apps/dondone-web/src/pages/dashboard/components/DashboardRequestTable.tsx`
- `apps/dondone-web/src/pages/dashboard/model/dashboardData.ts`
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 외부 계약 변경 없음
- 프론트 내부 렌더 구조 일부 변경(상단 context row 및 board header)

# Security Notes
- 인증/인가/토큰/민감정보 경로 영향 없음

# Maintainability Notes
- 기존 컴포넌트 분리 구조 유지
- 스타일 네임스페이스는 `dashboard-*`, `attendance-*` 중심으로 유지해 다른 페이지 영향 최소화
- 데이터 모델 변경은 icon 텍스트 수준으로 제한

# Implementation Steps
1. `EmployerDashboardPage` 상단 컨텍스트 행 및 타이틀 UI 조정
2. 요약 카드/보드 컴포넌트 마크업 소폭 조정
3. `global.css`의 dashboard/attendance 섹션 전면 리톤
4. `dashboardData`의 아이콘 텍스트 톤 정리
5. 빌드 + Playwright 시각 확인

# Test Plan
- `cd apps/dondone-web && npm run build`
- `/dashboard` 수동 확인
  - 상단/요약카드/테이블 밀도 및 정렬
  - 모바일 폭(<=820px)에서 레이아웃 깨짐 여부

# Review Focus
- 스타일 누수로 workers/settings에 영향 없는지
- 타이포 크기/행간이 과도하지 않은지
- 접근성(버튼/표 구조) 유지 여부

# Worktree Split Decision
Single lane

대시보드 컴포넌트와 전역 CSS가 강결합되어 있어 단일 lane에서 연속 검증이 효율적이다.

# Commit Plan
1. `feat(web): retone dashboard visual hierarchy`
2. `style(web): refine attendance board typography and icon mood`

# Open Questions
- 없음

# Assumptions
- 레퍼런스의 UX 구조를 그대로 복제하지 않고, 타이포/아이콘/톤만 반영하는 방향이 요구와 부합한다.
