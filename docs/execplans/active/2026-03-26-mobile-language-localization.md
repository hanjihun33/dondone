## Source Inputs
- 사용자 요청:
  - 메뉴 > 설정 > 언어에서 한국어/영어 선택 시 실제 UI 반영
  - 향후 베트남어 등 추가 언어 확장을 고려한 구조 필요
  - 영어 번역이 애매한 문구는 우선 한국어 유지 후 잔여 목록 정리
- 코드 탐색 결과:
  - `MenuScreen.kt`에서 언어 선택이 `rememberSaveable("ko")` 수준으로만 유지되고 앱 전역 반영 경로가 없음
  - `values/strings.xml`는 일부 Workproof 문자열만 리소스화되어 있고 `values-en/`는 없음
  - `FinanceHomeUiModel.kt`, `TransactionHistoryUiModel.kt`, `TransactionHistoryScreen.kt`, `core/ui/Formatter.kt`에 한국어 하드코딩 및 `Locale.KOREA`/`Locale.KOREAN` 고정 포맷 존재
  - 한글 하드코딩 밀집 파일 상위 구간: `FinanceHomeUiModel.kt`, `MenuScreen.kt`, `FinanceHomeScreen.kt`, `MenuUiModel.kt`, `TransferUiModel.kt`
- 기존 테스트:
  - `FinanceHomeUiModelTest.kt`
  - `TransactionHistoryUiModelTest.kt`

## Goal
Android 앱에서 선택한 언어를 앱 전역 로케일에 반영하고, 최소한 메뉴/금융 홈/거래내역에 대해 영어 선택 시 DonDone 고유명사를 제외한 UI 문구가 영어로 보이도록 구조를 정비한다. 이후 다른 언어를 추가할 때 enum/리소스/포맷 경로만 확장하면 되도록 만든다.

## In Scope
- Android 전역 언어 설정 모델 추가
- 언어 선택 저장 및 앱 시작 시 로케일 복원
- `MenuScreen.kt` 언어 설정 UI를 전역 상태와 연결
- `core/ui/Formatter.kt`의 통화/날짜 포맷을 현재 로케일 기반으로 정리
- `FinanceHomeUiModel.kt` 내 사용자 노출 문자열을 리소스/현지화 가능한 포맷 경로로 이동
- `TransactionHistoryUiModel.kt`, `TransactionHistoryScreen.kt`의 문자열 및 날짜/숫자 포맷 현지화
- `values/strings.xml` 확장 및 `values-en/strings.xml` 추가
- 번역 애매 문구의 한국어 fallback 유지 및 잔여 목록 정리

## Out of Scope
- 백엔드 API/DTO/DB 변경
- 메뉴/금융/거래내역 외 다른 모든 화면의 완전한 영문화
- 실사용 다국어 번역 품질 검수
- 베트남어 실제 번역 추가

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/DonDoneApplication.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/ui/Formatter.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/TransactionHistoryUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/TransactionHistoryScreen.kt`
- 필요 시 언어 설정 저장용 신규 파일

### Docs
- `docs/execplans/active/2026-03-26-mobile-language-localization.md`

### Shared
- `apps/dondone-mobile/android/app/src/main/res/values/strings.xml`
- `apps/dondone-mobile/android/app/src/main/res/values-en/strings.xml`

## Contract Changes
- 서버 계약 변경 없음
- 앱 내부 계약 변경:
  - 전역 언어 코드(`ko`, `en`, 추후 확장 가능) 모델 추가
  - 현지화가 필요한 UI model 빌더에 `Context` 또는 string resolver 의존성이 일부 추가될 수 있음

## Security Notes
- 인증/인가 영향 없음
- 언어 설정 저장은 민감정보가 아니므로 일반 `SharedPreferences` 수준으로 충분
- 외부 공유/토큰/노출 경로 변경 없음

## Maintainability Notes
- 언어 코드는 문자열 리터럴 분산 대신 단일 enum 또는 모델로 관리해야 추후 베트남어 추가 시 안전하다
- 현재 UI model에 하드코딩된 한국어가 많아 이번 변경에서 최소한 공용 resolver/formatter 경로를 도입해야 파일별 임시 번역 분산을 줄일 수 있다
- 모든 화면을 한 번에 완전 현지화하기는 범위가 크므로, 이번 작업은 사용자 요청 직접 대상 화면 우선 + 잔여 구간 명시 전략으로 마무리한다

## Implementation Steps
1. 앱 전역에서 사용할 언어 모델과 저장소를 추가한다
2. `DonDoneApplication` 또는 앱 진입 지점에서 저장된 언어를 적용한다
3. `MenuScreen.kt` 언어 설정 시 전역 언어를 변경하도록 연결한다
4. 메뉴/금융/거래내역에서 사용자 노출 문자열을 리소스 또는 resolver 기반으로 이동한다
5. 숫자/통화/날짜 포맷을 현재 로케일 기준으로 계산하도록 정리한다
6. 영어 번역 리소스를 추가하고 애매한 문구는 한국어 fallback으로 둔다
7. 테스트를 보강하고 빌드/단위 테스트로 회귀를 확인한다

## Test Plan
- `FinanceHomeUiModelTest.kt`에 영어 로케일 기준 문자열/포맷 검증 추가
- `TransactionHistoryUiModelTest.kt`에 영어 로케일 기준 날짜/라벨 검증 추가
- 가능하면 언어 설정 저장소 단위 테스트 추가
- 검증 명령:
  - `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.FinanceHomeUiModelTest`
  - `./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.TransactionHistoryUiModelTest`
  - 필요 시 `./gradlew :app:testDebugUnitTest`

## Review Focus
- 언어 선택 후 앱 재진입/재구성 시 로케일이 유지되는지
- `Locale.KOREA` 고정 포맷이 남아 영어 화면에서 한국식 포맷을 강제하지 않는지
- 번역이 애매한 문구 fallback이 의도치 않게 빈 문자열이나 키 노출로 보이지 않는지
- 새 언어 추가 시 enum/리소스 추가만으로 확장 가능한 구조인지

## Worktree Split Decision
- Single lane

전역 언어 상태, 공용 포맷터, 금융/메뉴 화면 문자열이 서로 강하게 연결되어 있어 병렬 분할 이점이 낮고 병합 리스크가 크다.

## Commit Plan
- 1차: 언어 설정 저장소 + 앱 로케일 적용
- 2차: 메뉴/금융/거래내역 현지화 + 테스트

## Open Questions
- 없음

## Assumptions
- 이번 작업의 “UI의 모든 문구”는 우선 사용자가 직접 언급한 언어 설정 진입점과 현재 열린 금융 플로우(Menu/Finance/TransactionHistory)를 기준으로 처리한다
- 번역이 애매한 문구는 한국어 유지가 허용되며, 잔여 목록을 별도로 보고하면 된다
- DonDone 브랜드명은 모든 언어에서 그대로 유지한다
