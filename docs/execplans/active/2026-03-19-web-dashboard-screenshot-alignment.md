# Source Inputs
- 사용자 요청: `docs/screenshot.png`를 기준으로 dashboard 페이지 UI 정렬
- 기준 이미지: `docs/screenshot.png` (Employee Attendance 레이아웃)
- 현재 구현 파일:
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/shared/styles/global.css`
  - `apps/dondone-web/src/shared/ui/icons.tsx`

# Goal
dashboard 페이지를 스크린샷과 유사한 `Employee Attendance` 화면 구조(헤더 + 4개 요약 카드 + 검색/필터 바 + 칩 + 주간 출근 테이블)로 수정한다.

# In Scope
- dashboard 페이지 마크업 재구성
- 스크린샷에 맞는 아이콘/버튼/카드/테이블 스타일 적용
- 테이블 더미 데이터 구조 정리 및 화면 바인딩
- 반응형(기존 768px 이하)에서 레이아웃 깨짐 없는 수준 유지

# Out of Scope
- 백엔드 API 연동
- 출근 데이터 계산/검증 로직 변경
- 다른 페이지(workers/issues/settings) 디자인 수정

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
- `apps/dondone-web/src/pages/dashboard/model/dashboardAttendanceData.ts` (신규)
- `apps/dondone-web/src/shared/styles/global.css`
- `apps/dondone-web/src/shared/ui/icons.tsx`

# Contract Changes
- 없음 (UI 데모 데이터/표현만 변경)

# Security Notes
- 인증/인가/JWT/토큰/노출 경로 변경 없음
- 외부 입력 처리 로직 추가 없음

# Maintainability Notes
- dashboard 전용 클래스명을 별도 prefix로 분리해 기존 `.request-*`/`.worker-*` 스타일 충돌을 피한다.
- 표 데이터는 페이지 파일에 하드코딩하지 않고 `model` 파일로 분리해 구조적 수정 비용을 낮춘다.

# Implementation Steps
1. dashboard 전용 모델 파일에 요약 카드/필터 칩/주간 테이블 더미 데이터를 정의한다.
2. `EmployerDashboardPage.tsx`를 스크린샷 구조로 재작성하고 모델 데이터에 바인딩한다.
3. 필요한 공용 아이콘(`search/filter/download/calendar` 등)을 `icons.tsx`에 보강한다.
4. `global.css`에 dashboard 전용 스타일 블록을 추가/조정한다.
5. `npm run build`로 타입/빌드 검증 후 시각적 핵심 포인트를 점검한다.

# Test Plan
- `cd apps/dondone-web && npm run build`
- `/dashboard` 수동 확인:
  - 헤더/요약 카드/검색바/필터칩/주간 테이블 구조가 스크린샷과 유사한지
  - 빈 셀 해치 배경, 상태 pill 색상, 버튼 정렬 확인
  - 768px 이하에서 표 영역 overflow 동작 확인

# Review Focus
- dashboard 변경이 다른 페이지 스타일에 영향 주지 않는지
- 아이콘과 텍스트 정렬(수직 정렬, 간격)이 카드/툴바/셀에서 일관적인지
- 테이블 렌더링 키/반복 구조가 안정적인지

# Worktree Split Decision
Single lane

dashboard 페이지 마크업/스타일/아이콘이 같은 웹 프론트 영역에서 강하게 결합되어 있어, 병렬 분할 시 충돌 위험이 높다. 단일 레인으로 구현-검증을 연속 처리한다.

# Commit Plan
1. `feat(web): align dashboard attendance page to screenshot`
2. `style(web): add dashboard attendance icons and table polish`

# Open Questions
- 없음

# Assumptions
- 스크린샷의 레이아웃/톤을 우선으로 하며, 텍스트/수치/이름은 데모 데이터로 유사하게 맞춘다.
- 기존에 제거한 다운로드 버튼은 스크린샷에 포함되어 있어 dashboard 헤더에 재도입한다.
