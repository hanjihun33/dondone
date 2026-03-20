# Review Map

이 문서는 송금 지연 경로를 처음부터 다시 훑을 때 쓰는 안내서다.

핵심은 네 가지다.

- 실제로 어디까지 읽었는가
- 지금 코드상 무엇이 먼저 의심되는가
- 다음에 어떤 함수와 파일을 보면 좋은가
- 무엇이 아직 측정되지 않았는가

여기서 `실제로 읽은 파일`은 이름만 본 파일이 아니라, 이번 리뷰 라운드에서 내용을 직접 열어 확인한 파일만 뜻한다.

## 한눈에 보기

| 구간 | 이번 라운드 상태 | 지금 보이는 핵심 | 다음 초점 |
|---|---|---|---|
| Request Path | 다시 검토 완료 | `createTransfer()` 안에서 lock 범위가 넓고, fast-fail 이 늦다 | policy 내부 시간 분해와 lock hold 확인 |
| Async Worker / Chain | 다시 검토 완료 | worker cadence 와 receipt 지연만으로도 구조적 대기가 있다 | 실제 대기 분해와 RPC 호출 수 확인 |
| Mobile / Client | 다시 검토 완료 | create 직후 전체 reload 후 polling 시작이라 첫 상태 확인이 늦다 | full reload 비용과 첫 poll 시작 시점 확인 |
| Measurement | 아직 시작 전 | 현재는 코드 기반 추정이 많다 | baseline 측정과 단계별 latency 분해 |

## Request Path

### 실제로 읽은 파일
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/UserWalletRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/TransferRepository.java`

### 지금 보이는 핵심
- `createTransfer()` 가 초반에 wallet row lock 을 잡는다.
- lock 을 잡은 뒤 idempotency 확인, policy evaluation, 저장까지 이어져 lock hold 구간이 길다.
- `TRANSFER_ALREADY_IN_PROGRESS` 검사가 recipient 조회, wallet 조회, balance 조회 뒤에 와서 fast-fail 이 늦다.
- `precheck()` 와 `createTransfer()` 가 같은 policy evaluation 경로를 다시 타서 중복 비용 가능성이 있다.
- controller 는 얇고, 실제 병목 후보는 service 계층에 몰려 있다.

### 다음에 보면 좋은 함수와 파일
- `TransferService.createTransfer(...)`
- `RemittancePolicyService.evaluate(...)`
- `WalletService.getBalances(...)`
- `WalletService.estimateTransferGasCostWei(...)`
- `JobService.enqueue(...)`
- `RecipientService.getRequiredRecipient(...)`

### 아직 측정되지 않은 것
- 같은 `userId` 로 동시 요청 시 실제 lock wait 가 얼마나 생기는지
- `RemittancePolicyService.evaluate()` 에서 DB 시간과 체인 시간이 각각 얼마나 쓰이는지
- `precheck -> createTransfer` 연속 호출의 실제 중복 비용
- `saveAndFlush()` 가 lock 구간에 주는 영향

## Async Worker / Chain

### 실제로 읽은 파일
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/JobService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/Job.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/repo/JobRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/DemoRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/config/RemittanceProperties.java`
- `apps/dondone-backend/src/main/resources/application.yml`

### 지금 보이는 핵심
- worker 는 `fixedDelay=2000ms` 로 돈다.
- submit 이후 receipt poll job 을 `+2s` 뒤로 다시 잡아서 구조적으로 2초 이상 기다리기 쉽다.
- demo gateway 도 receipt 를 바로 주지 않아서 demo 모드 자체에 의도된 대기가 있다.
- worker 는 job 을 한 번에 여러 개 가져와 순차 처리하므로, 큐가 쌓이면 뒤쪽 job 은 더 늦어진다.
- sepolia 경로는 nonce, gas, receipt, known-tx 조회가 전부 외부 RPC 라서 네트워크 응답이 직접 영향을 준다.
- receipt 조회 실패를 pending 처럼 다루는 예외 경로도 실제 체감 지연을 늘릴 후보다.

### 다음에 보면 좋은 함수와 파일
- `RemittanceJobWorker.run()`
- `RemittanceJobWorker.handleSubmit(...)`
- `RemittanceJobWorker.handlePoll(...)`
- `SepoliaRemittanceBlockchainGateway.getReceipt(...)`
- `SepoliaRemittanceBlockchainGateway.isTransactionKnown(...)`
- `WalletService.fundWallet(...)`

### 아직 측정되지 않은 것
- 전체 7초 중 worker wait, receipt wait, RPC 시간이 각각 얼마나 차지하는지
- `getReceipt()` 재시도 횟수와 `isTransactionKnown()` 추가 호출 비용
- top-20 sequential processing 이 실제로 큐잉 지연을 만드는지
- wallet funding 이 포함된 시나리오에서 동기 receipt wait 영향

## Mobile / Client

### 실제로 읽은 파일
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRemoteState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/RemittanceActionUiState.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`

### 지금 보이는 핵심
- `confirmTransfer()` 는 송금 생성 직후 polling 을 바로 시작하지 않고, 먼저 전체 remittance state reload 를 수행한다.
- `BackendRemittanceRepository.load()` 는 `wallet -> balance -> recipients -> transfers -> transferDetail` 순으로 최대 5개 HTTP 요청을 직렬로 보낸다.
- polling 시작 전 `1500ms` 대기가 한 번 더 들어간다.
- `mergeRemoteTransferDetail()` 이 부분 상태 갱신을 지원하는데도, 현재 흐름은 전체 재조회 후 polling 시작으로 짜여 있다.
- remote state 를 `LOADING` 으로 바꾸는 경로가 사용자에게 로딩 플리커처럼 보일 가능성이 있다.

### 다음에 보면 좋은 함수와 파일
- `DemoSessionViewModel.applyRemittanceRemoteState(...)`
- `DemoSessionViewModel.syncRemoteRemittance(...)`
- `DemoSessionViewModel.mergeRemoteTransferDetail(...)`
- `DemoSessionReducer.confirmTransfer(...)`
- `BackendRemittanceRepository.executeForData(...)`
- `TransferScreen.kt`

### 아직 측정되지 않은 것
- `confirmTransfer()` 이후 전체 reload 가 실제로 몇 ms 걸리는지
- `load()` 내부 각 HTTP 호출의 개별 시간
- 첫 `getTransferDetail()` poll 이 시작되기까지의 실제 지연
- 한 번의 송금 플로우에서 `loadRemittanceRemoteState()` 가 총 몇 번 호출되는지

## 다음 검토 순서

처음 보는 사람이 그대로 따라가려면 이 순서가 가장 낫다.

1. `TransferService.createTransfer(...)`
2. `RemittancePolicyService.evaluate(...)`
3. `WalletService.getBalances(...)`
4. `RemittanceJobWorker.handleSubmit(...)`
5. `RemittanceJobWorker.handlePoll(...)`
6. `DemoSessionViewModel.confirmTransfer(...)`
7. `BackendRemittanceRepository.load(...)`
8. `DemoSessionViewModel.mergeRemoteTransferDetail(...)`

## 갱신 규칙

- 새로 읽은 파일만 `실제로 읽은 파일`에 추가한다.
- 코드만 보고 의심되는 내용은 `지금 보이는 핵심`에만 둔다.
- 숫자로 확인된 내용은 [03-current-findings.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/03-current-findings.md) 와 [02-baseline-and-measurement-plan.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/02-baseline-and-measurement-plan.md) 에 먼저 올린다.
