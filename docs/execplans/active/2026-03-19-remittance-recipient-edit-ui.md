# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 기존 활성 계획:
  - `docs/execplans/active/2026-03-19-mobile-remittance-backend-connection.md`
- PRD / 계약:
  - `docs/DonDone_PRD_v1.5.md` 7E, 12.5
  - `docs/DonDone_P0_API_Contract_v0.md` 7.2, 7.3, 7.4
- 현재 모바일 구조:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/RemittanceActionUiState.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt`
- 현재 백엔드 계약:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RecipientService.java`

# Goal
기존 안드로이드 계정 관리 디자인을 유지하면서 수신 지갑 수정 기능을 추가한다. 사용자는 수신 지갑 row를 눌러 별칭, 관계, 지갑 주소를 수정하고 저장할 수 있어야 하며, 로그인 여부에 따라 demo local state 또는 백엔드 `PUT /api/remittance/recipients/{recipientId}` 와 연결되어야 한다.

# In Scope
- 계정 관리 화면에서 수신 지갑 편집 진입점 추가
- 기존 추가 sheet 톤을 유지한 수신 지갑 수정 bottom sheet 추가
- 모바일 remittance repository에 recipient update 호출 추가
- `DemoSessionViewModel` 에 수신 지갑 수정 액션 추가
- 비로그인 demo 상태의 로컬 recipient 수정 reducer 추가
- 성공/실패/loading 상태를 기존 `RemittanceActionUiState` 로 명시적으로 구분
- 필요한 모바일 unit test 보강

# Out of Scope
- 삭제 또는 `allowed=false` 비활성화 UX
- 백엔드 remittance API shape 변경
- 관리자 remittance ops API
- 송금 화면 내 quick edit 진입점
- 회원 검색 기반 recipient rebind 전용 UX

# Affected Modules
## Backend
- 구현 변경 없음
- 기존 `PUT /api/remittance/recipients/{recipientId}` 계약 재사용

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/RemittanceActionUiState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRepository.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/**`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/finance/presentation/**`

## Docs
- `docs/execplans/active/2026-03-19-remittance-recipient-edit-ui.md`

## Shared
- 기존 `RemittanceRecipientPayload` 와 `Recipient` 모델의 관계/주소 필드 재사용

# Contract Changes
- 백엔드 외부 계약 변경 없음
- 모바일 내부 계약 변경:
  - `RemittanceRepository` 에 `updateRecipient(...)` 추가
  - `RemittanceSubmittingAction` 에 recipient update 구분값 추가
  - `RecipientWalletUiModel` 에 수정 sheet 초기값용 관계 코드/표시값 추가

# Security Notes
- 수정 호출은 기존 로그인 세션의 Bearer 토큰을 그대로 사용한다.
- 로그아웃/세션 만료 시 기존 remittance 경로처럼 즉시 세션을 만료 처리한다.
- 지갑 주소는 테스트넷 공개 정보만 노출하며 access token 은 UI/로그에 노출하지 않는다.
- 로그인 전 수정은 demo seed/local state 한정이며 서버 호출은 하지 않는다.

# Maintainability Notes
- 추가와 수정이 같은 시각 언어를 사용하더라도 입력/제출 로직은 mode 분기로 분명히 나눠 sheet 상태가 꼬이지 않게 한다.
- `AccountManageScreen` 에서 필드 검증과 액션 상태 분기를 최소 helper로 묶어, add/edit sheet가 거의 같은 UI라도 한 composable에 과도한 조건문이 쌓이지 않게 한다.
- relation label/code 매핑은 한 곳에서 소유해 reducer, UI model, sheet 초기값이 서로 다른 문자열을 쓰지 않게 한다.
- remote/local 수정 흐름은 `DemoSessionViewModel` 이 소유하고 composable은 결과 상태만 소비하게 유지한다.

# Implementation Steps
1. recipient edit UX 범위를 `계정 관리 row 탭 -> edit bottom sheet -> 저장` 으로 고정한다.
2. `RecipientWalletUiModel` 을 수정 sheet 초기값을 담을 수 있게 확장한다.
3. `AccountManageScreen` 에 add/edit sheet 상태를 분리하고, recipient row tap 시 edit sheet 를 연다.
4. `RemittanceRepository` / `BackendRemittanceRepository` 에 update recipient 호출을 추가한다.
5. `RemittanceActionUiState` 에 recipient update 제출 종류를 추가한다.
6. `DemoSessionReducer` 와 `DemoSessionViewModel` 에 local/remote recipient update 액션을 추가한다.
7. `DonDoneNavGraph` 에 새 콜백을 연결한다.
8. 관련 unit test 를 보강하고 compile/test 검증을 실행한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Pkotlin.incremental=false :app:testDebugUnitTest --tests 'com.dondone.mobile.feature.finance.presentation.AccountManageUiModelTest' --tests 'com.dondone.mobile.app.session.DemoSessionReducerTest' --tests 'com.dondone.mobile.app.session.DemoSessionViewModelTest' :app:compileDebugKotlin`

# Review Focus
- row 탭이 선택/복사 UX와 충돌하지 않는가
- add sheet 와 edit sheet 의 상태가 서로 섞이지 않는가
- 로그인 상태에 따라 local/remote 수정 경로가 올바르게 갈리는가
- 수정 성공 후 최신 recipient 값이 UI와 선택 상태에 반영되는가
- 기존 송금/수신자 추가 플로우가 회귀하지 않는가

# Worktree Split Decision
- Single lane

수신자 편집 UI, ViewModel 액션, repository 계약, 테스트가 같은 화면 흐름 안에서 동시에 바뀐다. 상태 관리와 sheet 상호작용이 긴밀하게 엮여 있어 단일 lane 이 안전하다.

# Commit Plan
- `docs: recipient edit UI 실행계획 추가`
- `feat: account manage recipient edit 연결`
- `test: recipient edit 상태 검증`

# Open Questions
- 없음. 이번 슬라이스는 수신자 수정만 다루고 삭제/비활성화는 후속 과제로 남긴다.

# Assumptions
- 수신 지갑 수정은 현재 모바일 UX 범위에서 주소 직접 수정까지 허용한다.
- 수정 후에도 recipient 는 allowlist 활성 상태(`allowed=true`)를 유지한다.
- 전화번호 검색 기반으로 생성한 recipient 도 이번 슬라이스에서는 동일한 edit sheet 를 사용하며, 별도 재검색 UX 는 제공하지 않는다.
