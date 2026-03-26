# 2026-03-26 AppLanguage Locale SideEffect Removal

## Scope
- Remove `Locale.setDefault(...)` side effect from `ProvideAppLanguage`
- Replace fragile `AppLanguage.fromDefault()` UI formatting dependencies with explicit language flow where needed
- Verify compile/tests for touched mobile localization paths

## Files
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/AppLanguage.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/ui/Formatter.kt`
- dependent mobile UI/model files that still rely on `AppLanguage.fromDefault()` for UI formatting

## Contract Changes
- None

## Verification
- `:app:compileDebugKotlin`
- targeted unit tests for touched format/model code if needed

## Risks
- Removing global locale mutation may surface places still depending on `Locale.getDefault()` implicitly
