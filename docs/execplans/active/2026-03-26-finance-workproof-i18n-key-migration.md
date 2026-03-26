## Source Inputs

- 사용자 요청: 금융/근무 탭도 홈 화면과 같은 방식으로 영어 전환 적용
- [UiTextTranslator.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt)
- [FinanceHomeScreen.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt)
- [FinanceHomeUiModel.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt)
- [WorkproofScreen.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt)
- [WorkproofUiModel.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt)
- [WorkproofDetailSection.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofDetailSection.kt)
- [WorkproofPdfSection.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofPdfSection.kt)

## Goal

금융/근무 탭에서 직접 보이는 한글 문구를 영어 선택 시 일관되게 영어로 표시되도록 정리하고, 언어 전환 후에도 상태 판단이 문자열 원문에 의존하지 않도록 바꾼다.

## In Scope

- `UiTextTranslator`에 금융/근무 탭용 키와 번역 추가
- `FinanceHomeScreen`의 누락 고정 문구와 언어 의존 비교 제거
- `FinanceHomeUiModel`의 상태/동적 메시지 중 탭에서 직접 보이는 핵심 문구 정리
- `WorkproofScreen`, `WorkproofUiModel`의 하드코딩 문구 정리
- 근무 탭 상세/문서 생성 하위 섹션에서 직접 보이는 핵심 문구 정리
- 관련 컴파일/단위 테스트 확인

## Out of Scope

- 계좌 관리, 거래내역, 메뉴 탭 문구 정리
- Android string resource 전체 구조 개편
- 제3언어 번역 추가
- 금융/근무 외 남은 화면의 전면 리팩터링

## Affected Modules

### Backend

- 없음

### Mobile

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofDetailSection.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofPdfSection.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofSharedComponents.kt`

### Docs

- `docs/execplans/active/2026-03-26-finance-workproof-i18n-key-migration.md`

### Shared

- 없음

## Contract Changes

- 외부 API/DTO/DB 스키마 변경 없음
- 모바일 내부 UI 문자열 소유권이 각 화면 파일에서 `UiTextTranslator` 카탈로그로 추가 이동
- `WorkproofUiModel` 생성 함수에 언어 주입이 필요할 수 있음

## Security Notes

- 인증/인가/토큰/민감정보 처리 변경 없음
- 위치/파일 열기/공유 경로는 기존 동작 유지

## Maintainability Notes

- 화면 표시 로직이 특정 한국어 문자열 비교에 의존하는 부분을 상태/플래그 기준으로 바꿔 언어 추가 시 회귀를 줄인다.
- 금융은 기존 `translate(...)` 호환 레이어를 활용하되, 동적 메시지와 상태 문자열은 가능한 key 기반으로 소유권을 명확히 한다.
- 근무는 `stringResource`, 하드코딩, UiModel 생성 문자열이 섞여 있으므로 이번 범위에서는 탭에서 직접 보이는 문구부터 우선 정리한다.

## Implementation Steps

1. 금융 탭에서 직접 노출되는 고정 라벨과 상태 문구를 카탈로그에 추가한다.
2. `FinanceHomeScreen`의 한국어 비교 로직을 상태 플래그 또는 key 기반 판단으로 교체한다.
3. `FinanceHomeUiModel`의 상태/안내/배너 문구를 언어 객체를 통해 생성하도록 바꾼다.
4. 근무 탭의 요약 카드/배너/토스트/상세 진입 라벨을 카탈로그로 옮긴다.
5. `WorkproofUiModel`에 언어를 주입하고 상태/감사 문구를 key 기반으로 바꾼다.
6. 상세/문서 생성 섹션의 직접 노출 문구를 같은 카탈로그로 정리한다.
7. 컴파일과 관련 테스트를 실행해 회귀를 확인한다.

## Test Plan

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.workproof.presentation.WorkproofUiModelTest`
- 필요 시 `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.navigation.RouteTest`

## Review Focus

- 금융/근무 탭에서 영어 선택 시 잔여 한글이 직접 노출되지 않는지
- 상태/배너/버튼 로직이 문자열 원문이 아니라 의미 기준으로 동작하는지
- 근무 위치/첨부/PDF 관련 안내 문구가 기존 흐름을 깨지 않는지
- 카탈로그 키가 중복되거나 화면별로 파편화되지 않았는지

## Worktree Split Decision

Single lane

공용 번역 카탈로그와 두 탭 화면/UiModel 로직이 함께 바뀌므로 병렬 분리는 충돌 가능성이 크다. 문자열 비교 제거와 탭별 하위 섹션 조정까지 포함되어 단일 레인으로 마무리한다.

## Commit Plan

- 1차 커밋: 금융/근무 탭 i18n 키 추가 및 화면/UiModel 마이그레이션
- 필요 시 2차 커밋: 테스트/문구 보정

## Open Questions

- 없음

## Assumptions

- 이번 단계의 `금융/근무 쪽` 범위는 메인 탭과 그 탭에서 직접 여는 하위 상세/문서 생성 시트까지 포함한다.
- 기존 영어 표현은 사용자 이해를 우선해 자연스러운 UI 영어로 다듬어도 된다.
- 거래내역/계좌관리 등 금융 하위 별도 화면은 이번 범위에서 제외한다.
