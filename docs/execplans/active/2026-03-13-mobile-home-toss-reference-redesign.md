# Source Inputs
- 사용자 요구: Android `홈 화면만` 토스 레퍼런스 수준으로 최대한 유사하게 재구성하되, 현재 메뉴/기능은 유지
- Android 홈 구현 현황: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 홈 상태 조합 현황: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- 공통 디자인 시스템 현황: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Color.kt`, `Theme.kt`, `Components.kt`
- 앱 공통 크롬 현황: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 레퍼런스 탐색: `https://wwit.design/2021/02/16/toss/`의 `홈, 알림` 섹션 (`toss_home_01` ~ `toss_home_04`)
- explorer 메모:
  - 홈은 `독립 카드 3장` 구조에서 `리스트형 정보 흐름`으로 재배치 필요
  - 공통 토큰은 현재 보라 중심이 강하므로, 1차는 홈 로컬 스타일 우선
  - DTO/API/보안/상태 로직 변경 없이 `presentation + HomeUiModel 재조합` 범위로 제한 가능

# Goal
- DonDone Android 홈 화면을 현재의 벤토/쇼케이스형 카드 레이아웃에서 벗어나, 토스 레퍼런스에 가까운 `화이트 기반 + 얇은 구분 + 리스트형 정보 위계` 홈으로 재구성한다.
- 상단/하단 크롬 톤도 홈과 맞춰 `화이트 배경 + 보라 포인트` 원칙으로 통일한다.
- 현재 홈에서 제공하던 메뉴/기능 진입점은 유지한다.
- 결과가 만족스러우면 이후 다른 화면으로 확장 가능한 기준 스타일을 확보한다.

# In Scope
- 홈 화면의 섹션 순서, 위계, 여백, CTA 배치 재구성
- 홈 화면 전용 색/구분선/타이포/칩/버튼 표현 조정
- 홈의 빠른 액션을 토스식 `아이콘 + 라벨` 행으로 교체
- 루트 화면 상단/하단 크롬의 색/선/강조 톤 조정
- `HomeUiModel`의 홈 전용 표시 필드 재조합 및 정리
- 필요한 경우 홈 전용 경량 컴포넌트 또는 토큰 추가
- 현재 메뉴/네비게이션 기능 유지 검증

# Out of Scope
- 백엔드/모바일 API 계약 변경
- 인증/보안/상태 머신 변경
- 금융/급여/근무/메뉴/송금 화면의 대규모 동시 리디자인
- 토스 디자인의 복제 수준 구현을 위한 자산/아이콘/카피 전면 교체

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/home/presentation/HomeUiModelTest.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 필요 시:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Color.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`

## Docs
- `docs/execplans/active/2026-03-13-mobile-home-toss-reference-redesign.md`

## Shared
- 없음

# Contract Changes
- DTO/API/DB schema/API response 변경 없음
- Android 내부 `HomeUiModel` 표시용 필드 구조는 변경 가능
- 다른 feature와 공유되는 상태 계약은 건드리지 않음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보 처리 변경 없음
- UI 재배치로 인한 보안 규칙 변화 없음

# Maintainability Notes
- 1차 구현은 `feature/home/presentation` 내부에서 최대한 닫는다.
- 기존 벤토형 공통 컴포넌트를 전역 수정하지 않는다.
- 홈에서만 필요한 얇은 row/section 스타일은 홈 로컬에 두고, 재사용 가치가 명확할 때만 `core/designsystem`으로 승격한다.
- `HomeUiModel`에서 더 이상 쓰지 않는 표시용 필드는 이번 작업 안에서 함께 정리한다.
- 상태 로직/계산 로직과 화면 표현 로직이 섞이지 않도록 `HomeUiModel` 조합과 `HomeScreen` 렌더를 분리한다.

# Implementation Steps
1. 홈 현재 섹션을 토스식 정보 흐름으로 재배열한다.
   - 상단 개인화/대표 액션
   - 계좌/돈 상태 요약
   - 오늘 근무/진행 상황
   - 다음 행동
2. `HomeScreen.kt`에서 카드 안의 카드 구조를 제거하고, 리스트형 section/row 중심으로 다시 짠다.
3. 빠른 액션을 `아이콘 + 라벨` 행으로 바꿔 토스식 홈 진입 패턴에 맞춘다.
4. 홈과 루트 크롬(`DonDoneApp.kt`)을 `흰 배경 + 보라 포인트` 톤으로 맞춘다.
5. 홈에서만 쓰는 색/구분선/칩/버튼 표현을 정리한다.
   - 1차는 홈 로컬 스타일 우선
   - 필요 시 최소 토큰만 `Color.kt`에 추가
6. `HomeUiModel.kt`에서 홈 재구성에 맞는 표시 필드만 남기고 불필요한 필드를 정리한다.
7. 기존 액션 연결(`계좌 관리`, `송금하기`, `기록 보기`, `급여 확인`, `금융 보기`, `문서`)이 유지되는지 점검한다.
8. 홈 상태 분기 회귀를 막기 위해 `HomeUiModelTest.kt`를 추가한다.

# Test Plan
- 정적 확인
  - `HomeScreen.kt` import/참조 정리 확인
  - `HomeUiModel.kt`와 `HomeScreen.kt` 간 필드 불일치 확인
- 단위 테스트
  - 입금 전/급여일 전 `nextAction` 유지
  - 차액 발생 시 `WAGE` 라우팅 유지
  - 차이 없음 + 송금 완료 시 `MENU` 라우팅 유지
- 빌드/검증
  - 가능하면 `./gradlew :app:compileDebugKotlin`
  - 가능하면 `./gradlew :app:assembleDebug` 후 에뮬레이터 스크린샷 확인
  - 환경 문제로 불가하면 blocker 명시

# Review Focus
- 토스 레퍼런스에 가까운 정보 위계와 화면 리듬이 형성됐는가
- 홈과 상단/하단 크롬이 색/강조 톤에서 이질감 없이 연결되는가
- 기존 메뉴/행동 진입점이 사라지지 않았는가
- `HomeUiModel` 표시 필드 정리가 과도하지 않은가
- 불필요한 전역 디자인 시스템 변경으로 다른 화면 회귀를 만들지 않았는가

# Worktree Split Decision
- Single lane

이 작업은 `HomeScreen/HomeUiModel`과 `DonDoneApp` 크롬 톤이 결합됩니다. 홈 레이아웃/액션 구성/크롬 톤을 동시에 조정해야 하므로 병렬 분할 이점이 작고, 중간 구조 변경 시 충돌 가능성이 높아 단일 레인으로 진행합니다.

# Commit Plan
- 1개 커밋 기본
  - `feat: redesign android home screen and root chrome with toss-inspired style`
- 공통 토큰까지 일부 확장되면 2개로 분리 검토
  - 홈 화면 재구성
  - 최소 공통 토큰/컴포넌트 추가

# Open Questions
- 토스 유사도를 높이기 위해 상단/하단 크롬 조정을 어느 범위까지 포함할지
- `미리받기 진행도`를 홈에서 얼마나 축소할지 시안 비교 후 확정

# Assumptions
- 홈 화면 외 feature의 구조와 기능은 이번 작업에서 유지한다.
- 토스 레퍼런스는 시각적 참고이며, DonDone 도메인 정보와 카피는 유지한다.
- 현재 홈 액션 흐름은 유지하고, 표현 방식만 재구성한다.
- DTO/API/보안/상태 로직 변경 없이 presentation 레이어로 제한 가능하다.
