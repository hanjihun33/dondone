# Source Inputs
- 사용자 요청: 근로자 출/퇴근 오입력 정정 요청을 수락/거절하는 별도 페이지(탭) 추가
- 현재 웹 라우팅/네비게이션 구조
  - `apps/dondone-web/src/app/router.tsx`
  - `apps/dondone-web/src/app/AppShell.tsx`
- 기존 이슈 페이지/데이터 구조
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/issues/components/IssueQueueList.tsx`
  - `apps/dondone-web/src/pages/issues/model/issuesQueueData.ts`
- 대시보드 진입점
  - `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`

# Goal
근로자 출/퇴근 정정 요청을 운영자가 한 화면에서 `수락/거절` 처리할 수 있는 `요청 관리` 탭을 사이드바에 노출하고, 대시보드에서 해당 탭으로 빠르게 이동할 수 있게 한다.

# In Scope
- 사이드바에 `요청 관리` 메뉴 추가 (`/issues` 연결)
- `/issues` 페이지를 근태 정정 요청 큐 화면으로 개편
- 요청별 수락/거절 액션 및 상태 반영(프론트 로컬 상태)
- 요청 상태 필터(대기/수락/거절/전체), 검색(이름/사유)
- 대시보드에서 요청 관리 페이지로 이동 가능한 CTA 추가

# Out of Scope
- 백엔드 API 연동 및 영속 저장
- 실제 권한/감사로그/알림 시스템 구현
- 모바일 앱 화면 동기화

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-19-web-attendance-adjustment-requests-tab.md`

## Shared
- `apps/dondone-web/src/app/AppShell.tsx`
- `apps/dondone-web/src/pages/dashboard/EmployerDashboardPage.tsx`
- `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
- `apps/dondone-web/src/pages/issues/components/IssueQueueList.tsx`
- `apps/dondone-web/src/pages/issues/model/issuesQueueData.ts`
- `apps/dondone-web/src/shared/styles/global.css`
- (필요 시) `apps/dondone-web/src/shared/ui/icons.tsx`

# Contract Changes
- 없음 (프론트 로컬 mock 데이터/상태만 변경)

# Security Notes
- 인증/인가 경로 변경 없음
- 민감정보/토큰 처리 변경 없음
- 다만 운영 액션(수락/거절)은 실제 API 연동 시 감사로그 필수라는 전제 유지

# Maintainability Notes
- 기존 `issues` 경로를 재활용하되, 도메인 용어를 `정산 이슈`에서 `근태 정정 요청`으로 일관되게 정리
- 요청 상태 문자열은 분산 하드코딩하지 않고 모델 타입으로 단일화
- 액션 상태 전이는 페이지 컨테이너(`IssuesQueuePage`)에서 관리하고 리스트 컴포넌트는 표시 전용/콜백 중심으로 유지

# Implementation Steps
1. `issuesQueueData.ts`를 정정 요청 도메인 모델(요청자, 기존 시간, 요청 시간, 사유, 상태)로 재정의한다.
2. `IssuesQueuePage.tsx`에서 로컬 상태(`pending/approved/rejected`)와 필터/검색/카운트 로직을 구현한다.
3. `IssueQueueList.tsx`를 카드형 요청 목록 + `수락/거절` 버튼 콜백 구조로 개편한다.
4. `AppShell.tsx` 네비게이션에 `요청 관리` 항목(`/issues`)을 추가한다.
5. `EmployerDashboardPage.tsx`에 `요청 관리로 이동` CTA를 추가한다.
6. `global.css`에 요청 관리 페이지 전용 스타일(탭, 검색, 액션 버튼, 상태 라벨)을 추가한다.
7. `npm run build`로 타입/번들 검증한다.

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인
  - 사이드바 `요청 관리` 메뉴 노출 및 라우팅
  - 기본 필터 `대기`에서 요청 노출
  - 각 요청 `수락/거절` 클릭 시 상태/카운트 반영
  - 상태 탭/검색 조합 필터 정상 동작
  - 대시보드 CTA 클릭 시 `/issues` 이동

# Review Focus
- 상태 전이(대기 -> 수락/거절) 일관성
- 필터/검색 조합 시 빈 상태 처리 정확성
- 네비게이션 활성 상태 및 페이지 진입 동선
- 기존 워커/설정/대시보드 스타일과 톤 충돌 여부

# Worktree Split Decision
Single lane

라우팅, 내비게이션, 페이지 로직, 스타일이 동시에 바뀌는 프론트 단일 영역 작업이라 충돌 위험이 높고, DTO/API 계약 변경은 없어서 병렬 이점이 작다. 단일 레인에서 구현-검증까지 마무리한다.

# Commit Plan
1. `feat(web): add attendance correction request management tab`
2. `feat(web): implement accept/reject workflow on issues page`
3. `style(web): add request queue interactions and dashboard shortcut`

# Open Questions
- 없음 (현재는 mock 기반 로컬 처리로 가정)

# Assumptions
- 사용자 요청의 `수락/거절 페이지`는 별도 신규 경로 대신 기존 `/issues`를 `요청 관리`로 전환해 제공한다.
- `수락/거절` 처리 결과는 세션 내 로컬 상태만 반영하며 새로고침 시 초기화된다.
- 운영자가 가장 먼저 봐야 할 상태는 `대기`이므로 기본 필터를 `대기`로 둔다.
