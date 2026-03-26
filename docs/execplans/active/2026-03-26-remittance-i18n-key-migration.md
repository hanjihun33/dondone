## Scope
- 송금 플로우의 한글 하드코딩을 key 기반 번역 구조로 옮긴다.
- 범위는 `TransferUiModel.kt`, `TransferScreen.kt`, `DonDoneNavGraph.kt`, `UiTextTranslator.kt`로 제한한다.
- API, DTO, 저장소, 인증 계약은 변경하지 않는다.

## Files
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModelTest.kt`

## Approach
1. 송금 UI에서 반복되는 제목, 버튼, 상태, 안내 문구를 번역 key로 카탈로그에 추가한다.
2. `TransferUiModel.kt`가 선택 언어를 받아 상태/안내/리모트 게이트 문구를 해당 언어로 생성하도록 바꾼다.
3. `TransferScreen.kt`의 직접 문자열은 `LocalAppLanguage` 기반으로 치환하고, helper 함수는 언어 인자를 받도록 정리한다.
4. 네비게이션에서 현재 언어를 `TransferUiModel` 생성에 전달한다.
5. 컴파일과 remittance 전용 unit test로 회귀를 확인한다.

## Risks
- 송금 화면은 helper 함수와 바텀시트가 많아 일부 직접 문자열이 남을 수 있다.
- 금액 입력/리뷰/트래커 단계의 문자열이 모델과 화면 양쪽에 나뉘어 있어 누락 시 언어가 섞여 보일 수 있다.

## Verification
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.remittance.presentation.TransferUiModelTest`
