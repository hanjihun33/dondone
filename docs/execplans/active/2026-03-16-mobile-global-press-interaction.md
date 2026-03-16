# Source Inputs
- 사용자 요구: Android 앱에서 메뉴를 누를 때뿐 아니라 전반 클릭 요소에 인터랙션 추가
- 현재 공통 버튼 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- 현재 하단 탭 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 주요 화면별 로컬 버튼 구현:
  - `feature/home/presentation/HomeScreen.kt`
  - `feature/finance/presentation/FinanceHomeScreen.kt`
  - `feature/wage/presentation/WageScreen.kt`
  - `feature/menu/presentation/MenuScreen.kt`
  - `feature/workproof/presentation/WorkproofScreen.kt`
- 코드 탐색 메모:
  - 공통 `PrimaryActionButton/SecondaryActionButton/PillButton`가 다수 화면에서 재사용됨
  - 홈/금융/급여는 로컬 버튼 wrapper도 별도로 가짐
  - 메뉴/하단 탭/월 이동 버튼 등은 `Modifier.clickable` 기반
  - DTO/API/Auth/Security 영향 없음

# Goal
- Android 앱 전반의 주요 클릭 요소에 일관된 `press interaction`을 추가한다.
- 과한 모션 대신 짧은 scale 반응과 기존 ripple을 조합해 토스 톤과 어울리는 눌림 감을 만든다.

# In Scope
- 공통 press interaction modifier 추가
- 공통 버튼과 주요 로컬 버튼 wrapper에 적용
- 하단 탭, 메뉴 리스트, 주요 클릭 행/텍스트 버튼에 적용

# Out of Scope
- 모든 개별 clickable을 100% 전수 적용하는 대규모 리팩터
- 네비게이션/상태 로직 변경
- DTO/API/DB/Auth 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-global-press-interaction.md`

## Shared
- 없음

# Contract Changes
- 없음
- 내부 UI interaction만 변경

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보 처리 변화 없음

# Maintainability Notes
- interaction 로직은 디자인 시스템 modifier 하나로 모아 중복을 피한다.
- 버튼 wrapper는 공통 modifier를 재사용하고, 화면별 개별 모션 상수 증식을 피한다.
- 메뉴/탭/행 클릭은 우선 주요 요소만 적용해 diff를 통제한다.

# Implementation Steps
1. `Components.kt`에 공통 `pressableScale` modifier를 추가한다.
2. 공통 버튼 컴포넌트에 같은 interaction source 기반 scale을 연결한다.
3. 홈/금융/급여의 로컬 버튼 wrapper에 적용한다.
4. 하단 탭, 메뉴 리스트, 월 이동 버튼, 주요 링크성 클릭 요소에 적용한다.
5. 최소 compile 검증으로 Compose import/interaction source 연결 오류를 확인한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --stop`
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Dkotlin.incremental=false :app:compileDebugKotlin`
- 가능하면 실제 앱에서 버튼, 하단 탭, 메뉴 행 클릭 시 눌림 scale과 ripple 체감 확인

# Review Focus
- 눌림 반응이 과하지 않은가
- 버튼과 탭에서 scale이 잘리거나 레이아웃을 흔들지 않는가
- interaction source 재사용으로 ripple/pressed 상태가 정상 동작하는가

# Worktree Split Decision
- Single lane

공통 modifier와 여러 화면의 wrapper 연결이 한 번에 맞물려서 병렬 분할 이점이 작고 충돌 위험이 높다.

# Commit Plan
- `feat: 모바일 공통 press interaction 추가`

# Open Questions
- 없음

# Assumptions
- 사용자가 원하는 대상은 Android 앱 전반의 주요 클릭 요소다.
- 인터랙션은 `짧은 scale + 기존 ripple 유지` 정도의 미세한 반응이면 충분하다.
