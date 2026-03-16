# Findings
- `TransferScreen.kt` 상단 분기가 `showTrackerScreen` / `showReviewScreen` / `flowStep` 로 퍼져 있어 dead branch 와 읽기 비용이 컸다.
  - 조치: 최상위 라우팅을 단일 `when` 으로 평탄화했다.
- `RecipientStepCard` 가 `uiModel.destinationMode` 와 별도의 `selectedTab` state 를 이중 관리하고 있었다.
  - 조치: 탭 선택값을 `destinationMode` 에서 직접 파생하도록 정리했다.
- 금액 입력, 리뷰, 완료 화면에서 목적지 타입별 문구와 보조 표시 규칙이 반복 분기되고 있었다.
  - 조치: 목적지 요약/금액 화면 카피/리뷰 카피/완료 카피를 helper 와 local data holder 로 정리했다.
- 리뷰 화면의 `입금 계좌/지갑 주소` 행이 읽기 전용처럼 보이지만 실제로는 dismiss 를 트리거하고 있었다.
  - 조치: `TransferReviewInfoRow` 의 클릭을 nullable callback 으로 바꾸고, 해당 행은 non-clickable 로 전환했다.
- 출금 계좌 바텀시트가 선택된 계좌와 무관하게 `KB` 뱃지를 고정 노출하고 있었다.
  - 조치: 고정 브랜드 표기를 제거하고 현재 계좌명 기반 배지 텍스트로 교체했다.

# Open Questions
- 계좌 이체 금액의 소스 오브 트루스가 `KRW 입력값` 이 아니라 `amountUsd(Int)` 라서, `ACCOUNT` 모드에서 사용자가 입력한 원화와 내부 저장값 사이에 절삭 차이가 남는다.
- 이 문제를 근본적으로 해결하려면 `TransferUiModel` 또는 reducer 레벨에서 KRW draft state 를 별도로 가지거나, UI 를 USDC 기반 입력으로 재정의해야 한다.

# Testing Gaps
- `./gradlew :app:compileDebugKotlin` 실행은 시도했지만 Gradle이 `Could not determine a usable wildcard IP for this machine` 환경 오류로 시작되지 못했다.
- 자동 검증이 막혀 있어 수동 확인이 필요한 항목:
  - 리뷰 화면 진입 후 복귀 시 계좌 이체 금액 복원
  - 리뷰 화면 정보 행의 클릭 비활성화 동작
  - 계좌 바텀시트 배지 표기

# Residual Risks
- `ACCOUNT` 모드 금액 절삭 문제는 presentation 파일만으로 완전히 닫지 못했다.
- `TransferScreen.kt` 는 여전히 단일 파일에 많은 composable 을 담고 있어, 후속 변경이 누적되면 다시 비대해질 수 있다.

# Change Summary
- `TransferScreen.kt` 중심으로 control flow, 목적지별 표시 규칙, 리뷰 오버레이 상태, 읽기 전용 정보 행 동작을 정리했다.
- 호출부인 `DonDoneNavGraph.kt` 에서 더 이상 쓰지 않는 callback 계약 하나를 제거했다.
