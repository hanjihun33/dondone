## Source Inputs
- 사용자 제보: 예치 후 홈 화면 `지금 쓸 수 있는 돈`이 즉시 갱신되지 않음
- 현재 mobile home/vault/remittance 상태 동기화 코드
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRemoteState.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/vault/VaultRemoteState.kt`
- 기존 vault 모바일 계획
  - `docs/execplans/active/2026-03-20-vault-mobile-integration.md`

## Goal
vault 예치/출금이 확정된 뒤 홈 화면의 대표 지갑 잔액이 stale 상태로 남는 문제를 수정한다. vault summary의 wallet token balance와 remittance remote balance를 일관되게 유지해 홈 화면 `지금 쓸 수 있는 돈`이 즉시 반영되게 한다.

## In Scope
- mobile vault->remittance balance 동기화 로직 추가
- vault terminal polling 뒤 remittance silent refresh 추가
- 관련 ViewModel 단위 테스트 추가

## Out of Scope
- backend API 변경
- home 화면 레이아웃 변경
- vault/remittance 도메인 구조 개편

## Affected Modules
### Backend
- 변경 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`

### Docs
- 본 execplan

### Shared
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRemoteState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/vault/VaultRemoteState.kt`

## Contract Changes
- backend/mobile 외부 계약 변경 없음
- mobile 내부 상태 동기화 규칙 추가
  - vault summary 수신 시 remittance remote balance의 token amount를 동일 값으로 보정
  - vault terminal 상태 확인 시 remittance silent refresh 수행

## Security Notes
- auth/token 처리 변경 없음
- 지갑 private key 처리 변경 없음
- 기존 unauthorized 처리 경로 유지

## Maintainability Notes
- home 화면이 직접 vault 상태를 보지 않고 remittance balance를 사용한다는 현재 구조를 유지하되, state owner를 ViewModel 한 곳에 둔다.
- vault summary를 화면마다 별도 해석해 잔액을 계산하지 않고, remote state 동기화 helper로 일관성을 만든다.

## Implementation Steps
1. vault summary 기반으로 remittance remote balance를 보정하는 helper를 추가한다.
2. `applyVaultRemoteState`에서 remittance remote balance를 즉시 동기화한다.
3. vault terminal polling 완료 후 remittance silent refresh를 추가한다.
4. 예치 확정 뒤 home balance가 업데이트되는 ViewModel 테스트를 추가한다.

## Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests 'com.dondone.mobile.app.session.DemoSessionViewModelTest'`
- Android SDK 경로가 없으면 blocker로 기록

## Review Focus
- vault 예치 확정 후 home 대표 지갑 잔액이 즉시 줄어드는지
- 출금 확정 후 home 대표 지갑 잔액이 즉시 늘어나는지
- remittance/vault state 간 소유권이 꼬이지 않는지
- 기존 remittance polling/terminal refresh 동작을 깨지 않는지

## Worktree Split Decision
Single lane

이번 변경은 `DemoSessionViewModel`의 상태 동기화 흐름과 그 테스트가 함께 움직인다. 동일 상태 소유자를 동시에 수정해야 하므로 단일 lane이 가장 안전하다.

## Commit Plan
- `fix: vault 예치 후 홈 지갑 잔액 동기화`
- 필요 시 `test: vault balance sync 회귀 테스트 추가`

## Open Questions
- 없음

## Assumptions
- 홈 화면 `지금 쓸 수 있는 돈`은 authenticated 상태에서 remittance remote balance를 기준으로 표시한다.
- 사용자 제보는 vault 확정 이후 remittance balance refresh가 누락된 데서 발생했다.
