# DonDone Android Implementation Plan

## 목적
- 현재 [mockup.html](C:/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/mockup/mockup.html) 기반 데모를 Android Native(Compose)로 옮길 때, 구현 순서와 구조를 명확히 정리한다.
- [ARCHITECTURE.md](C:/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/mockup/ARCHITECTURE.md)의 분리 원칙을 실제 Android 패키지/화면 구조로 구체화한다.
- PRD v1.5 기준 P0 여정인 `근무 기록 -> 미리받기 -> 급여 점검 -> 문서/신고 준비 -> 테스트넷 송금`을 우선 구현 대상으로 둔다.

## 범위와 가정
- 현재 저장소에는 Android 앱 모듈이 아직 없다.
- 초기 구현은 `단일 app module + feature-first package`로 시작한다.
- `Demo Time Travel`, 시드 데이터, mock 상태는 초기에 유지한다.
- 백엔드 연동 전에도 동작 가능한 `demo/fake repository`를 먼저 만든다.
- 다국어는 JS 사전을 그대로 옮기지 않고 Android `string resources` 기준으로 정리한다.

## 구현 원칙
1. 홈 첫 인상은 기록 앱이 아니라 금융 홈이어야 한다.
2. 계산 로직은 Compose UI 밖으로 빼고, ViewModel 또는 UseCase에서 조립한다.
3. 탭 이동과 금융 내부 상세 흐름은 `NavGraph`로 분리한다.
4. 화면 상태는 `Loading / Empty / Error / Content`를 명시적으로 나눈다.
5. mockup의 `state` 직접 변경 방식은 Android에서 `UiState + Action + Event` 구조로 바꾼다.
6. P0는 빨리 옮기고, 모듈 분리는 코드량이 커진 뒤 진행한다.

## 추천 프로젝트 구조

```text
app/
  src/main/java/com/dondone/app/
    app/
      DonDoneApp.kt
      MainActivity.kt
      bootstrap/
        AppBootstrapper.kt
        DemoSeedFactory.kt
      navigation/
        DonDoneNavGraph.kt
        DonDoneDestination.kt
        MainTab.kt
      session/
        DemoSessionController.kt
        DemoSessionState.kt
    core/
      designsystem/
        theme/
        component/
      ui/
        state/
        formatter/
      model/
      util/
    data/
      demo/
        DemoRepositoryModule.kt
        seed/
      remote/
        dto/
        api/
      repository/
        WorkproofRepositoryImpl.kt
        WageRepositoryImpl.kt
        FinanceRepositoryImpl.kt
        RemittanceRepositoryImpl.kt
      mapper/
    domain/
      model/
      calculator/
        WageEstimator.kt
        AdvanceCalculator.kt
        VaultYieldCalculator.kt
        WorkproofCalculator.kt
      usecase/
        GetHomeSummaryUseCase.kt
        GetAdvanceSnapshotUseCase.kt
        GetWageCheckUseCase.kt
        SubmitTransferUseCase.kt
    feature/
      landing/
      onboarding/
      home/
      workproof/
      finance/
      wage/
      remittance/
      menu/
      copilot/
      document/
```

## Feature 패키지 내부 규칙
각 feature는 presentation 중심으로 시작하고, 필요한 경우에만 feature 내부 하위 domain을 둔다.

```text
feature/home/
  presentation/
    HomeRoute.kt
    HomeScreen.kt
    HomeViewModel.kt
    HomeUiState.kt
    HomeUiEvent.kt
```

권장 역할:
- `Route`: ViewModel 연결, lifecycle, navigation callback 연결
- `Screen`: 순수 Compose UI
- `ViewModel`: state 조립, action 처리, repository/usecase 호출
- `UiState`: 화면 렌더용 상태 객체
- `UiEvent`: 토스트, 이동, bottom sheet 열기 같은 1회성 이벤트

## mockup 파일과 Android 매핑

| mockup 파일 | 현재 역할 | Android 권장 위치 |
|---|---|---|
| `mockup.html` | 전체 UI 마크업 | `feature/*/presentation/*Screen.kt` |
| `mockup.boot.js` | 탭 설정, 시드, 문구, Android 매핑 메타 | `app/bootstrap`, `app/navigation`, `data/demo`, `res/values/strings.xml` |
| `mockup.domain.js` | 계산/파생값 로직 | `domain/calculator`, `domain/usecase`, 일부 `core/ui/formatter` |
| `mockup.navigation.js` | 탭/오버레이 이동 | `app/navigation/DonDoneNavGraph.kt` |
| `mockup.app.js` | 렌더/이벤트 조립 | 각 feature의 `ViewModel + Route` |

## 화면 매핑 기준

### Main Tabs
- `home` -> `feature/home`
- `send` -> `feature/finance`
- `workproof` -> `feature/workproof`
- `vault(menu)` -> `feature/menu`

### Finance 내부 세부 흐름
- `wage` -> `feature/wage`
- `transfer` -> `feature/remittance`
- `manage` -> `feature/finance` 또는 `feature/account`
- `advance overlay` -> `feature/finance`의 bottom sheet
- `interest overlay` -> `feature/menu` 또는 `feature/finance`의 bottom sheet

현재 mockup에서 하단 탭은 4개처럼 보이지만, 실제 상태상 `wage`, `transfer`, `manage`는 금융 내부 흐름에 가깝다. Android에서는 하단 탭과 내부 상세 흐름을 분리하는 편이 자연스럽다.

## 추천 NavGraph 구조

```text
AppGraph
- Landing
- Onboarding
- MainGraph

MainGraph
- HomeTab
- FinanceTab
- WorkproofTab
- MenuTab

FinanceGraph
- FinanceHome
- WageCheck
- TransferForm
- TransferConfirm
- TransferTracker
- AccountManage

Dialog / BottomSheet
- AdvanceSheet
- VaultSheet
- ClaimSheet
- CopilotSheet
- WorkproofDaySheet
- WorkproofEditSheet
- SettingsSheet
```

## 상태 설계

### 앱 전역 상태
- 로그인 여부
- 현재 언어
- 선택 계좌
- `asOf` 날짜
- 온보딩 완료 여부
- 현재 데모 seed

### 화면별 상태
- `HomeUiState`
- `WorkproofUiState`
- `WageUiState`
- `FinanceUiState`
- `RemittanceUiState`
- `MenuUiState`

### 이벤트 처리
- 버튼 클릭 -> `Action`
- ViewModel 처리 -> `UiState` 갱신
- 1회성 결과 -> `UiEvent` 발행

예시:
- `EnterActualDepositClicked`
- `TransferSubmitClicked`
- `OpenAdvanceSheet`
- `ShowToast("입금이 반영됐어요")`

## Demo Time Travel 설계
`Demo Time Travel`은 특정 화면 기능이 아니라 앱 전체 상태를 바꾸는 세션 기능으로 본다.

권장 위치:
- `app/session/DemoSessionController`
- `data/demo/DemoSeedFactory`

권장 책임:
- 현재 기준일(`asOf`) 보관
- seed reset
- 날짜 이동
- 날짜 변경에 따른 파생 상태 재계산 트리거

모든 feature ViewModel은 이 세션을 구독해 현재 날짜 기준 상태를 다시 계산한다.

## 도메인 계산 로직 이전 기준
현재 [mockup.domain.js](C:/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/mockup/mockup.domain.js)의 계산 함수는 거의 그대로 Kotlin으로 옮길 수 있다.

### 우선 이전 대상
- `computeWageEstimate`
- `computeAdvanceSnapshot`
- `computeVaultApplyAvailable`
- `computeYieldSnapshot`
- `getWorkproofMonthRecords`
- `getWorkproofLevel`
- `getWorkproofStatusKey`

### 이전 원칙
- 포맷 함수와 번역 함수는 domain에 두지 않는다.
- 숫자 계산은 `domain/calculator`
- 화면 조립은 `domain/usecase`
- 최종 표시 문자열은 `ViewModel` 또는 `core/ui/formatter`

## 데이터 계층 전략
초기에는 백엔드가 없어도 데모가 돌아야 하므로 repository interface 뒤에 demo 구현을 먼저 둔다.

```text
ViewModel
  -> UseCase
    -> Repository interface
      -> DemoRepositoryImpl
      -> RemoteRepositoryImpl
```

예시:
- `WorkproofRepository`
- `WageRepository`
- `AdvanceRepository`
- `RemittanceRepository`
- `VaultRepository`

초기에는 `DemoRepositoryImpl`만 연결하고, 이후 API가 붙으면 `RemoteRepositoryImpl`로 교체하거나 분기한다.

## 구현 순서

### 1단계. Android 기본 틀
- Compose 프로젝트 생성
- Theme, Typography, Color, Spacing 토큰 구성
- MainActivity + NavHost + Bottom Navigation 구성
- Demo session/controller 생성

### 2단계. 도메인 이식
- `mockup.domain.js` 계산식을 Kotlin으로 이전
- 계산기 단위 테스트 작성
- demo seed 모델 구성

### 3단계. 핵심 화면 이식
우선순위:
1. Home
2. Workproof
3. Wage
4. Finance
5. Remittance
6. Menu / Documents / Copilot

이 순서가 좋은 이유:
- 홈에서 제품 첫인상을 잡고
- 근무 기록이 돈의 근거가 되고
- 급여 점검이 보호 기능을 설명하고
- 이후 송금과 보관으로 연결되기 때문이다.

### 4단계. 세부 상호작용
- Bottom sheet
- Dialog
- Toast / Snackbar
- Time Travel UI
- PDF/문서 준비 상태 표시

### 5단계. 백엔드 연결
- DTO 정의
- Repository remote 구현
- mock/demo source와 스위치 가능하게 유지

## 화면별 구현 메모

### Home
- 계좌 카드
- 오늘 근무 카드
- 미리받기 진행도
- 이번 달 돈 상태
- 다음 행동 CTA

핵심 포인트:
- 첫 화면에서 금융 앱처럼 느껴져야 한다.
- 근무 카드는 보조이되, 금융 연결 문구를 유지한다.

### Workproof
- 월간 캘린더
- 일자 상세
- 수정 사유 필수
- 수정 이력 보기

핵심 포인트:
- 단순 근태가 아니라 근거 관리 UX여야 한다.

### Wage
- 월간 요약
- 참고용 추정 급여
- 실제 입금 입력
- 차액 원인/근거
- 문서/신고 준비 액션

핵심 포인트:
- "정답 계산기"가 아니라 "확인 도구"라는 톤 유지

### Finance
- 계좌/선택 계좌 상태
- 돈 나누기
- 미리받기
- 보관/이자
- 급여 점검 요약

핵심 포인트:
- 금융 홈의 연장선처럼 보여야 한다.

### Remittance
- 허용 목록 기반 수신자 선택
- 금액 입력
- 확인
- 전송 상태 추적
- 영수증

핵심 포인트:
- 안전정책과 상태 가시성이 중요하다.

## Design System 권장 범위
초기에 꼭 분리할 것:
- `Color`
- `Typography`
- `Spacing`
- `RoundedCorner`
- `Button`
- `StatusBadge`
- `MoneyCard`
- `SectionHeader`
- `ProgressBar`

특히 `미리받기 진행도`, `상태 배지`, `금액 카드`, `바텀시트`는 재사용도가 높아서 초기에 컴포넌트화하는 게 좋다.

## 지금 단계에서 하지 않는 것
- 처음부터 feature별 Gradle module 분리
- 지나치게 세분화된 Clean Architecture
- 실거래/실정산 로직
- 복잡한 DI/플러그인 구성

초기 목표는 `빠르게 보이는 앱`을 만드는 것이고, 구조는 그 뒤를 받쳐주는 수준이면 충분하다.

## 권장 일정

### Week 1
- Android 프로젝트 생성
- design system 기본 구성
- navigation 뼈대
- demo session
- domain 계산기 이전

### Week 2
- Home / Workproof / Wage 화면 이전
- Finance / Remittance 기본 플로우 이전
- mock seed 연결

### Week 3
- 문서/신고 준비
- Copilot UI
- Time Travel polish
- 상태/토스트/에러 처리 정리

## 오픈 이슈
- `send`, `wage`, `transfer`, `manage`를 하나의 Finance graph로 둘지, 일부를 별도 feature로 분리할지
- 다국어 리소스를 `ko/en`만 먼저 가져갈지, 추후 확장 가능한 키 체계로 먼저 설계할지
- `돈 나누기`, `보관/이자`를 금융 탭 전용으로 둘지 홈 추천 카드와 일부 공유할지
- 향후 Android 전용 UI로 더 재해석할지, mockup의 시각 구조를 최대한 그대로 옮길지

## 최종 권장안 요약
- 시작은 `단일 app module + feature-first package`
- mockup의 `boot/domain/navigation/app` 책임을 Android `bootstrap/domain/navigation/presentation`으로 그대로 이관
- `Demo Time Travel`은 앱 전역 세션으로 분리
- 화면 이식은 `Home -> Workproof -> Wage -> Finance -> Remittance` 순서
- 구조보다 중요한 건 P0 데모 완주 가능성이고, 모듈 분리는 실제 코드가 커진 뒤 진행
