# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- 관련 기존 계획:
  - `docs/execplans/active/2026-03-19-mobile-remittance-backend-connection.md`
- 코드 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/home/presentation/HomeUiModelTest.kt`

# Goal
모바일에서 송금 후 홈으로 돌아왔을 때 대표 지갑 `dUSDC` 잔액이 이전 snapshot을 유지하지 않도록 remittance 원격 상태 동기화 경로를 보정한다. 홈과 송금 화면이 같은 최신 balance source를 사용하고, 송금 최종 상태 도달 시 필요한 재조회가 누락되지 않게 한다.

# In Scope
- 송금 생성 후 / 송금 상태 polling 완료 후 remittance 원격 상태 갱신 경로 점검 및 보정
- 홈 대표 지갑 잔액이 remittance 최신 balance payload를 안정적으로 사용하도록 보강
- 필요한 범위의 unit test 추가 또는 갱신

# Out of Scope
- 백엔드 remittance API shape 변경
- 홈 카드 디자인 변경
- 송금 정책, 수신자 관리, 메뉴 영수증 UX 변경
- 실시간 push / websocket 기반 상태 갱신 도입

# Affected Modules
## Backend
- 없음. 기존 remittance load/balance 계약만 사용

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- 관련 테스트 `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/**`

## Docs
- `docs/execplans/active/2026-03-20-mobile-home-wallet-balance-refresh.md`

## Shared
- `RemittanceRemoteState` / `RemittanceRemotePayload`
- `DemoState.remittance`

# Contract Changes
- 외부 API 계약 변경 없음
- 모바일 내부 상태 계약만 보정 가능:
  - 송금 terminal 상태 반영 시 `activeTransfer` 갱신만 하지 않고 최신 `balance` 재조회 또는 동일 수준의 state refresh를 수행
  - 홈 화면은 인증 사용자의 대표 지갑 balance를 `remittanceRemoteState.payload.balance` 기준으로만 해석

# Security Notes
- 기존 `Authorization: Bearer <token>` 사용 경로 유지
- 새 토큰 저장, 민감정보 로깅, exposed path 변경 없음
- 추가 새로고침은 인증 세션이 유효한 경우에만 수행

# Maintainability Notes
- 송금 상태 polling과 remittance 전체 reload 책임을 `DemoSessionViewModel` 한 곳에 둬서, 홈 화면이 transfer lifecycle 세부 구현을 알지 않게 유지한다.
- 홈 화면은 balance formatting만 소유하고, "언제 최신 balance를 다시 읽을지" 결정 로직은 view model / state layer에 둔다.
- terminal 상태 후속 처리와 수동 refresh 경로가 분산되면 다시 stale snapshot 문제가 생기기 쉬우므로 공통 helper로 정리하는 편이 안전하다.

# Implementation Steps
1. 송금 후 홈 잔액이 stale 해지는 실제 경로를 `confirmTransfer()`, polling, `mergeRemoteTransferDetail()`, `applyRemittanceRemoteState()` 기준으로 확인한다.
2. terminal transfer 상태(`CONFIRMED` / `FAILED` / `TIMED_OUT`)에 도달했을 때 remittance 전체 state를 다시 읽도록 보강한다.
3. 필요한 경우 loading 중 기존 payload 유지 또는 후속 refresh helper 추가로 홈/송금 전환 시 이전 snapshot 재사용 구간을 줄인다.
4. 홈 대표 지갑 UI model 테스트와 view model 회귀 테스트를 추가/보강한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.feature.home.presentation.HomeUiModelTest`
- 필요 시
  - `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest --tests com.dondone.mobile.app.session.DemoSessionViewModelTest`
- 가능하면 마지막에 `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest`

# Review Focus
- 송금 terminal 상태 도달 뒤 remittance `balance`가 실제로 재동기화되는가
- 홈 대표 지갑과 송금 화면이 서로 다른 잔액 snapshot을 보지 않는가
- loading / error / unauthenticated 상태가 기존 UX를 깨지 않는가
- 추가 refresh가 무한 loop나 중복 polling을 만들지 않는가

# Worktree Split Decision
- Single lane

송금 상태 polling, remittance 원격 상태, 홈 UI model이 같은 shared state를 만진다. DTO는 고정되어 있어도 상태 전이 순서가 얽혀 있으므로 한 lane에서 추적하고 마무리하는 편이 안전하다.

# Commit Plan
- `docs: 모바일 홈 지갑 잔액 refresh 실행계획`
- `fix: 송금 후 홈 대표 지갑 balance stale 상태 수정`
- `test: remittance home balance refresh 회귀 보강`

# Open Questions
- 없음

# Assumptions
- 사용자가 "최신 값"이라고 말한 대상은 인증 사용자 홈 화면의 대표 지갑 `dUSDC` 잔액이다.
- 송금이 아직 terminal 상태가 아니면 홈은 마지막으로 확인된 balance를 보여줄 수 있고, terminal 도달 후에는 자동 재조회로 최신값을 맞춘다.
- 홈에서 별도 수동 새로고침 UX를 추가하지 않고 현재 네비게이션/상태 흐름 안에서 해결한다.
