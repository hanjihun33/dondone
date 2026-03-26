## Scope
- 메뉴 탭에서 보이는 계정, 서비스, 설정, 문서, 신고 준비, 로그아웃 확인 문구를 key 기반 번역 구조에 맞춘다.
- `MenuUiModel.kt`에서 만들어지는 상태/요약/영수증 문구도 선택 언어에 따라 생성되도록 정리한다.

## Files
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`

## Approach
1. 메뉴에서 직접 노출되는 문자열을 key 카탈로그에 추가한다.
2. `MenuUiModel.kt`는 `AppLanguage`를 받아 문서/영수증/fallback 문구를 현지화한다.
3. `MenuScreen.kt`는 시트, 토스트, 공유 제목, 액션 버튼 등 남은 직접 문자열을 `text/translate`로 치환한다.
4. `compileDebugKotlin`과 메뉴/라우트 관련 테스트로 회귀를 확인한다.

## Risks
- 메뉴 시트가 많아서 누락되기 쉽다.
- 영수증 공유 텍스트는 줄바꿈/접두사가 많아 포맷 문자열 이스케이프를 조심해야 한다.

## Verification
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.navigation.RouteTest`
