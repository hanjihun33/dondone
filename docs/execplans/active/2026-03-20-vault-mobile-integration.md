## Source Inputs
- `docs/DonDone_P0_API_Contract_v0.md`
  - `9. Vault`
- `docs/DonDone_P0_Functional_Spec_v0.md`
  - `10. Vault`
- 현재 backend vault 계약
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/api/VaultController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/api/dto/response/*.java`
- 현재 mobile finance 구조
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/*`

## Goal
Android finance 화면의 vault 영역을 backend async vault 계약에 맞춰 실연동한다. 사용자는 vault summary를 보고 예치 또는 출금 요청을 만들 수 있어야 하며, 요청 상태를 `REQUESTED/BROADCASTED/CONFIRMED/FAILED` 기준으로 확인할 수 있어야 한다.

## In Scope
- mobile `vault` repository 및 remote state 추가
- `DemoSessionViewModel`에 vault load/action/polling 추가
- finance home/bottom sheet를 backend vault 데이터 기준으로 갱신
- 예치/출금 요청 버튼 연결
- in-flight vault transaction 상태 표시
- vault 관련 오류/세션 만료 처리

## Out of Scope
- backend 계약 재설계
- mobile 전체 디자인 개편
- 별도 vault 전용 상세 화면 신설
- contract/Foundry 환경 정비

## Affected Modules
### Backend
- 변경 없음. 기존 `/api/vault/**` 계약 재사용

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/*.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/vault/*.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`

### Docs
- 본 execplan

### Shared
- `BackendApiSupport` 기반 OkHttp 호출 패턴 재사용
- remittance polling/idempotency 패턴 참고

## Contract Changes
- backend 변경 없음
- mobile 내부 계약 추가
  - `VaultRemoteState`
  - `VaultSummaryPayload`
  - `VaultTransaction*Payload`
  - `VaultActionUiState`
- finance UI 모델에 vault action/status/feedback 필드 추가

## Security Notes
- vault API는 JWT access token 기반 호출만 허용
- mobile은 private key를 다루지 않고 backend 상태/오류만 표시
- unauthorized 응답은 기존 auth 만료 처리 경로로 통합
- vault action은 idempotency key를 client에서 생성해 요청마다 새로 보냄

## Maintainability Notes
- remittance의 repository/state/polling 패턴을 그대로 따르되 vault 전용 타입을 분리한다.
- vault 계산/표시 로직을 `FinanceHomeUiModel` 한 곳에 몰아 넣지 말고 remote state를 먼저 정규화한다.
- 기존 demo seed 기반 `VaultData`를 바로 삭제하지 말고, remote 미연동 fallback을 유지할지 명확히 결정한 뒤 최소 변경으로 정리한다.

## Implementation Steps
1. mobile vault repository와 payload/state 타입을 추가한다.
2. `DemoSessionViewModel`에 vault load/action/polling 및 auth 만료 처리를 추가한다.
3. `DemoState`와 sync 함수에 remote vault 반영 경로를 추가한다.
4. `FinanceHomeUiModel`에 vault 상태/버튼/피드백 모델을 추가한다.
5. `FinanceHomeScreen` bottom sheet에 amount selection과 deposit/withdraw action을 연결한다.
6. in-flight transaction 상태를 polling으로 갱신하고 summary 재조회로 화면을 동기화한다.
7. mobile build로 컴파일 검증한다.

## Test Plan
- `cd apps/dondone-mobile/android/app && ./gradlew assembleDebug`
- 가능하면 `./gradlew testDebugUnitTest`
- 수동 확인
  - 로그인 후 finance 탭 진입
  - vault summary 표시
  - 예치 요청 후 처리중 상태 표시
  - 확정 후 잔액/이자/버튼 문구 갱신
  - 출금 요청 후 처리중 상태 표시

## Review Focus
- backend vault 계약과 mobile DTO/상태가 일치하는지
- 세션 만료/오류 메시지 처리 일관성이 유지되는지
- in-flight status polling이 중복으로 남지 않는지
- finance 화면이 loading/empty/error/success를 명시적으로 표현하는지

## Worktree Split Decision
Single lane

`DemoSessionViewModel`, finance UI model, composable callback, remote state가 동시에 움직인다. 공유 상태와 화면 계약이 같이 변하므로 병렬 분리는 충돌 위험이 높다.

## Commit Plan
- `feat: 모바일 vault 실연동 추가`
- 필요 시 `fix: vault 화면 상태 문구 정리`

## Open Questions
- vault active 상태에서도 부분 출금을 같은 bottom sheet에서 처리할지 여부
- transaction list를 화면에 바로 노출할지, latest status만 카드/시트에 노출할지 여부

## Assumptions
- 이번 턴에서는 latest vault transaction 상태만 노출해도 충분하다.
- 예치/출금은 finance bottom sheet 한 곳에서 처리한다.
- mobile은 backend summary와 latest transaction 기준으로만 동작하며, 별도 local vault 계산은 보조/fallback으로만 남긴다.
