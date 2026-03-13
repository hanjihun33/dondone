# Source Inputs
- 사용자 요청: `확인 후 보내기` 이후 `apps/dondone-mobile/mockup/mockup.html` 기준의 별도 송금 진행 화면 적용
- mockup 구조 확인: `apps/dondone-mobile/mockup/mockup.html`, `apps/dondone-mobile/mockup/mockup.app.js`
- 기존 Android 송금 구현 확인:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- tester 탐색 메모: `SUBMITTED` 의미 충돌, tracker/confirm/status card의 배타 표시, 뒤로가기 정책 정리가 핵심

# Goal
Android 송금 flow에서 `확인 후 보내기` 직후 mockup과 유사한 전용 tracker 화면을 표시하고, 사용자가 `전송중`과 `완료` 상태를 명확히 인지할 수 있게 만든다.

# In Scope
- 송금 상태를 `확인 바텀시트`와 `tracker 진행 화면`으로 분리
- `확인 후 보내기` 이후 tracker 전용 화면 렌더링
- tracker 화면의 진행/완료 단계, tx hash, 계좌/수신자/금액 정보 표시
- transfer route 상단 chrome title과 뒤로가기 정책을 tracker 상태에 맞게 조정
- reducer/viewmodel에서 demo용 자동 완료 전이 추가
- reducer/uiModel 단위 테스트 보강

# Out of Scope
- backend/API/DTO 변경
- 실제 블록체인 전송 연동
- 홈/문서/다른 화면의 송금 완료 후속 UX 재설계
- transfer route 분리 또는 글로벌 overlay 시스템 도입

# Affected Modules
## Backend
- 없음

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/ScreenChrome.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionReducerTest.kt`

## Docs
- `docs/execplans/active/2026-03-10-transfer-tracker-status-screen.md`

## Shared
- 없음

# Contract Changes
- 외부 계약 변경 없음
- 모바일 내부 `TransferStatus`에 confirm sheet 상태와 tracker 진행 상태를 구분하는 값 추가
- `TransferUiModel`에 tracker 전용 표시 필드 추가

# Security Notes
- 인증/인가/토큰/외부 노출 경로 영향 없음
- 여전히 testnet/demo 안내 문구를 유지해 실제 송금처럼 오해되지 않도록 한다

# Maintainability Notes
- 송금 상태 의미를 reducer 한 곳에서 명확히 정의하고, UI는 `TransferUiModel`의 명시적 플래그만 사용한다
- tracker 화면은 `TransferScreen.kt` 내부의 로컬 컴포저블로 닫아 공용 디자인 시스템 변경을 피한다
- 뒤로가기 규칙은 `DonDoneApp.kt`에 집중시켜 상단 버튼과 시스템 back이 다르게 동작하지 않게 한다

# Implementation Steps
1. `TransferStatus`와 reducer를 정리해 `REVIEWING -> SUBMITTED -> CONFIRMED` 전이를 만든다
2. viewmodel에 demo용 자동 완료 coroutine을 추가한다
3. `TransferUiModel`에 confirm sheet / tracker / 완료 상태의 배타 표시 규칙을 반영한다
4. `TransferScreen`에서 tracker 전용 full-screen 콘텐츠를 추가하고 기존 inline status card 의존을 제거한다
5. `ScreenChrome`와 `DonDoneApp`에서 tracker 제목과 back 동작을 맞춘다
6. reducer/uiModel 단위 테스트와 compile 검증을 실행한다

# Test Plan
- `cd apps/dondone-mobile/android`
- `./gradlew.bat :app:testDebugUnitTest`
- `./gradlew.bat :app:compileDebugKotlin`
- 수동 확인
  - `송금 전 확인 -> 확인 후 보내기` 직후 tracker 전용 화면 표시
  - tracker 첫 진입은 진행 상태, 잠시 후 완료 상태로 전이
  - confirm sheet / tracker / 기존 form이 동시에 보이지 않음
  - tracker 상태의 상단 back이 기존 계좌/수신자 step back으로 빠지지 않음

# Review Focus
- `SUBMITTED`가 더 이상 confirm sheet 상태와 충돌하지 않는지
- 잔액 차감이 완료 전이에 한 번만 반영되는지
- tracker 상태에서 제목/뒤로가기 규칙이 일관적인지
- transfer 진입 재시작 시 이전 tracker 상태가 부적절하게 남지 않는지

# Worktree Split Decision
- Single lane

상태 모델, viewmodel coroutine, transfer UI, app chrome이 같은 송금 계약을 공유하므로 병렬 분리가 안전하지 않다. 이번 범위는 한 lane에서 일관되게 수정하고 함께 검증하는 편이 맞다.

# Commit Plan
- 1개 커밋
- 범위: `feat/mobile transfer tracker status screen`

# Open Questions
- 없음

# Assumptions
- tracker는 별도 nav route 추가 없이 기존 `TRANSFER` route 안에서 전용 full-screen 상태로 구현한다
- mockup과 유사하게 `확인 후 보내기` 이후 짧은 지연 뒤 자동으로 `CONFIRMED`로 전이한다
- tracker를 닫거나 뒤로 나가도 다음 송금 진입 시 `openTransferFlow()`가 상태를 초기화한다
