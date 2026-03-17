## Source Inputs
- `docs/DonDone_PRD_v1.5.md` 13.4 1주차 역할/완료 기준
- Android 현재 구현 상태 탐색
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`

## Goal
- 차지훈 1주차 담당 범위의 미완료 항목을 Android 기준으로 명확히 정리한다.
- Android 앱에서 `asOf` 상태는 존재하지만 조작 UI가 빠진 이유를 해소하고 실제 조작 가능하게 만든다.
- 공통 `loading / error / toast` UI 컴포넌트를 디자인시스템 계층에 추가하고 실제 화면에서 재사용 가능한 형태로 정리한다.

## In Scope
- Android app 상단 chrome 또는 공통 영역에 `asOf` 조작 UI 연결
- Android 공통 UI 컴포넌트 추가:
  - loading state
  - error state
  - toast host / toast message
- 최소 1개 이상 실제 사용처 반영
- 차지훈 1주차 미완료 항목 TODO 정리
- 관련 단위 테스트 보강

## Out of Scope
- 백엔드 API/DTO 변경
- 실제 블록체인 송금 구현 검증
- mockup 웹 버전 구조 변경
- 대규모 내비게이션/상태 관리 리팩터링

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- 필요 시 공통 toast 상태용 신규 파일 추가

### Docs
- `docs/execplans/active/2026-03-17-mobile-week1-followup.md`

### Shared
- 없음

## Contract Changes
- 외부 API 계약 변경 없음
- Android 내부 UI 계약만 확장:
  - 상단 날짜 표시부에 `asOf` 이전/다음 액션 연결
  - 공통 toast state/host 사용 방식 추가

## Security Notes
- 인증/인가 영향 없음
- toast 메시지에 민감 정보 노출 금지
- 데모 날짜 조작은 기존 로컬 demo state 범위 내에서만 동작해야 함

## Maintainability Notes
- `asOf` 조작은 각 화면에 흩뿌리지 않고 app chrome 또는 공통 상단 계층에서 소유한다.
- `Toast.makeText` 직접 호출을 계속 늘리지 않고 공통 host로 흡수한다.
- loading/error 컴포넌트는 개별 화면 전용 문구/스타일을 숨기지 말고, 공통 골격만 제공한다.

## Implementation Steps
1. 현재 Android `asOf` state 보유 경로와 화면 chrome 표시 경로를 기준으로 누락 연결 지점을 확정한다.
2. 차지훈 1주차 미완료 항목을 TODO로 정리한다.
3. `DonDoneApp.kt` 상단 chrome에 날짜 stepper UI를 추가하고 `DemoSessionViewModel.shiftAsOfDay`와 연결한다.
4. 디자인시스템에 `loading / error / toast` 공통 컴포넌트를 추가한다.
5. 기존 `Toast.makeText` 사용처를 공통 toast host 기반으로 치환하거나 공통 API를 거치도록 정리한다.
6. reducer/viewmodel 또는 UI 테스트를 최소 범위로 보강한다.

## Test Plan
- Android 단위 테스트:
  - `DemoSessionReducerTest`
  - 공통 UI 로직 관련 테스트가 가능하면 추가
- Gradle wrapper 실행 가능 시 `./gradlew.unix test`
- 실행 불가 시 네트워크/환경 blocker를 기록

## Review Focus
- `asOf` 조작 UI가 모든 관련 화면에서 일관되게 노출되는지
- 날짜 변경 시 기존 화면 상태 reset/상세 화면 동작과 충돌하지 않는지
- 공통 toast 도입이 기존 메뉴 화면 동작을 깨지 않는지
- 공통 loading/error 컴포넌트가 과도하게 특정 화면 구현에 결합되지 않았는지

## Worktree Split Decision
- Single lane

이 작업은 `DonDoneApp.kt` 상단 chrome, demo session state, 공통 디자인시스템을 함께 건드린다. UI 계약과 상태 연결이 동시에 움직이므로 병렬 분할 시 충돌 위험이 높다.

## Commit Plan
- `feat(mobile): add shared week1 support ui and asOf controls`

## Open Questions
- `asOf` 조작 UI를 홈까지 노출할지 여부는 UX 판단 여지가 있다.

## Assumptions
- 현재 사용자 요청의 우선순위는 Android 앱 완성도 보완이며 mockup은 참고용이다.
- `asOf` UI는 슬라이더보다 상단 날짜 stepper가 현재 Android chrome 구조와 더 잘 맞는다.
- loading/error 컴포넌트는 이번 작업에서 최소 1개 실제 사용처 반영이면 충분하다.
