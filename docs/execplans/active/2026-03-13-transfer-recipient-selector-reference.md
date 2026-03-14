# Source Inputs
- 사용자 요구: 송금하기 버튼을 누르면 보내는 계좌 대신 받는 사람/받는 계좌 선택 화면이 먼저 보이도록 변경
- 추가 요구: 첨부한 레퍼런스 이미지와 유사한 `받는 사람 선택` UI 구성 적용
- 현재 송금 화면 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 현재 송금 UI model 구현: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 송금 단계 전환 로직: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`, `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- 데모 데이터 기준 수신자/계좌 구조: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`
- explorer 메모:
  - 수신자 단계는 현재 단순 row 리스트이며, 재사용 가능한 공통 row/card는 `TransferScreen.kt` 내부 로컬 컴포넌트 중심
  - 수신자 데이터는 현재 `Recipient(name, relationship, address)`만 있어 실제 은행 계좌 메타데이터는 없음
  - 백엔드/API/Auth 영향 없이 Android presentation + reducer 범위에서 닫을 수 있음

# Goal
- 송금 진입 시 첫 단계가 `받는 사람/받는 계좌 선택`이 되도록 유지한다.
- 첨부 레퍼런스를 참고해 수신자 선택 화면을 은행 앱 스타일의 `헤더 질문 + 세그먼트 탭 + 입력 필드 + 섹션 리스트` 구조로 재구성한다.
- 수신자 선택 뒤의 금액 입력 단계도 레퍼런스 스타일의 `출금 계좌/도착지 요약 + 질문형 금액 입력 + 숫자 패드` 구조로 재구성한다.
- 현재 데모 데이터와 송금 플로우를 깨지 않으면서 수신자 선택 경험을 더 직관적으로 만든다.

# In Scope
- `TransferScreen.kt`의 수신자 단계 UI 재구성
- `TransferScreen.kt`의 금액 입력 단계 UI 재구성
- 필요 시 `TransferUiModel.kt`에 수신자/금액 화면 표시용 파생 필드 추가
- 필요 시 송금 상태에 `계좌/지갑` 선택 모드 저장
- 송금 플로우 시작/뒤로가기 규칙과 수신자 단계의 연결 유지
- 최소 단위 테스트 추가 또는 보강

# Out of Scope
- 실제 송금 도메인 모델 변경
- 백엔드/API/DB/Auth 계약 변경
- 송금 완료 트래커/확인 바텀시트 전면 리디자인
- 계좌 OCR/카메라 연동 실제 구현

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionReducerTest.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-13-transfer-recipient-selector-reference.md`

## Shared
- 없음

# Contract Changes
- 외부 DTO/API/DB schema 변경 없음
- Android 내부 `TransferUiModel`에 수신자 선택 화면용 표시 필드/섹션 모델이 추가될 수 있음
- 송금 상태 전이 규칙은 유지하되 시작 step은 `RECIPIENT` 기준으로 유지

# Security Notes
- auth/authz/token/exposed path 영향 없음
- 실제 계좌번호/실송금 기능 추가 없음
- 카메라 아이콘은 시각 요소만 두고 민감 권한 요청은 추가하지 않음

# Maintainability Notes
- 레퍼런스 스타일을 맞추더라도 수신자 단계 전용 UI는 `TransferScreen.kt` 내부에서 우선 닫는다.
- 수신자 분류 규칙은 도메인 규칙이 아니라 표시 규칙이므로 `TransferUiModel`에서 파생한다.
- 현재 데모 데이터에 없는 은행/즐겨찾기 개념을 상태 모델에 과하게 추가하지 않는다.
- 송금 플로우 단계 변경과 수신자 UI 리디자인은 함께 검토하되, 확인/완료 화면까지 불필요하게 건드리지 않는다.

# Implementation Steps
1. 수신자 선택 레퍼런스를 현재 데이터 구조에 맞게 해석하고 필요한 표시용 섹션 모델을 정의한다.
2. `TransferUiModel.kt`에 수신자 row 아이콘/보조 라벨/섹션 정보 등 표시용 파생 필드를 추가한다.
3. `TransferScreen.kt`의 `RecipientStepCard`를 레퍼런스 스타일의 상단 질문형 헤더, 세그먼트 탭, 입력 필드, 섹션 리스트 구조로 재작성한다.
4. `계좌/지갑` 선택 모드가 금액 입력 단계까지 유지되도록 송금 상태와 callback을 정리한다.
5. `AmountStepCard`를 레퍼런스 스타일의 상단 요약, 질문형 금액 입력, 숫자 패드 구조로 재작성한다.
6. 현재 플로우에서 `송금하기` 진입 시 수신자 선택이 먼저 보이고, 계좌 변경/뒤로가기 흐름이 유지되는지 확인한다.
7. 관련 reducer/UI model 테스트를 보강한다.

# Test Plan
- 단위 테스트
  - `openTransferFlow`가 `RECIPIENT`에서 시작하는지 확인
  - `TransferUiModel`의 수신자 표시용 섹션/문구가 기대대로 조합되는지 확인
- 정적 확인
  - `TransferScreen.kt`에서 수신자 선택 콜백 연결 유지 확인
  - 입력 필드/탭/리스트가 다른 step UI와 import 충돌 없이 정리되는지 확인
- 빌드/검증
  - 가능하면 `./gradlew :app:testDebugUnitTest --tests ...`
  - 가능하면 `./gradlew :app:compileDebugKotlin`

# Review Focus
- 사용자가 송금 진입 직후 `받는 사람을 고르는 화면`으로 자연스럽게 인지하는가
- 레퍼런스 구조를 반영하되 현재 데모 데이터와 동작이 어색하게 충돌하지 않는가
- 수신자 선택 후 금액 입력 단계로의 연결이 유지되는가
- 표시용 분류 규칙이 도메인 모델을 오염시키지 않는가

# Worktree Split Decision
- Single lane

송금 단계 전이와 수신자 화면 표시 모델이 동시에 움직입니다. 파일 수는 적지만 flow coupling이 있어서 병렬 분할보다 단일 레인에서 빠르게 정합성을 맞추는 편이 안전합니다.

# Commit Plan
- 1개 커밋 기본
  - `feat: redesign transfer recipient selector flow`

# Open Questions
- `연락처` 탭을 실제 동작 없이 시각 상태만 둘지, 같은 수신자 목록의 보조 뷰로 둘지
- 레퍼런스의 `내 계좌` 섹션을 현재 데이터에서 어떤 의미로 매핑할지

# Assumptions
- 첨부 레퍼런스는 시각/정보 구조 참고용이며, DonDone의 실제 송금 데이터 구조는 그대로 유지한다.
- 현재 `Recipient.address`는 실제 은행 계좌번호가 아니라 지갑 주소이므로, 표시 단계에서 일부 축약/라벨링할 수 있다.
- 카메라 아이콘은 이번 작업에서 장식/미구현 CTA로만 처리해도 된다.

# Follow-up Scope Note
- 추가 사용자 요구에 따라 `TransferTrackerScreen`도 홈 탭과 유사한 화이트 캔버스, 얇은 디바이더, 간결한 상태/정보 섹션 흐름으로 정렬한다.
- 이 후속 정리는 Android presentation 범위에 한정하며 외부 계약, 실제 송금 도메인, 보안 규칙 변경은 없다.
- 추가 정리 범위로 `TransferScreen.kt` 내부에서 더 이상 사용하지 않는 과거 트래커 카드/확인 카드/금액 카드 헬퍼와 관련 파라미터를 제거해 파일 결합도를 낮춘다.
- 리팩토링은 동작 변경 없이 presentation 파일 내부의 죽은 코드, 미사용 색상 상수, 불필요 import 정리에 한정한다.
- 같은 맥락으로 `TransferUiModel.kt`도 현재 화면이 실제 소비하는 파생 필드만 남기고, 과거 확인 시트/이전 트래커 전용 카피와 raw 파생 값은 제거한다.
- 앱 크롬 후속 정리로 `DonDoneApp.kt`와 `ScreenChrome.kt` 사이에서 child screen 전용 뒤로가기 플래그 전달을 제거하고, 현재 동작과 일치하는 최소 계약만 남긴다.
