# Remittance CreateTransfer Latency Review

## Scope
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/UserWalletRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/DemoRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt`

## Findings
### 1. `createTransfer()` 가 wallet row lock 을 너무 이른 시점에 잡고, 그 상태로 외부 체인 RPC까지 수행한다
- `createTransfer()` 는 시작 직후 `walletService.getRequiredWalletForUpdate(userId)` 를 호출한다.
- 이 조회는 `UserWalletRepository.findByUserIdForUpdate(...)` 의 `PESSIMISTIC_WRITE` 로 구현되어 있다.
- 현재 로컬 PostgreSQL 컨테이너의 `default_transaction_isolation` 확인 결과는 `read committed` 였다.
- 따라서 지금 구조는 기본 `read committed` 트랜잭션 위에서 wallet row 를 `FOR UPDATE` 성격으로 잠그고, 그 락을 유지한 채 idempotency 조회, policy evaluation, transfer insert flush 까지 진행하는 흐름으로 보는 게 맞다.
- `RemittancePolicyService.evaluate(...)` 내부에서는 recipient 조회, wallet 조회, balance RPC, gas cost 추정이 이어진다.
- 특히 Sepolia 모드에서는 `getBalances()` 가 native balance RPC + token balance RPC 를 치고, `estimateTokenTransferGasCostWei()` 가 gas price RPC 를 추가로 친다.
- 결과적으로 같은 사용자의 동시 송금 요청은 wallet row lock 해제까지 직렬화되고, RPC 응답이 느릴수록 lock hold time 이 그대로 늘어난다.

### 2. 이미 활성 송금이 있는 사용자도 체인 RPC를 먼저 친 뒤에야 차단된다
- `TRANSFER_ALREADY_IN_PROGRESS` 판정은 `existsByUserIdAndStatusIn(...)` 로 빠르게 확인 가능하다.
- 하지만 현재 순서는 balance 조회와 gas 추정 이후에야 이 조건을 검사한다.
- 이미 진행 중인 송금이 있는 사용자는 어차피 차단될 요청인데도 불필요한 체인 RPC를 먼저 수행하고 있다.
- 이 순서 때문에 첫 번째 finding 의 lock 구간도 불필요하게 길어진다.

### 3. `precheck()` 와 `createTransfer()` 가 같은 정책 평가 경로를 중복 호출한다
- `TransferService.precheck(...)` 와 `TransferService.createTransfer(...)` 모두 `policyService.evaluate(...)` 를 호출한다.
- 일반적인 UI 흐름은 `precheck -> createTransfer` 연속 호출이므로, 짧은 시간 안에 같은 recipient, 같은 amount 로 balance RPC 와 gas price RPC 를 다시 치게 된다.
- correctness 관점에서 create 시점 재검증은 필요할 수 있지만, 현재 형태는 latency 와 RPC 비용이 거의 두 배에 가깝게 중복된다.

### 4. `saveAndFlush()` 도 lock 구간 안에 있다
- `createTransfer()` 는 transfer 저장 시 `saveAndFlush()` 를 사용한다.
- 이 선택은 idempotency unique 충돌을 메서드 안에서 즉시 드러내기 위한 의도로 보인다.
- 다만 flush 도 lock 이 유지되는 구간에 포함되므로, 같은 사용자 경쟁 요청이 있을 때 contention window 를 조금 더 늘린다.

### 5. 사용자가 체감하는 총 송금 시간은 `createTransfer()` 외에도 worker cadence 영향을 크게 받는다
- worker 는 `remittance.worker.poll-interval-ms=2000` 으로 돌고 있다.
- submit 완료 후 receipt poll job 을 `now + receipt-poll-delay-seconds` 로 다시 예약한다.
- demo gateway 역시 `receipt-poll-delay-seconds` 전에는 receipt 를 반환하지 않도록 되어 있다.
- 모바일은 transfer 생성 직후 전체 remittance remote state 를 다시 읽고, 이후 `1500ms` 간격 polling 으로 상세 상태를 확인한다.
- 따라서 로컬 테스트에서도 `submit job 대기 + receipt poll 예약 대기 + 모바일 재조회` 가 합쳐져 사용자가 보는 총 완료 시간이 길어질 수 있다.

## Open Questions
- 사용자당 활성 송금을 정확히 1건으로 강하게 제한해야 하는지, 아니면 중복 submit 만 피하면 되는지 정책 기준을 먼저 확정해야 한다.
- `precheck` 결과를 짧은 TTL 로 재사용할 수 있는지, 아니면 `createTransfer` 시점의 balance/gas 재검증이 반드시 필요한지 합의가 필요하다.
- 현재 느린 구간에서 더 큰 비중이 `createTransfer()` 내부 lock hold time 인지, worker cadence 인지 실측 계측이 필요하다.

## Testing Gaps
- 같은 `userId` 에 대해 `createTransfer()` 동시 호출 시 lock wait 와 총 응답시간을 검증하는 테스트가 없다.
- `precheck -> createTransfer` 연속 호출에서 체인 RPC 호출 수와 latency 를 계측하는 로그나 테스트가 없다.
- Sepolia 모드와 demo 모드를 나눠 `createTransfer` 응답시간과 최종 confirm 시간의 분해 측정을 남긴 문서가 없다.

## Residual Risks
- lock 범위만 줄여도 체감 시간이 충분히 내려가지 않을 수 있다. 현재 UX 지연은 worker poll cadence 와 모바일 재조회도 크게 기여한다.
- 반대로 lock 을 섣불리 제거하면 `existsByUserIdAndStatusIn(...)` 체크 레이스로 같은 사용자 동시 송금 생성이 열릴 수 있다.
- 현재 로컬 `.env` 는 `REMITTANCE_CHAIN_MODE=sepolia` 이므로, 로컬 테스트 결과도 실제 Sepolia RPC 상태에 강하게 영향을 받는다.

## Result
- 현재까지의 핵심 병목은 "wallet row lock 자체" 보다 "wallet row lock 을 잡은 상태로 외부 체인 RPC를 수행하는 순서" 로 정리할 수 있다.
- 그다음 우선순위는 `precheck/createTransfer` 중복 policy evaluation 과 worker cadence 다.
- 다음 분석 단계는 `RemittancePolicyService.evaluate(...)` 안에서 실제 RPC 호출 수와 순서를 더 잘게 분해하고, `createTransfer` 요청 시간을 계측 포인트와 함께 수치화하는 것이다.
