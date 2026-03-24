## Source Inputs
- 사용자 요청: `송금하기 > 수신자 추가`에서 `계좌 지갑 관리 > 지갑 추가`와 같은 메뉴/플로우를 쓰도록 공통화
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- 코드 탐색: `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`

## Goal
수신 지갑 추가 UX를 공통 바텀시트로 모듈화하고, `송금`과 `계좌 지갑 관리`가 동일한 휴대폰 검색/직접 입력 플로우를 사용하게 한다.

## In Scope
- 수신 지갑 추가용 공통 UI 모델과 바텀시트 컴포저블 추출
- `AccountManageScreen`에서 기존 추가 시트 대신 공통 시트 사용
- `TransferScreen`의 `수신자 추가` 버튼이 공통 시트를 사용하도록 변경
- `TransferScreen`/네비게이션/ViewModel 콜백 계약을 휴대폰 검색 + `targetUserId`까지 지원하도록 확장

## Out of Scope
- 수신 지갑 수정 플로우 공통화
- 백엔드 계약 변경
- 수신자 리스트 정렬/섹션 정책 변경
- 계좌 송금 탭 UI 개편

## Affected Modules
### Backend
- 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- 신규 공통 recipient add sheet 파일

### Docs
- `docs/execplans/active/2026-03-23-mobile-shared-recipient-add-sheet.md`

### Shared
- recipient add UI model / composable shared surface

## Contract Changes
- `TransferScreen` 콜백이 `targetUserId` 기반 수신 지갑 추가를 지원하도록 확장
- `TransferScreen`이 수신 지갑 추가용 검색 상태와 action state를 별도 입력으로 받는다
- 백엔드 API/DTO 변경 없음

## Security Notes
- 기존 허용 수신자 생성 API만 재사용
- 로그인 없는 경우 전화번호 검색은 여전히 차단
- 지갑 주소 직접 입력 검증 규칙 유지

## Maintainability Notes
- `송금`이 `금융` 화면 모델에 직접 의존하지 않도록 연락처 후보 모델은 공통 recipient 파일로 이동한다.
- 추가 플로우와 수정 플로우는 역할이 달라 이번 범위에서는 추가 플로우만 공통화한다.
- 생성 성공/실패 후 시트 닫힘 규칙은 두 화면에서 동일하게 유지한다.

## Implementation Steps
1. 공통 recipient add candidate/model과 바텀시트 파일을 만든다.
2. `AccountManageUiModel`과 `AccountManageScreen`이 공통 타입/시트를 사용하도록 바꾼다.
3. `TransferScreen`에 공통 시트를 연결하고 기존 단순 직접입력 시트를 제거한다.
4. `DonDoneNavGraph`와 `DemoSessionViewModel`에서 필요한 콜백/상태를 송금 화면에도 전달한다.
5. 최소 회귀 테스트/컴파일 확인을 시도하고 blocker를 기록한다.
6. 리뷰 후속으로 송금 화면의 auth fallback을 제거하고, 공통 시트 실패 메시지와 데모 연락처 시드를 정리한다.

## Test Plan
- recipient add 공통화 후 송금 화면에서 전화번호 검색/직접 입력 진입이 가능한지 확인
- 성공 시 시트 닫힘, 실패 시 시트 유지 규칙 확인
- 가능하면 관련 unit test 또는 빌드 실행
- Android SDK 경로 문제로 실행이 막히면 blocker 명시

## Review Focus
- 송금 화면이 금융 화면 타입에 과도하게 결합되지 않았는지
- 수신 지갑 추가 성공/실패 후 시트 상태 관리가 깨지지 않았는지
- 기존 계좌 지갑 관리의 검색/입력/오류 UX가 유지됐는지

## Worktree Split Decision
- Single lane

상태/콜백/화면이 동시에 바뀌고 shared surface도 새로 생겨 병렬 분리가 안전하지 않다.

## Commit Plan
- `mobile: share recipient add sheet across transfer and account manage`

## Open Questions
- 없음

## Assumptions
- `송금`에서 필요한 UX는 `계좌 지갑 관리`와 같은 추가 플로우이며, 기존 단순 직접입력 시트는 대체한다.
- 송금 화면의 `수신자 추가`는 원격 지갑 모드에서만 보이는 현재 정책을 유지한다.
