# Source Inputs
- 사용자 요구: `apps/dondone-mobile/android`의 하단 `bottom navigation`을 토스 스타일에 가깝게 정리
- 현재 앱 크롬 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 루트 탭/라우트 정의: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/Route.kt`
- 루트 화면 크롬 조건: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- 공통 디자인 시스템: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Color.kt`, `Theme.kt`, `Components.kt`
- 코드 탐색 메모:
  - `Scaffold.bottomBar`에서 `RootBottomBar`를 직접 렌더링
  - 현재는 선택 탭에 칩형 배경을 주는 4분할 탭 구조
  - DTO/API/auth/security 계약 영향 없음

# Goal
- DonDone Android 루트 `bottom navigation`을 토스 홈 하단 바처럼 더 단정하고 평평한 인상으로 재구성한다.
- 기존 루트 탭 구조와 이동 동작은 유지하면서, 선택 상태 표현과 여백/그림자/톤을 토스 레퍼런스에 가깝게 맞춘다.

# In Scope
- `RootBottomBar` 레이아웃, 높이, 패딩, 그림자, 구분선 조정
- `RootTabItem`의 선택 상태 표현 변경
- 필요 시 하단 바 전용 색상 토큰 최소 추가
- safe area와 하단 시스템 제스처 영역에 맞는 패딩 재조정

# Out of Scope
- 루트 탭 종류/라우트 변경
- 상단 바/홈 본문/다른 feature 화면 동시 리디자인
- DTO/API/DB/Auth 계약 변경
- 실제 시스템 navigation bar 제어

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Color.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-bottom-navigation-toss-style.md`

## Shared
- 없음

# Contract Changes
- 없음
- 앱 내부 라우트, DTO, API 응답, DB schema 변경 없음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보 처리 변화 없음

# Maintainability Notes
- bottom navigation 변경은 우선 `DonDoneApp.kt` 범위에 닫아 둔다.
- 토스 스타일을 이유로 전역 테마를 과하게 바꾸지 않는다.
- 재사용 가치가 명확한 색상만 `Color.kt`로 승격하고, 나머지는 하단 바 로컬 상수로 유지한다.
- 선택 상태 계산과 탭 클릭 동작은 현재 구조를 유지해 회귀 위험을 늘리지 않는다.

# Implementation Steps
1. 현재 `RootBottomBar`의 구조와 선택 상태 표현을 토스 스타일 기준으로 단순화한다.
2. 선택 탭은 전체 칩 대신 `소프트 포인트 아이콘 배경 + 진한 라벨`로 전환하고, 비선택 탭은 중성 회색으로 맞춘다.
3. 바 컨테이너를 `화이트 + 라운드 + 얇은 보더 + 낮은 elevation` 방향으로 정리한다.
4. 하단 safe area와 시각 높이를 조정해 홈 화면 하단 리듬이 과하게 뜨지 않게 맞춘다.
5. 필요하면 관련 상수/헬퍼를 정리해 읽기 쉽게 유지한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Dkotlin.incremental=false :app:compileDebugKotlin` 로 Kotlin compile 검증
- 가능하면 Android 모듈의 `assembleDebug` 또는 최소 compile 태스크 실행
- 실행 환경이 허용하면 Compose 미리보기/앱 실행으로 하단 탭 클릭과 선택 상태를 수동 확인

# Review Focus
- 탭 이동 동작이 기존과 동일한가
- 선택/비선택 상태 구분이 충분히 명확한가
- 토스 레퍼런스에 가깝되 DonDone 전체 톤과 충돌하지 않는가
- `DonDoneApp.kt`가 과도하게 복잡해지지 않았는가

# Worktree Split Decision
- Single lane

하단 바 스타일 변경은 `DonDoneApp.kt` 내 레이아웃과 선택 상태 표현이 한 파일에 밀집돼 있습니다. 공유 DTO나 API는 없지만, 분할 이점보다 스타일 조정 중 충돌 가능성이 더 커서 단일 레인으로 진행합니다.

# Commit Plan
- `feat: 안드로이드 하단 네비게이션 토스 스타일 정리`

# Open Questions
- 없음

# Assumptions
- 토스 스타일은 시각적 참고 수준으로 적용하고, 아이콘/탭 수/탭 라벨은 유지한다.
- 사용자 의도는 `앱 내부 메뉴 이동용 bottom navigation` 스타일 변경이다.
- 하단 바 외 시스템 navigation UI는 이번 범위에 포함하지 않는다.
