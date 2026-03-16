# Source Inputs
- 사용자 요청: `DonDoneApp` 리뷰 후 리팩토링 진행
- 현재 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 연관 chrome 규칙: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- 연관 route/helper: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/Route.kt`
- 연관 화면 계약: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 기존 테스트: `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/navigation/RouteTest.kt`
- 탐색 및 리뷰 노트: 2026-03-16 메인 스레드 검토, explorer 서브에이전트 분석 결과

# Goal
- `DonDoneApp` 셸에서 near-dead chrome 분기와 empty-string sentinel 표현을 제거한다.
- transfer back 규칙을 셸 본문 밖 helper로 이동해 `DonDoneApp`의 책임을 app shell 조립으로 축소한다.
- 기존 auth gate, workproof reset 계약, 데모 플로우 동작은 유지한다.

# In Scope
- `DonDoneApp.kt`의 셸 상태 조립 정리
- `ScreenChrome.kt`의 헤더 표현 규칙 정리
- transfer back helper 추출
- 관련 단위 테스트 추가
- 계획/리뷰 문서 기록

# Out of Scope
- auth/login/logout 흐름 변경
- `DemoSessionViewModel` 상태 구조 변경
- `WorkproofScreen`의 reset/detail visibility 계약 재설계
- DTO/API/DB/OpenAPI 변경
- 백엔드 및 모바일 디자인 개편

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/TransferBackNavigation.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/navigation/ScreenChromeTest.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/navigation/TransferBackNavigationTest.kt`

## Docs
- `docs/execplans/active/2026-03-16-dondoneapp-shell-refactor.md`
- `docs/reviews/active/2026-03-16-dondoneapp-shell-review.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- `ScreenChrome` 내부 표현은 `String` sentinel 대신 nullable title로 정리한다.
- UI 동작 계약은 유지하며, 헤더 숨김/날짜 노출 규칙을 더 명시적으로 표현한다.

# Security Notes
- auth/authz/token/exposed path 변경 없음
- 로그인 게이트와 세션 복구 동작은 유지한다.
- 민감 데이터 처리나 로그 정책 변경 없음

# Maintainability Notes
- 셸이 빈 문자열로 헤더 숨김을 우회 표현하지 않도록 상태 의미를 명시적으로 만든다.
- transfer back-state machine은 셸 본문에서 분리하되, `DemoSessionViewModel` public API는 그대로 사용한다.
- dirty worktree 상태라 `DonDoneNavGraph.kt`, `DemoSessionViewModel.kt`, `WorkproofScreen.kt`까지 넓히는 구조 변경은 피한다.
- workproof detail/reset 상태는 이번에 제거하지 않고 helper 수준으로만 캡슐화한다.

# Implementation Steps
1. `ScreenChrome`의 title 표현을 nullable로 바꾸고 root route의 dead `showDate` 분기를 제거한다.
2. `DonDoneApp`에서 헤더/date 계산과 workproof transient 상태를 보조 helper로 정리한다.
3. transfer back 규칙을 별도 navigation helper 파일로 옮기고 `DonDoneApp`에서는 action 해석만 수행한다.
4. chrome/back helper에 대한 단위 테스트를 추가한다.
5. Android app 모듈 기준 최소 검증 명령을 실행하고 blocker가 있으면 기록한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.navigation.RouteTest --tests com.dondone.mobile.app.navigation.ScreenChromeTest --tests com.dondone.mobile.app.navigation.TransferBackNavigationTest`
- 필요 시 `./gradlew :app:compileDebugKotlin`
- Gradle/JDK 환경 문제로 막히면 blocker와 수동 확인 포인트를 남긴다.

# Review Focus
- `ScreenChrome` nullable title 전환 후 헤더 숨김/표시 동작이 기존과 일치하는지
- transfer back helper 분리 후 `REVIEWING`, `SUBMITTED`, `CONFIRMED`, step return target 동작이 유지되는지
- root tab/date 규칙 단순화가 실제 route 집합과 모순되지 않는지
- dirty worktree의 기존 auth/advance 변경과 충돌하지 않는지

# Worktree Split Decision
- Single lane

현재 모바일 worktree에 auth/advance 관련 변경이 이미 진행 중이고 `DonDoneApp.kt` 자체도 수정 상태입니다. shared shell 파일을 동시에 여러 레인에서 건드리면 충돌 가능성이 높아 단일 레인으로 유지합니다.

# Commit Plan
- 1개 커밋
  - `refactor: 정리 dondone app shell chrome state`

# Open Questions
- workproof detail/reset ownership을 route wrapper로 이동할지 여부는 후속 과제다.

# Assumptions
- 기대 동작은 현재 로그인 게이트, root tab 구조, 송금 플로우를 그대로 유지하는 것이다.
- 이번 작업은 contract-safe refactor이며 사용자가 체감하는 동작 변화는 없어야 한다.
