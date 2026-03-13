# Source Inputs
- 사용자 요청: `apps/dondone-mobile/mockup/mockup.html` 기준으로 Android 송금 `금액 입력` 화면 스타일 정렬
- mockup 기준 확인: `apps/dondone-mobile/mockup/mockup.html` `#transfer-step-amount` 구간
- 현재 구현 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 공용 디자인 시스템 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Components.kt`, `Color.kt`
- 모바일 explorer 결과: amount step은 회색 카드 3개 + 보라색 텍스트 액션 구조, 공용 `SectionPanel` 수정은 파급이 큼

# Goal
Android 송금 `금액 입력` 화면을 mockup과 가깝게 정렬한다. `받는 사람`, `보내는 계좌`, `보낼 금액`을 동일한 회색톤 카드로 맞추고, 기존 카드형 `변경` 액션은 제거해 보라색 텍스트 액션으로 정리한다.

# In Scope
- `AmountStepCard`의 카드 구조와 색상 톤 조정
- `변경` 액션을 amount step 전용 inline text action으로 변경
- amount 입력부를 mockup에 맞는 보라 포인트 + 회색 카드 조합으로 조정
- 기존 송금 플로우 동작 유지

# Out of Scope
- 백엔드/API/DTO 변경
- 송금 상태 카드(`TransferStatusCard`) 재설계
- 계좌 선택/받는 사람 선택 리스트 화면 전체 재디자인
- 공용 디자인 시스템 전역 색상 리브랜딩

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/core/designsystem/Color.kt`의 기존 상수 재사용

## Docs
- `docs/execplans/active/2026-03-10-transfer-amount-mockup-sync.md`

## Shared
- 없음

# Contract Changes
- 외부 계약 변경 없음
- 내부적으로 `TransferScreen` amount step 전용 로컬 컴포저블 구조가 바뀔 수 있으나 `TransferUiModel` 필드는 유지한다

# Security Notes
- 인증/인가/토큰/외부 노출 경로 영향 없음

# Maintainability Notes
- 공용 `SectionPanel`을 바꾸면 다른 화면 색상까지 바뀌므로 이번 작업은 amount step 전용 로컬 카드 컴포저블로 한정한다
- amount step 시각 규칙은 `TransferScreen.kt` 안에서 닫아두고, shared design system 확장은 이번 범위에서 제외한다
- 기존 송금 단계 전환/뒤로가기 로직은 유지하고 UI만 교체한다

# Implementation Steps
1. `mockup.html` amount step 구조를 기준으로 Compose 카드 구조를 재구성한다
2. `SimpleInfoSection` 재사용 대신 amount step 전용 summary card 컴포저블을 만든다
3. `변경` 액션을 `SecondaryActionButton`에서 inline 보라 텍스트 액션으로 바꾼다
4. `보낼 금액` 입력부를 회색 카드 + 브랜드 포인트 구조로 조정한다
5. 색상/간격을 점검하고 기존 콜백 연결을 유지한다
6. `:app:compileDebugKotlin`로 컴파일 검증한다

# Test Plan
- `cd apps/dondone-mobile/android`
- `./gradlew.bat :app:compileDebugKotlin`
- 수동 확인 포인트:
- `금액 입력` 화면에서 세 카드가 모두 회색톤으로 보이는지
- `변경` 액션이 버튼형 카드가 아니라 보라 텍스트로 보이는지
- 수신자/계좌 변경 후 기존 단계 이동이 유지되는지

# Review Focus
- mockup과 현재 Compose 구현의 카드 톤 차이가 충분히 줄었는지
- amount step 전용 로컬 컴포저블이 다른 송금 단계에 불필요한 결합을 만들지 않았는지
- `변경` 액션 클릭 영역이 지나치게 작아지지 않았는지
- 기존 단계 이동/뒤로가기 로직이 UI 변경으로 깨지지 않았는지

# Worktree Split Decision
- Single lane

`TransferScreen.kt`가 UI 구조와 콜백 연결을 함께 가지고 있어 ownership 경계가 작지 않다. 이번 작업은 shared DTO는 안 바꾸지만 amount step 구조와 액션 배치를 한 파일에서 같이 조정해야 하므로 병렬 분할 이점이 낮다.

# Commit Plan
- 1개 커밋으로 정리
- 범위: `feat/mobile remittance amount step mockup sync`

# Open Questions
- 없음

# Assumptions
- 사용자의 `변경 부분 카드를 없애고 보라색으로 변경` 요청은 mockup 기준으로 해석해, 카드형 버튼을 제거하고 보라색 텍스트 액션으로 바꾸는 의미로 본다
- `받는 사람`, `보내는 계좌`, `보낼 금액`은 모두 같은 회색 카드 톤으로 통일한다
- CTA 버튼은 기존 송금 상태 플로우를 유지하되, amount step 카드 영역만 mockup 쪽으로 맞춘다
