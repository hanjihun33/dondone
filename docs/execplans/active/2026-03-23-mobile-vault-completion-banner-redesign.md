## Source Inputs
- 사용자 요청: 예치 완료 알림을 송금 완료 알림 디자인 기준으로 개선
- 모바일 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 모바일 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- 모바일 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- 모바일 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- 모바일 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`

## Goal
금융 홈의 예치 완료/실패 상태 노출을 송금 완료 배너와 유사한 수준의 강조도와 정보 위계로 개선해, 예치 결과를 더 빠르게 인지할 수 있게 한다.

## In Scope
- `FinanceHomeScreen`의 예치 상태 패널을 배너형 UI로 재구성
- 성공/실패 상태에 따른 아이콘, 배경, 보조 문구 위계 정리
- 기존 `FinanceHomeUiModel` 상태를 그대로 사용하면서 UI 모델 명칭 정리 범위 검토
- 회귀 방지용 UI 모델/렌더링 테스트 보강
- 홈 송금 완료 배너와 금융 예치 완료 배너가 공통 디자인 시스템 컴포넌트를 쓰도록 정리

## Out of Scope
- 백엔드 API/DTO 변경
- 예치 완료 상태 저장 방식 변경
- 예치 완료 배너 dismiss 저장소 추가
- 홈 화면에 예치 완료 배너를 새로 노출하는 흐름 확장

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- 필요 시 공통 스타일 상수/보조 컴포저블 추가
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModelTest.kt`

### Docs
- `docs/execplans/active/2026-03-23-mobile-vault-completion-banner-redesign.md`

### Shared
- 없음

## Contract Changes
- 없음
- 기존 `VaultRemoteState` / `VaultActionUiState` 소비 방식 유지

## Security Notes
- 인증/권한 경로 변경 없음
- 토큰 처리 및 노출 경로 변경 없음

## Maintainability Notes
- 상태 소유권은 `Home`과 `Finance`가 각자 유지하되, 완료 알림 시각 컴포넌트는 디자인 시스템으로 끌어올려 중복 구현을 없앤다.
- 기존 토스트/상태 메시지 흐름은 유지하고, 화면 내 배너는 `latestStatusText` 계열 데이터만 사용한다.

## Implementation Steps
1. 송금 완료 배너의 시각 구조를 기준으로 예치 상태 패널에서 필요한 요소를 추린다.
2. `FinanceHomeUiModel`의 상태 필드가 배너 렌더링에 충분한지 확인하고, 부족하지 않다면 계약은 유지한다.
3. 디자인 시스템에 송금 완료 배너/토스트 톤과 맞는 공통 dismissible notice 컴포넌트를 추가한다.
4. `HomeScreen`과 `FinanceHomeScreen`이 해당 공통 컴포넌트를 사용하도록 교체한다.
5. 성공/실패에 따라 아이콘, 배경, 보더, 타이포 위계를 공통화한다.
6. UI 모델 테스트를 추가 또는 보강해 상태 문구 회귀를 막는다.

## Test Plan
- `FinanceHomeUiModelTest`에 성공/실패 상태 문구와 안내 문구 노출 회귀 테스트 추가
- 가능하면 `./gradlew :app:testDebugUnitTest --tests 'com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest'` 실행
- Android SDK 경로 문제로 실행이 막히면 blocker를 명시

## Review Focus
- 예치 완료/실패 시 상태 강조도가 실제로 개선됐는지
- 기존 vault 상태 메시지/토스트 흐름을 중복되게 훼손하지 않았는지
- 금융 홈의 다른 카드 스타일과 시각적으로 충돌하지 않는지

## Worktree Split Decision
- Single lane

예치 상태 UI와 테스트가 같은 파일 집합을 공유하고 상태 계약도 단일 ViewModel/UI 모델에 묶여 있어 병렬 분리가 안전하지 않다.

## Commit Plan
- `mobile: improve vault completion banner design`

## Open Questions
- 없음

## Assumptions
- 이번 요청의 핵심은 상태 저장 방식 추가가 아니라 금융 홈에서 보이는 예치 완료 알림의 디자인 개선이다.
- 송금 완료 알림의 레이아웃 감각을 참고하되, 위치는 기존 예치 섹션 안에서 유지한다.
