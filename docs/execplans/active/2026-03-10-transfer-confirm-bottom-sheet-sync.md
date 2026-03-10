# Source Inputs
- 사용자 요청: 송금 amount step의 `송금 제출`을 `송금 전 확인`으로 바꾸고, `apps/dondone-mobile/mockup/mockup.html` 기준 확인 UI를 바텀시트로 전환
- mockup 기준 확인: `apps/dondone-mobile/mockup/mockup.html` send confirm overlay 구간
- 현재 구현 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 상태 전이 확인: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- viewmodel/nav 연결 확인: `DemoSessionViewModel.kt`, `DonDoneNavGraph.kt`
- explorer 결과: `SUBMITTED`는 실제 차감 전 상태라 confirm bottom sheet로 재해석 가능, dismiss 액션 추가 필요

# Goal
Android 송금 flow에서 amount step CTA를 `송금 전 확인`으로 바꾸고, `SUBMITTED` 상태를 mockup 기반 `송금 전 확인` 바텀시트로 표현한다.

# In Scope
- amount step CTA 문구 변경
- `SUBMITTED` 상태를 confirm bottom sheet로 노출
- bottom sheet 내부 내용을 mockup 기준으로 계좌/수신자/지갑주소/금액/체크 항목/테스트넷 안내 구조로 변경
- bottom sheet 닫기 액션 추가
- `CONFIRMED` 이후 기존 inline 상태 카드는 유지

# Out of Scope
- backend/API/DTO 변경
- 실제 블록체인/송금 처리 로직 변경
- `CONFIRMED` 이후 tracker overlay 신규 구현
- 송금 외 다른 화면 상태 카드 재설계

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`

## Docs
- `docs/execplans/active/2026-03-10-transfer-confirm-bottom-sheet-sync.md`

## Shared
- 없음

# Contract Changes
- 외부 계약 변경 없음
- 내부 모바일 계약 변경:
- `TransferUiModel`에 confirm bottom sheet 표시/문구용 필드 추가 가능
- `TransferScreen`에 dismiss confirm 콜백 추가

# Security Notes
- 인증/인가/토큰/외부 경로 영향 없음
- 테스트넷/데모 안내 문구는 유지해야 함

# Maintainability Notes
- `SUBMITTED`와 `CONFIRMED`를 같은 상태 카드로 공유하던 로직을 분리해야 하므로, UI 표시 조건은 `TransferUiModel`에서 명시적으로 나누는 편이 안전하다
- dismiss 로직은 reducer/viewmodel에 명시적인 함수로 추가해 `resetTransfer()`와 의미를 분리한다
- confirm bottom sheet 레이아웃은 `TransferScreen.kt` 내부 로컬 컴포저블로 두고 공용 sheet abstraction은 만들지 않는다

# Implementation Steps
1. reducer/viewmodel에 confirm sheet dismiss 액션을 추가한다
2. `TransferUiModel`에 `showConfirmationSheet`와 confirm sheet 표시 텍스트를 정리한다
3. amount step primary CTA 문구를 `송금 전 확인`으로 바꾼다
4. `TransferScreen`에 `ModalBottomSheet` 기반 confirm sheet를 추가한다
5. send confirm mockup 내용을 Compose로 옮기고 `확인 후 보내기 / 닫기` 버튼을 연결한다
6. `showStatusCard` 조건을 `CONFIRMED` 중심으로 조정한다
7. `:app:compileDebugKotlin`로 검증한다

# Test Plan
- `cd apps/dondone-mobile/android`
- `./gradlew.bat :app:compileDebugKotlin`
- 수동 확인 포인트:
- amount step CTA가 `송금 전 확인`으로 보이는지
- 탭 시 바텀시트가 열리고 mockup과 유사한 정보가 보이는지
- `닫기` 또는 sheet dismiss 시 amount step으로 복귀하는지
- `확인 후 보내기` 탭 시 기존 confirmed 흐름으로 넘어가는지

# Review Focus
- `SUBMITTED` 상태가 sheet open 상태로만 사용되고 실제 송금 완료와 혼동되지 않는지
- dismiss/confirm 액션이 amount step 입력값을 의도치 않게 초기화하지 않는지
- testnet/demo 안내가 bottom sheet 내부에서도 유지되는지
- `CONFIRMED` 상태 카드와 confirm sheet 조건이 서로 충돌하지 않는지

# Worktree Split Decision
- Single lane

상태 머신, uiModel, screen, nav wiring이 함께 바뀌므로 단일 lane이 안전하다. 파일 수는 적지만 모두 같은 송금 flow 계약을 공유해 병렬 분할 이점이 낮다.

# Commit Plan
- 1개 커밋
- 범위: `feat/mobile transfer confirm bottom sheet sync`

# Open Questions
- 없음

# Assumptions
- `SUBMITTED`는 실제 송금 완료가 아니라 `송금 전 확인 바텀시트가 열린 상태`로 재해석한다
- `CONFIRMED` 이후 UI는 이번 범위에서 기존 inline 상태 카드 유지로 충분하다
- bottom sheet dismiss는 amount step 입력값은 유지하고 상태만 `IDLE`로 되돌린다
