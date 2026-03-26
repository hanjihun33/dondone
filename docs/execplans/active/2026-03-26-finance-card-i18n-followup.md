## Scope
- 금융 홈의 `이번 달 받은 금액` 카드와 `미리받기 잔액` 진행 박스 안쪽 문구를 key 기반으로 옮긴다.
- 범위는 `FinanceHomeUiModel.kt`, `FinanceHomeScreen.kt`, `UiTextTranslator.kt`로 제한한다.
- API, 계산 로직, 상태 계약은 변경하지 않는다.

## Files
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModelTest.kt`

## Approach
1. 카드 내부에서 반복되는 제목, 상태, 보조 설명 문구를 번역 key로 추가한다.
2. `FinanceHomeUiModel.kt`가 현재 언어 기준으로 카드 라벨과 상태 문구를 생성하도록 바꾼다.
3. `FinanceHomeScreen.kt`의 해당 카드/시트에서 직접 박힌 라벨을 key 기반으로 치환한다.
4. 금융 홈 컴파일과 기존 `FinanceHomeUiModelTest`로 회귀를 확인한다.

## Risks
- 금융 홈은 상태 조합이 많아 특정 remote status에서 한국어가 일부 더 남을 수 있다.
- 동일 문구가 여러 카드와 시트에 중복 사용돼 누락 시 언어가 섞여 보일 수 있다.

## Verification
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest`
