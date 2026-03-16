# Source Inputs
- 사용자 요청: `DonDoneApp.kt`의 `@Composable` 단위 서브에이전트 리뷰 후 리팩토링 진행
- 현재 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 최근 셸 정리 결과: `docs/execplans/active/2026-03-16-dondoneapp-shell-refactor.md`
- 최근 리뷰 노트: `docs/reviews/active/2026-03-16-dondoneapp-shell-review.md`
- 상단바 서브에이전트 리뷰 결과: immediate correctness finding 없음, 추가 단순화 여지 존재
- 관련 테스트: `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/navigation/RouteTest.kt`, `ScreenChromeTest.kt`, `TransferBackNavigationTest.kt`

# Goal
- `DonDoneApp.kt` 안의 composable 책임을 auth gate, authenticated shell, chrome renderer로 분리해 파일 역할을 선명하게 만든다.
- `AppTopBar`가 raw route/flag를 직접 해석하지 않고 명시적 state resolver를 기반으로 렌더링하도록 정리한다.
- 기존 UI 동작, auth gate, workproof reset 계약, transfer back 동작은 유지한다.

# In Scope
- `DonDoneApp.kt`의 composable 구조 재배치
- top/bottom chrome composable을 별도 파일로 분리
- top bar state resolver 추가
- 관련 단위 테스트 추가/보강
- 계획/리뷰 문서 기록

# Out of Scope
- `DonDoneNavGraph.kt`, `DemoSessionViewModel.kt`, `WorkproofScreen.kt` 계약 변경
- route 구조 변경
- DTO/API/Auth 정책 변경
- 디자인 리뉴얼

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneAppChrome.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/AppTopBarStateTest.kt`

## Docs
- `docs/execplans/active/2026-03-16-dondoneapp-composable-refactor.md`
- `docs/reviews/active/2026-03-16-dondoneapp-composable-review.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- composable 호출 위치와 file 배치는 바뀌지만 외부 화면 계약은 유지한다.
- top bar 결정 로직을 명시적 state resolver로 이동한다.

# Security Notes
- auth/authz/token/exposed path 변경 없음
- 로그인 복구와 로그인 화면 분기 동작은 유지한다.
- 민감 정보 처리 및 네트워크 호출 변경 없음

# Maintainability Notes
- `DonDoneApp`는 app shell orchestration만 맡고 renderer composable은 별도 파일로 분리한다.
- top bar의 layout 결정은 resolver가 담당하고 composable은 render-only에 가깝게 유지한다.
- dirty worktree 상태라 behavior change보다 구조 분리에 집중하고, 인접 auth/advance 파일은 건드리지 않는다.
- bottom bar는 correctness 이슈가 없어 최소 이동만 적용하고 로직 재설계는 하지 않는다.

# Implementation Steps
1. `DonDoneApp.kt`에서 auth gate와 authenticated shell을 분리한다.
2. top/bottom chrome composable과 renderer helper를 `DonDoneAppChrome.kt`로 이동한다.
3. `AppTopBarState` resolver를 추가해 `AppTopBar`의 raw 분기 조건을 정리한다.
4. resolver 단위 테스트를 추가하고 기존 navigation/chrome 테스트와 함께 실행한다.
5. 문서와 리뷰 노트를 남기고 검증 결과를 기록한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.navigation.RouteTest --tests com.dondone.mobile.app.navigation.ScreenChromeTest --tests com.dondone.mobile.app.navigation.TransferBackNavigationTest --tests com.dondone.mobile.app.AppTopBarStateTest`
- 필요 시 `./gradlew :app:compileDebugKotlin`
- 수동 확인: `HOME`, `WORKPROOF 상세`, `ACCOUNT`, `TRANSFER` 헤더와 하단바 배치

# Review Focus
- auth gate 분리 후 비인증 상태에서 `uiState`를 불필요하게 읽지 않는지
- `AppTopBarState`가 기존 collapsed/root/child 표시 규칙을 그대로 재현하는지
- 파일 분리 후 top/bottom renderer 참조 범위가 과도하게 public으로 새지 않는지
- 최근 shell refactor와 충돌 없이 현재 dirty worktree에 안전한지

# Worktree Split Decision
- Single lane

`DonDoneApp.kt`와 직결된 chrome renderer를 같은 레인에서 정리해야 변경 안전성이 높습니다. shared shell file과 새 chrome file이 같이 움직이므로 병렬 레인 분할은 이득보다 충돌 위험이 큽니다.

# Commit Plan
- 1개 커밋
  - `refactor: 분리 dondone app composable chrome`

# Open Questions
- 없음

# Assumptions
- 범위는 저장소 전체가 아니라 `DonDoneApp.kt`와 직접 연결된 chrome renderer composable에 한정한다.
- top bar/bottom bar의 시각 결과는 유지하고, 이번 작업은 구조적 단순화가 목적이다.
