# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 기존 활성 계획:
  - `docs/execplans/active/2026-03-19-mobile-remittance-backend-connection.md`
- review 결과:
  - 모바일 correctness review
  - backend correctness review
  - security review
- 현재 변경 파일:
  - `git status --short`
  - `git diff develop..HEAD`

# Goal
리뷰에서 확인된 remittance 후속 이슈를 정리한다. 이번 슬라이스는 모바일 UX/복사 정확도와 백엔드 송금 안정성 보강을 우선 해결한다.

# In Scope
- 계좌·지갑 관리 화면에서 내 지갑 long-press 복사가 전체 주소를 사용하도록 수정
- 송금 수신자 등록 제출 중 route 전체가 잠기는 현상을 완화
- partial funding 재시도 시 부족분만 보충되도록 wallet funding 로직 보강
- precheck에서 실제 gas 요구량을 반영해 부족 native balance를 차단
- signed transaction 평문 저장 제거 또는 최소화
- active submit/poll job 중복 생성 방지 보강
- 관련 모바일/백엔드 테스트 추가 및 보강

# Out of Scope
- 홈 화면 `detailText` 재노출
  - 사용자가 제거 요청했던 UI라 이번 수정에서 유지
- 회원가입 후 자동 지갑 생성/시드 지급 정책 변경
  - 데모 운영 가정으로 이번 슬라이스에서 보류
- remittance API shape 자체 변경
- 운영자 콘솔/모바일 신규 화면 추가

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/RemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Transfer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/Job.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/repo/JobRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/JobService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittanceOpsService.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/RemittanceActionUiState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/TransferBackNavigation.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/**`

## Docs
- `docs/execplans/active/2026-03-19-remittance-review-fixes.md`

## Shared
- 없음

# Contract Changes
- 외부 HTTP API shape는 유지한다.
- backend 내부 계약은 아래만 바뀐다.
  - funding/recovery는 전체 seed 기준이 아니라 자산별 부족분 계산을 사용
  - blockchain gateway는 예상 token transfer gas cost 계산을 제공
  - transfer의 signed transaction 저장값은 암호화 저장으로 전환
  - active job dedupe는 DB level key를 사용해 submit/poll 동시 enqueue를 차단
- mobile 내부 계약은 아래만 바뀐다.
  - remittance action state가 submit 종류를 구분한다
  - 계좌·지갑 관리 모델이 표시용 주소와 복사용 전체 주소를 분리한다

# Security Notes
- signed transaction은 평문 DB 저장을 피하고 기존 wallet encryption과 같은 보호 경로를 재사용한다.
- clipboard 복사는 사용자 요청 기능을 유지하되 전체 주소 정확도만 보장한다.
- 공개 faucet 성격은 이번 슬라이스에서 유지하므로 추가 운영 통제는 별도 후속 과제로 남긴다.

# Maintainability Notes
- mobile은 `isSubmitting` 하나로 모든 remittance 작업을 뭉개지 말고 제출 종류를 구분해 back 정책이 UI intent를 읽을 수 있게 한다.
- wallet address는 표시용/복사용 값을 모델에서 명시적으로 분리해 화면 로직에서 축약 주소를 재사용하다가 복사 버그가 나지 않게 한다.
- backend는 gas 계산, funding 부족분 계산, active job dedupe key를 각 소유 계층에서 한 번만 계산하게 유지한다.
- signed transaction 보호는 broad refactor 대신 기존 `WalletCryptoService` 재사용으로 제한한다.

# Implementation Steps
1. review findings 기준으로 범위와 제외 사항을 문서에 고정한다.
2. mobile account manage 모델/화면에서 복사용 전체 주소를 분리하고 테스트를 추가한다.
3. remittance action state에 작업 종류를 추가하고 recipient create 중 back 잠금이 route 전체를 막지 않게 조정한다.
4. wallet funding 로직을 자산별 부족분 계산으로 바꾸고 gateway API를 확장한다.
5. policy service에서 예상 gas 비용을 사용해 insufficient balance 판정을 보강한다.
6. signed transaction 저장/재사용 경로를 암호화 저장 방식으로 전환한다.
7. job entity/service에 active dedupe key를 추가해 submit/poll 중복 enqueue를 DB level에서 막는다.
8. 관련 backend/mobile 테스트를 보강하고 범위별 검증을 실행한다.

# Test Plan
- Mobile
  - `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Pkotlin.incremental=false :app:testDebugUnitTest --tests 'com.dondone.mobile.feature.finance.presentation.AccountManageUiModelTest' --tests 'com.dondone.mobile.app.navigation.TransferBackNavigationTest' --tests 'com.dondone.mobile.app.session.DemoSessionViewModelTest' :app:compileDebugKotlin`
- Backend
  - `cd apps/dondone-backend && ./gradlew test --tests com.workproofpay.backend.remittance.WalletServiceTest --tests com.workproofpay.backend.remittance.RemittanceJobWorkerTest --tests com.workproofpay.backend.remittance.RemittanceOpsServiceTest --tests com.workproofpay.backend.remittance.RemittanceIntegrationTest`

# Review Focus
- mobile에서 long-press 복사가 전체 주소를 보장하는가
- recipient create 중 back 정책이 송금 생성 중 정책과 분리되었는가
- funding recovery가 partial success 상태에서 과다 지급을 일으키지 않는가
- precheck가 실제 gas 부족을 미리 차단하는가
- signed transaction이 평문으로 남지 않는가
- submit/poll job이 경합 상황에서도 하나만 active 하게 유지되는가

# Worktree Split Decision
- Single lane

mobile remittance 상태와 backend remittance state machine이 함께 바뀌고, signed transaction 저장 방식과 active job schema 성격 변경이 겹친다. 공통 계약과 검증 포인트가 연결되어 있어 단일 lane이 안전하다.

# Commit Plan
- `docs: remittance review fixes 실행계획 추가`
- `fix: remittance mobile review findings 정리`
- `fix: remittance backend 안정성 보강`
- `test: remittance review regression 보강`

# Open Questions
- 없음. faucet 정책과 홈 detailText는 이번 슬라이스 제외로 고정한다.

# Assumptions
- 데모 운영 가정상 회원가입 후 자동 지갑 생성/시드 지급은 유지한다.
- mobile에서 back 차단은 transfer create submit/polling 같은 송금 핵심 단계에만 적용하고, recipient create에는 적용하지 않는다.
- signed transaction 재시도 요구는 유지되므로 비저장이 아니라 암호화 저장으로 해결한다.
