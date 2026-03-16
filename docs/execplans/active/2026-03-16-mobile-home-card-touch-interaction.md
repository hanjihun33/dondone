# Source Inputs
- 사용자 요구: 홈 화면 카드마다 터치 인터랙션을 추가하되 카드 본문 탭과 내부 CTA 버튼 역할을 구분
- 구현 범위 합의:
  - `급여 점검`은 카드 본문 탭과 버튼 탭을 분리
  - `지금 쓸 수 있는 돈`은 계좌 요약 카드 탭과 `송금하기` 버튼을 분리
  - `오늘 근무`는 요약 카드 탭과 `출근`/`퇴근` 버튼을 분리
- 코드 탐색 결과:
  - 홈 화면: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
  - 공통 interaction primitive: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
  - 홈 다음 행동 타겟 결정: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`

# Goal
- 홈 화면에서 카드형 정보 블록에 메뉴/문서 행과 비슷한 눌림 반응을 추가한다.
- 카드 본문 탭과 CTA 버튼 탭의 목적지를 분리해 터치 의도를 명확하게 만든다.

# In Scope
- `HomeScreen.kt`에서 홈 카드형 UI 재구성
- 카드 본문에 `pressableScale + ripple` 적용
- 홈 카드 본문 탭과 내부 버튼 탭 목적지 분리

# Out of Scope
- 다른 모바일 화면의 인터랙션 패턴 변경
- 네비게이션 라우트/상태 계산 변경
- DTO/API/백엔드/보안 설정 변경

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`

## Docs
- `docs/execplans/active/2026-03-16-mobile-home-card-touch-interaction.md`

## Shared
- 없음

# Contract Changes
- 없음
- `HomeUiModel` 데이터 구조와 라우팅 계약은 유지

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 정보 노출 변화 없음

# Maintainability Notes
- 홈 전용 pressable 카드 wrapper를 화면 내부 helper로 묶어 동일한 scale/ripple 조합을 재사용한다.
- 부모 카드와 자식 버튼의 중첩 clickable 충돌을 피하기 위해 카드 본문과 버튼 영역을 명시적으로 분리한다.
- 라우팅 규칙은 기존 `resolveAction`과 `onOpenWage/onOpenAccount/onOpenWorkproof`를 그대로 재사용해 UI 변경이 상태 계산으로 번지지 않게 한다.

# Implementation Steps
1. 홈 화면에 재사용 가능한 pressable 카드 helper를 추가한다.
2. `지금 쓸 수 있는 돈` 섹션을 `계좌 요약 카드 + 송금 버튼` 구조로 나눈다.
3. `급여 점검` 섹션을 `급여 화면 진입 카드 + 상황별 CTA 버튼` 구조로 나눈다.
4. `오늘 근무` 섹션을 `근무 요약 카드 + 출근/퇴근 버튼` 구조로 나눈다.
5. Compose compile 검증으로 import/레이아웃 오류를 확인한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon :app:compileDebugKotlin`
- 수동 확인:
  - 홈 계좌 카드 탭 시 계좌 화면 이동
  - `송금하기` 버튼 탭 시 송금 화면 이동
  - `급여 점검` 본문 카드 탭 시 급여 화면 이동
  - `급여 점검` 우측 버튼 탭 시 기존 `nextAction` 목적지 이동
  - `오늘 근무` 카드 탭 시 근무기록 화면 이동
  - `출근`/`퇴근` 버튼 탭 시 각각 기존 동작 유지
  - 좁은 폭과 큰 글꼴에서 `급여 점검` 카드/버튼 레이아웃이 깨지지 않는지 확인
  - 카드와 CTA 버튼 경계선 부근 탭에서 오탭 없이 각 클릭 영역이 분리되는지 확인
  - 홈 카드 영역을 누른 뒤 바로 세로 스크롤할 때 제스처 체감이 과도하게 무겁지 않은지 확인

# Review Focus
- 카드 본문과 CTA 버튼의 클릭 영역이 겹치지 않는가
- 홈 카드의 눌림 반응이 기존 버튼 톤과 과도하게 다르지 않은가
- `급여 점검` 카드 본문 탭이 항상 급여 화면으로 고정되는 요구가 코드에 명확히 반영됐는가

# Worktree Split Decision
- Single lane

`HomeScreen.kt` 한 파일 안에서 레이아웃 구조와 클릭 역할을 함께 조정해야 해서 병렬 분할 이점이 작고 충돌 위험이 높다.

# Commit Plan
- `feat: 홈 카드 터치 인터랙션 분리`

# Open Questions
- 없음

# Assumptions
- `급여 점검` 카드 본문은 항상 급여 점검 화면으로 진입해야 한다.
- 홈 카드 본문 탭과 내부 CTA 버튼 탭은 서로 다른 의도를 가진다.
- 현재 홈 섹션 레이아웃은 카드화해도 기존 상태 표현을 유지할 수 있다.
