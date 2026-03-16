# Source Inputs
- 사용자 제보: `송금하기` 등 화면 전환 시 이전/다음 화면이 겹쳐 보이며 렉처럼 체감됨
- 코드 탐색:
  - 네비게이션 루트: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - 앱 chrome/root scaffold: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
  - 송금 화면: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
  - 공통 터치 모션 primitive: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- 라이브러리 확인:
  - `apps/dondone-mobile/android/app/build.gradle.kts` 에서 `androidx.navigation:navigation-compose:2.9.7`

# Goal
- 화면 이동 시 이전/다음 destination 이 겹쳐 보이는 전환을 제거해 즉시 전환되도록 만든다.
- 전환 중 ghosting/jank 로 체감되는 모션을 공통 경로에서 줄인다.

# In Scope
- `NavHost` 공통 전환 설정 점검 및 필요 시 명시적 비활성화
- 송금 플로우에서 남아 있는 화면 전환 체감용 모션 제거 또는 단순화
- 최소 검증 가능한 빌드/컴파일 확인

# Out of Scope
- 라우트 구조 변경
- UI 카피/레이아웃 대규모 리디자인
- 백엔드/DTO/보안/auth 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-navigation-transition-jank.md`

## Shared
- 없음

# Contract Changes
- 없음
- 화면 전환 체감만 조정하며 상태/DTO/API 계약은 유지

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보/로그 정책 변화 없음

# Maintainability Notes
- 전환 정책은 개별 화면마다 분산하지 말고 `NavHost` 레벨에서 한 번에 제어한다.
- ghosting 원인 후보가 여러 곳이어도, 먼저 공통 전환과 송금 화면 내부 모션만 줄여서 변경 범위를 작게 유지한다.
- 이미 호출 중인 `pressableScale` API 는 유지하되, 필요 시 primitive 구현만 조정해 재확산을 막는다.

# Implementation Steps
1. `navigation-compose` 버전에 맞는 `NavHost` 전환 비활성화 방법을 확인한다.
2. `DonDoneNavGraph.kt` 에서 enter/exit/pop 전환을 명시적으로 제거한다.
3. `TransferScreen.kt` 의 단계 전환 체감용 모션이 남아 있으면 정적 렌더로 단순화한다.
4. 남아 있는 공통 모션이 있으면 최소 범위에서 정리한다.
5. 좁은 컴파일 검증을 시도하고, 환경 blocker 가 있으면 기록한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Dkotlin.incremental=false :app:compileDebugKotlin`
- 수동 확인:
  - 홈에서 `송금하기` 탭 시 이전 화면 겹침 없이 즉시 송금 화면으로 전환되는지
  - 근무/금융/메뉴 탭 전환 시 동일한 ghosting 이 줄었는지
  - 송금 화면 내부 단계 변경에서 흐려지거나 미끄러지는 전환이 남지 않는지

# Review Focus
- `NavHost` 공통 전환이 실제로 제거됐는가
- 화면 이동 동작/back stack 은 유지되는가
- 송금 플로우 단계 표시가 모션 제거 후에도 명확한가

# Worktree Split Decision
- Single lane

공통 `NavHost` 와 송금 화면 모션은 같은 체감 문제를 다루며, 분리하면 중복 탐색 대비 이점이 작다.

# Commit Plan
- `fix: 모바일 화면 전환 ghosting 제거`

# Open Questions
- 실제 체감 문제의 전부가 `NavHost` 기본 전환인지, 일부는 기기 렌더링 성능 문제인지 확정되진 않았다.

# Assumptions
- 현재 사용 중인 `navigation-compose:2.9.7` 의 `NavHost` 는 기본 destination 전환을 내부적으로 적용할 수 있다.
- 사용자가 문제로 본 “겹쳐 보이는 전환”은 화면 이동 시 이전/다음 destination 의 동시 렌더링 체감이다.
