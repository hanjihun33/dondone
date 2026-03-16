# Source Inputs
- 사용자 요구: `TransferScreen.kt`를 서브에이전트 리뷰 후 리팩터링
- 현재 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 호출부: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- 관련 UI model: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 기존 테스트: `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModelTest.kt`
- 서브에이전트 리뷰 노트: `docs/reviews/active/2026-03-16-transfer-screen-refactor-review.md`

# Goal
- `TransferScreen.kt`의 화면 분기, 수취인 탭 상태, 금액 입력 파생값을 정리해 변경 안전성을 높인다.
- 외부 계약 변경 없이 presentation 내부 책임을 helper와 local state로 명확히 나눈다.
- 서브에이전트 리뷰에서 확인한 correctness/maintainability 포인트를 반영한다.

# In Scope
- `TransferScreen.kt` 내부 리팩터링
- 필요 최소한의 호출부 정리 (`DonDoneNavGraph.kt`)
- 계획/리뷰 문서 기록

# Out of Scope
- `TransferUiModel.kt` 계약 변경
- reducer/domain 상태 구조 변경
- 백엔드/API/Auth/DB 변경
- 송금 플로우 자체 재설계

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`

## Docs
- `docs/execplans/active/2026-03-16-transfer-screen-refactor-review.md`
- `docs/reviews/active/2026-03-16-transfer-screen-refactor-review.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- `TransferScreen` 내부에서 더 이상 사용하지 않는 callback은 호출부에서 제거할 수 있음
- 수취인 빈 상태 문구는 탭 맥락에 맞게 달라질 수 있음

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 민감 데이터 처리 방식 변경 없음
- 테스트넷/데모 범위 유지

# Maintainability Notes
- `uiModel.destinationMode`와 별도 탭 state를 동시에 들고 있던 구조는 single source of truth로 정리한다.
- 금액 입력의 파생 문구/shortcut/표시 문자열은 helper와 local data holder로 묶어 composable 본문 분기를 줄인다.
- 의미 있는 숫자 패딩/높이/최대 자리수는 이름을 부여해 하드코딩 확산을 막는다.
- 화면 전용 helper만 추가하고 reducer/domain 쪽 책임은 건드리지 않는다.

# Implementation Steps
1. `TransferScreen.kt`의 상단 분기와 수취인/금액 단계에서 반복되는 파생 로직을 식별한다.
2. 수취인 탭 매핑, 검색 placeholder, 빈 상태 설명, 섹션 제목 매핑을 helper로 정리한다.
3. 금액 입력 단계의 초기값, 단축 입력, 표시 문자열을 local data holder로 정리한다.
4. 호출부에서 더 이상 필요 없는 callback 인자를 제거한다.
5. Android app 모듈 기준 최소 컴파일/단위 테스트를 시도하고 환경 blocker를 기록한다.

# Test Plan
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.remittance.presentation.TransferUiModelTest`
- Gradle 환경이 막히면 blocker를 명시하고 수동 확인 포인트를 남긴다.

# Review Focus
- 탭 상태가 `destinationMode`와 이중 관리되지 않는지
- 빈 상태/검색 placeholder가 계좌/지갑 맥락과 맞는지
- 금액 입력 shortcut과 표시 문자열이 기존 동작과 어긋나지 않는지
- 호출부 정리로 외부 계약이 불필요하게 흔들리지 않았는지

# Worktree Split Decision
- Single lane

대상은 presentation 단일 파일과 호출부 한 곳이지만 상태 파생과 Compose 분기가 촘촘히 얽혀 있습니다. 구현은 한 레인에서 유지하고, 서브에이전트는 읽기 전용 리뷰만 담당합니다.

# Commit Plan
- 1개 커밋
  - `refactor: 정리 transfer screen presentation state`

# Open Questions
- `ACCOUNT` 모드 금액 소스 오브 트루스를 KRW로 분리할지 여부는 후속 과제다.

# Assumptions
- 기대 동작은 현재 송금 플로우 유지이며 이번 작업은 presentation 구조 개선이 목적이다.
- 빈 상태 문구 보정과 불필요 callback 제거는 현재 범위 안의 안전한 정리로 본다.
