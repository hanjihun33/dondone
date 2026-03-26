## Scope

- Home `Wallet Accounts` screen remaining Korean labels
- `Recipient wallet edit` flow under wallet accounts
- `Transaction History` month/filter/day strings
- Menu `Wage Review` screen remaining Korean copy

## Affected Files

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/TransactionHistoryUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/TransactionHistoryScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`

## Contract Changes

- None. UI copy only.

## Risks

- Existing screen helpers may still depend on raw Korean text through legacy translation fallback.
- Date formatting may need explicit locale handling to keep Korean and English outputs stable.

## Verification

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.TransactionHistoryUiModelTest`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.menu.presentation.MenuUiModelTest` if menu wiring changes
- Run the narrowest wage-related test if present; otherwise compile-only for wage slice
