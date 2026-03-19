# Source Inputs
- 사용자 요청: “설정 페이지에 무엇이 들어가면 좋은지” 제안된 6개 영역 기반으로 실제 화면 구현 요청
- 직전 합의된 설정 범위:
  - 회사 기본 정보
  - 근무/출결 정책
  - 검증/이상탐지 기준
  - 알림 설정
  - 권한 및 계정
  - 데이터/문서 관리
- 코드 탐색:
  - `apps/dondone-web/src/app/router.tsx`
  - `apps/dondone-web/src/app/AppShell.tsx`
  - `apps/dondone-web/src/pages/*`
  - `apps/dondone-web/src/shared/styles/global.css`

# Goal
`/settings` 페이지를 신규 추가하고, 관리자 관점에서 정책을 확인/수정 가능한 MVP 폼 UI를 6개 섹션으로 구성한다.

# In Scope
- `settings` 페이지 라우트 추가
- 사이드바 `설정` 메뉴를 실제 라우트로 연결
- `settings/model`에 정적 폼 데이터 및 옵션 정의
- `settings/components`에서 섹션 카드/폼 컨트롤 렌더링
- 저장 버튼/저장 상태(대기/저장중/완료) UI 제공
- 반응형 대응(기본 모바일 폭에서 읽기 가능)

# Out of Scope
- 실제 API 연동 및 서버 저장
- 권한 기반 필드 노출 제어
- 감사 로그 실제 조회 기능
- 백엔드 DTO/스키마/인증 로직 변경

# Affected Modules
## Backend
- 없음

## Mobile
- 없음

## Docs
- 본 실행계획 문서 추가

## Shared
- `apps/dondone-web/src/app/router.tsx`
- `apps/dondone-web/src/app/AppShell.tsx`
- `apps/dondone-web/src/pages/settings/**` (신규)
- `apps/dondone-web/src/shared/styles/global.css`

# Contract Changes
- 외부 API/DTO/DB 계약 변경 없음
- 프론트 내부 라우팅 계약 변경:
  - `"/settings"` 경로 신규 추가

# Security Notes
- 인증/인가 경로 변경 없음
- 민감정보 신규 수집/전송 없음
- 클라이언트 내 상태만 변경, 네트워크 호출 없음

# Maintainability Notes
- 페이지별 구조 원칙 유지:
  - `pages/settings/SettingsPage.tsx`
  - `pages/settings/components/*`
  - `pages/settings/model/*`
- 섹션/필드 메타데이터를 모델 파일로 분리해 화면 확장 시 JSX 중복 최소화
- 전역 CSS는 `settings-*` 네임스페이스로 분리하여 기존 대시보드/근로자 스타일 충돌 방지

# Implementation Steps
1. `pages/settings/model/settingsData.ts`에 폼 타입, 초기값, 섹션 정의 추가
2. `pages/settings/components/SettingsSectionGrid.tsx`에서 섹션 카드 및 필드 렌더 구현
3. `pages/settings/SettingsPage.tsx`에서 헤더/액션/저장 상태/섹션 조합
4. `router.tsx`에 `/settings` route 추가
5. `AppShell.tsx`에서 `설정` nav item을 링크로 전환
6. `global.css`에 settings 전용 스타일 추가
7. `npm run build` 및 Playwright로 `/settings` 시각 검증

# Test Plan
- 명령 검증:
  - `cd apps/dondone-web && npm run build`
- 수동 검증:
  - 사이드바 `설정` 클릭 시 `/settings` 이동
  - 각 섹션 입력/토글 가능 확인
  - `설정 저장` 클릭 시 상태 배지 문구 전환 확인
  - 모바일 폭에서 카드/폼 줄바꿈 확인

# Review Focus
- 라우터/내비게이션 연결 누락 여부
- 필드 키와 상태 업데이트 함수 매핑 정확성
- CSS 오염 여부(다른 페이지 영향)
- 저장 상태 UX 문구 및 접근성(`button type`, `label`, `role=switch`) 점검

# Worktree Split Decision
Single lane

라우터/사이드바/전역 CSS/신규 페이지를 동시에 수정하므로 파일 경합 가능성이 높고, 연동 확인이 즉시 필요한 UI 작업이다. 단일 lane으로 구현과 검증을 연속 수행하는 것이 안전하다.

# Commit Plan
1. `feat(web): add settings page with policy sections`
2. `style(web): add settings layout and responsive styles`

# Open Questions
- 없음 (현재 요청은 화면 구성 중심으로 명확함)

# Assumptions
- 설정 값은 MVP 데모용 로컬 상태로 관리하며, 실제 저장 API는 후속 작업으로 분리한다.
- UX 텍스트는 한국어 기준으로 제공한다.
