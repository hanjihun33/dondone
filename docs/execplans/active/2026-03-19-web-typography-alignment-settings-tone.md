# Source Inputs
- 사용자 요청: 대시보드/근무자 목록/설정 페이지 타이포그래피를 설정 페이지 톤으로 통일
- 현재 대상 파일
  - `apps/dondone-web/src/shared/styles/global.css`
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
  - `apps/dondone-web/src/pages/workers/WorkerSummaryPage.tsx`
  - `apps/dondone-web/src/pages/settings/SettingsPage.tsx`

# Goal
설정 페이지의 제목/본문/보조 텍스트 톤(크기, 두께, 행간, 색상)을 기준으로 대시보드와 근무자 목록의 타이포그래피를 시각적으로 일관되게 맞춘다.

# In Scope
- 타이포그래피 관련 CSS(폰트 크기/weight/line-height/letter-spacing/텍스트 색상) 정렬
- 대시보드/근무자 목록/설정 페이지 범위에서만 적용
- 반응형 구간(모바일)에서 제목 크기 최소 조정

# Out of Scope
- 레이아웃 구조 재배치
- 색상 시스템 전면 개편(배경/보더/컴포넌트 컬러)
- 백엔드/API/데이터 모델 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 없음 (UI 표현만 조정)

# Security Notes
- 인증/인가/토큰/노출 경로 영향 없음

# Maintainability Notes
- 기존 클래스 구조를 유지하고 파일 하단에 “타이포 정렬 오버라이드” 블록을 추가해 충돌 범위를 최소화한다.
- 페이지 스코프(`.dashboard-page`, `.worker-list-page`, `.settings-page`, `.location-settings-page`)를 명시해 다른 화면 파급을 방지한다.

# Implementation Steps
1. 설정 페이지 기준 타이포 값(제목/부제/본문/라벨)을 기준값으로 정의한다.
2. `global.css` 하단에 대상 페이지 스코프 오버라이드 추가.
3. 제목, 부제, 테이블 헤더/셀 텍스트, 버튼/칩 텍스트를 통일값으로 정렬.
4. 모바일 구간(<=768px) 타이틀 크기만 추가 보정.
5. `npm run build`로 검증.

# Test Plan
- `cd apps/dondone-web && npm run build`
- `/dashboard`, `/workers`, `/settings` 수동 확인
  - 제목/부제 크기, 행간, 색상 톤 일치
  - 테이블 텍스트 과대/과소 여부
  - 모바일 폭에서 제목 크기 과대 노출 없는지 확인

# Review Focus
- 오버라이드가 다른 페이지에 누수되지 않는지
- 기존 반응형 규칙과 충돌 없이 최종 우선순위가 의도대로 적용되는지
- 타이포만 바뀌고 레이아웃이 깨지지 않는지

# Worktree Split Decision
Single lane

`global.css` 단일 파일에서 다중 페이지 스타일 우선순위를 조정하는 작업이므로 병렬 분할 이점이 적고 충돌 가능성이 높다.

# Commit Plan
1. `style(web): align dashboard-workers typography to settings tone`

# Open Questions
- 없음

# Assumptions
- “설정페이지 기분”은 설정 페이지의 타이포 스케일/톤을 의미하며, 구조/색상/컴포넌트 형태 변경은 요구하지 않는다.
