## Source Inputs
- 사용자 제보: 모바일 예치 이자 화면의 `예상 수익` 표시가 기대와 다름
- 현재 mobile vault 구현
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/vault/*.kt`
- 기존 계획
  - `docs/execplans/active/2026-03-20-vault-mobile-integration.md`
- backend vault 계약
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/api/dto/response/VaultSummaryResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/service/VaultYieldService.java`

## Goal
모바일 finance vault bottom sheet의 `예상 수익`이 현재 보관 잔액만 기준으로 고정 표시되는 문제를 수정한다. 예치 전에는 선택한 예치 금액 기준 예상 이자가 보여야 하고, 출금 선택 시에는 출금 후 남는 예치 잔액 기준 예상 이자가 보여야 한다.

## In Scope
- mobile vault 예상 수익 계산 로직 수정
- remote vault summary의 APY/decimals를 사용한 preview 계산 추가
- finance vault UI 모델 단위 테스트 추가

## Out of Scope
- backend vault 계약 변경
- 새 preview API 추가
- finance 화면 레이아웃 개편
- remittance/advance/wage 동작 변경

## Affected Modules
### Backend
- 변경 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModelTest.kt`

### Docs
- 본 execplan

### Shared
- 없음

## Contract Changes
- backend/mobile API contract 변경 없음
- mobile 내부 표시 규칙만 변경
  - `DEPOSIT`: 선택한 예치 금액 기준 예상 이자
  - `WITHDRAW`: 출금 후 남는 예치 금액 기준 예상 이자

## Security Notes
- auth/token 경로 변경 없음
- private key/민감 정보 처리 변경 없음
- disclaimer 문구와 demo-only 제약은 유지

## Maintainability Notes
- preview 계산은 `FinanceHomeUiModel` 내부 helper로 분리해, 표시 규칙을 문자열 포맷 로직과 섞지 않는다.
- backend preview 필드와 mobile 보정 계산의 역할을 명확히 분리한다.
  - summary preview: 현재 principal 기준 서버값
  - mobile 보정 preview: 선택 액션/선택 금액 기준 화면값
- 소수 단위 토큰 표시가 유지되도록 기존 포맷 함수를 재사용한다.

## Implementation Steps
1. remote vault 사용 시 선택 액션과 선택 금액에 따라 preview principal을 계산한다.
2. APY bps와 decimals를 사용해 daily/monthly preview atomic 값을 mobile에서 계산한다.
3. finance vault detail의 `monthlyInterestText`, `dailyInterestText`, `defiMonthlyText`, `totalMonthlyText`에 보정 preview를 반영한다.
4. 예치/출금 선택에 따른 표시 기대값을 단위 테스트로 추가한다.

## Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests 'com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest'`
- Android SDK 환경이 없으면 blocker로 기록

## Review Focus
- 예치 전 선택 금액 기준 예상 이자가 0으로 고정되지 않는지
- 출금 선택 시 남는 예치금 기준으로 preview가 줄어드는지
- APY/decimals 반영이 backend contract와 일치하는지
- 기존 disclaimer와 상태 표시는 그대로 유지되는지

## Worktree Split Decision
Single lane

이번 변경은 `FinanceHomeUiModel`의 preview 계산과 그 테스트가 같이 움직인다. shared DTO나 backend contract 변경은 없지만 한 파일의 계산 규칙과 테스트가 강하게 결합되어 있어 단일 lane이 가장 안전하다.

## Commit Plan
- `fix: vault 예상 수익 프리뷰 계산 보정`
- 필요 시 `test: finance vault ui model 테스트 추가`

## Open Questions
- 없음

## Assumptions
- 현재 사용자 제보의 핵심은 `예상 수익` 섹션이 선택 금액을 반영하지 않는다는 점이다.
- backend에 선택 금액 preview 전용 API를 추가하지 않고 mobile 계산으로 해결해도 P0 범위에 충분하다.
