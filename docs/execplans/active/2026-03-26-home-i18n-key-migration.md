## Source Inputs

- 사용자 요청: 홈 화면에 보이는 한글을 `string key` 구조로 정리
- [UiTextTranslator.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt)
- [HomeScreen.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt)
- [HomeUiModel.kt](/c:/Users/SSAFY/Documents/workspace/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt)
- 기존 홈 화면 한글 노출 문구 조사 결과

## Goal

홈 화면에 직접 하드코딩된 한글 문구를 key 기반 번역 카탈로그로 옮겨, 영어 선택 시 홈 화면 UI가 일관되게 영어로 표시되도록 만든다.

## In Scope

- 홈 화면에서 직접 노출되는 고정/상태/배너 문구를 `AppTextKeys`와 카탈로그로 추가
- `HomeScreen.kt`의 하드코딩 텍스트 제거
- `HomeUiModel.kt`의 상태/안내/배너 문자열을 key 기반 조회로 전환
- 홈 화면 관련 컴파일 및 단위 테스트 확인

## Out of Scope

- 홈 화면 외 다른 화면의 잔여 한글 하드코딩 정리
- Android string resource 전면 전환
- `HomeUiModel`을 key-only 계약으로 전면 재설계
- 베트남어 등 제3언어 번역 추가

## Affected Modules

### Backend

- 없음

### Mobile

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/i18n/UiTextTranslator.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`

### Docs

- `docs/execplans/active/2026-03-26-home-i18n-key-migration.md`

### Shared

- 없음

## Contract Changes

- 외부 API/DTO/DB 스키마 변경 없음
- 모바일 내부 UI 문자열 소유권만 `Home*` 파일에서 `UiTextTranslator` 카탈로그로 이동

## Security Notes

- 인증/인가/토큰/민감정보 경로 변경 없음

## Maintainability Notes

- 홈 화면 문구를 `UiTextTranslator` 카탈로그로 모아 다른 화면과 같은 번역 소유권 경계를 유지한다.
- 이번 작업에서는 `HomeUiModel` 데이터 구조 자체를 바꾸지 않고, 문자열 생성 지점만 정리해 리스크를 제한한다.
- 동일 의미 문구는 기존 key가 있으면 재사용하고, 홈 전용 의미가 필요한 경우에만 새 key를 추가한다.

## Implementation Steps

1. 홈 화면에서 실제 노출되는 하드코딩 문자열을 고정 텍스트와 동적 메시지로 분류한다.
2. `UiTextTranslator.kt`에 홈 화면용 key와 영문 번역을 추가한다.
3. `HomeScreen.kt`의 섹션 제목, 버튼, 라벨을 `AppLanguage.KOREAN.text(...)` 또는 공용 helper 기반으로 대체한다.
4. `HomeUiModel.kt`의 상태/안내/배너 문구를 key 조회로 바꾼다.
5. 동적 문구는 기존 문자열 템플릿 의미를 유지하면서 영어 문구도 key 기반으로 조합한다.
6. 컴파일과 홈/라우트 관련 테스트를 실행해 회귀를 확인한다.

## Test Plan

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest`
- 필요 시 홈 화면에 간접 영향이 있는 `RouteTest` 재실행

## Review Focus

- 홈 화면에서 영어 선택 시 잔여 한글이 남지 않는지
- 상태별 메시지와 배너 메시지 의미가 기존과 동일한지
- 기존 `translate(...)` fallback과 충돌하지 않는지
- 새 key 추가가 중복/파편화되지 않았는지

## Worktree Split Decision

Single lane

번역 카탈로그와 홈 화면 코드가 같은 문자열 집합을 공유하므로 동시에 나누면 충돌 위험이 높다. 계약 변경은 없지만 공용 key 파일과 화면 구현이 함께 바뀌므로 단일 레인으로 처리한다.

## Commit Plan

- 1차 커밋: 홈 화면 i18n key 추가 및 홈 화면 문자열 마이그레이션
- 필요 시 2차 커밋: 테스트 보완 또는 후속 정리

## Open Questions

- 없음

## Assumptions

- 홈 화면 범위는 `Route.HOME`에서 렌더링되는 `HomeScreen`/`HomeUiModel`로 한정한다.
- 기존 영어 표현은 자연스러운 UI 영어로 다듬어도 기능 요구사항과 충돌하지 않는다.
- 이번 단계에서는 홈 화면에서만 직접 보이는 문구를 우선 정리하고, 다른 화면은 별도 작업으로 이어간다.
