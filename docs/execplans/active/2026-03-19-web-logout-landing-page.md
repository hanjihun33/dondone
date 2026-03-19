# Source Inputs
- 사용자 요청: 로그아웃 버튼 클릭 시 이동할 전용 페이지 신규 제작, `docs/screenshot.png` 레퍼런스 톤 반영
- 사용자 추가 요청: 홈 첫 화면 왼쪽 안내 문구 대신 제공된 `Remote Connectivity` HTML 샘플을 React 구조로 변환해 삽입
- 현재 구현 확인:
  - `apps/dondone-web/src/app/AppShell.tsx` (로그아웃 버튼 동작은 팝오버 닫기만 수행)
  - `apps/dondone-web/src/app/router.tsx` (인증/로그아웃 후 전용 라우트 없음)
  - `apps/dondone-web/src/shared/styles/global.css` (웹 전역 톤/타이포 정의)

# Goal
로그아웃 클릭 시 앱 셸(사이드바/헤더) 밖의 독립 페이지로 이동시키고, 스크린샷의 좌우 분할 레이아웃을 DonDone 웹 톤에 맞춰 재해석한 로그아웃/재로그인 안내 화면을 제공한다.

# In Scope
- 로그아웃 후 이동할 신규 페이지 컴포넌트 추가
- 신규 라우트 추가 및 `AppShell` 로그아웃 핸들러에 내비게이션 연결
- 전역 CSS에 신규 페이지 스타일 추가 (데스크톱/모바일 반응형 포함)
- 페이지 카피를 한국어로 구성
- 왼쪽 패널 콘텐츠를 React 기반 `Remote Connectivity` 시각 컴포넌트로 교체

# Out of Scope
- 실제 인증 API 연동 및 세션/토큰 무효화
- 백엔드 엔드포인트/DTO/DB 스키마 변경
- 기존 대시보드/근로자/요청관리/설정 페이지의 구조 개편

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-19-web-logout-landing-page.md` 신규

## Shared
- `apps/dondone-web/src/app/router.tsx`
- `apps/dondone-web/src/app/AppShell.tsx`
- `apps/dondone-web/src/pages/auth/LoggedOutPage.tsx` (신규)
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 없음 (UI/라우팅 변경만 수행)

# Security Notes
- 이번 변경은 클라이언트 내 화면 전환 수준이며 인증/인가 정책 변경은 없음
- 실제 배포 시 로그아웃 버튼에서 토큰 삭제/세션 만료 API 호출 연동 필요 (이번 범위 외)

# Maintainability Notes
- 신규 화면은 `pages/auth`로 분리해 도메인 경계를 명확히 유지
- 기존 토큰/폰트 변수(`:root`)를 재사용해 중복 스타일 변수 추가를 최소화
- 라우터 구조는 `AppShell` 외부 라우트로 분리해 향후 로그인/비밀번호 재설정 화면 확장에 대비

# Implementation Steps
1. `pages/auth/LoggedOutPage.tsx` 생성: 좌측 브랜드 패널 + 우측 재로그인 폼 레이아웃 및 한국어 카피 작성
2. `router.tsx`에 `/logged-out` 라우트 추가, 기존 루트 구조와 충돌 없도록 셸 외부 엔트리로 배치
3. `AppShell.tsx` 로그아웃 핸들러에서 팝오버 닫기 후 `/logged-out`로 이동하도록 변경
4. `global.css`에 `.logged-out-page` 계열 스타일 및 모바일 반응형 규칙 추가
5. `apps/dondone-web`에서 빌드 검증 실행

# Test Plan
- `cd apps/dondone-web && npm run build`
- 수동 확인:
  - 헤더 우측 프로필 > 로그아웃 클릭 시 `/logged-out` 이동
  - 로그아웃 페이지에서 버튼 동작(대시보드로 돌아가기/로그인) 확인
  - 모바일 폭에서 단일 컬럼으로 정상 전환되는지 확인

# Review Focus
- 로그아웃 클릭 이벤트가 기존 팝오버 상태와 충돌 없이 정상 이동하는지
- 신규 라우트가 `AppShell`을 우회해 사이드바/헤더가 숨겨지는지
- 신규 스타일이 기존 페이지 전역 스타일에 부작용을 만들지 않는지

# Worktree Split Decision
Single lane

라우팅(`router.tsx`), 셸 동작(`AppShell.tsx`), 전역 CSS(`global.css`)가 동시에 얽혀 있어 병렬 분할 시 충돌 가능성이 높다. 변경 범위도 작아 단일 레인 구현이 더 안전하다.

# Commit Plan
1. `feat(web): add logged-out landing page and wire logout navigation`

# Open Questions
- 없음

# Assumptions
- 로그아웃 후 첫 화면은 임시 인증 진입 화면으로 사용하며 실제 인증 검증은 후속 작업에서 연결한다.
- 스크린샷은 레이아웃 톤 레퍼런스이며 문구/브랜드 요소는 DonDone 기준으로 재작성한다.
