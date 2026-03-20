# Current Findings

이 문서는 송금 지연 원인을 레이어별로 정리하는 문서다.

지금 단계에서는 `코드로 확인한 사실`과 `아직 측정되지 않은 병목 후보`를 분리해서 적는다.

## 코드로 확인한 사실

### 공통
- 로컬 PostgreSQL의 `default_transaction_isolation` 은 `read committed`
- 로컬 `.env` 기준 remittance chain mode 는 `sepolia`

### Request Path
- `createTransfer()` 는 wallet row lock 조회 후 idempotency 확인과 policy evaluation 으로 들어간다.
- `RemittancePolicyService.evaluate()` 내부에 recipient 조회, wallet 조회, active transfer 체크, 체인 balance 조회, gas 추정 경로가 있다.
- `precheck()` 와 `createTransfer()` 가 같은 evaluation 경로를 다시 탈 수 있다.

### Async Worker / Chain
- worker poll interval 기본값은 `2000ms`
- receipt poll delay 기본값은 `2s`
- demo gateway 도 receipt 를 지연해서 돌려준다.
- sepolia 경로는 nonce, gas, receipt, known-tx 조회가 외부 RPC 기반이다.

### Mobile / Client
- 모바일 polling delay 는 `1500ms`
- create 이후 mobile 은 먼저 전체 remittance state 를 다시 읽고, 그 다음에 transfer detail polling 을 시작한다.
- `BackendRemittanceRepository.load()` 는 여러 API 호출을 순차로 수행한다.

## 현재 병목 후보

### 1. Request Path
- `createTransfer()` 가 wallet row lock 을 너무 이른 시점에 잡는다.
- lock 을 잡은 상태로 idempotency 확인, policy evaluation, DB flush 까지 이어져 lock hold 구간이 길다.
- active transfer fast-fail 검사가 balance / gas 관련 경로보다 늦다.
- `precheck -> createTransfer` 에서 policy evaluation 비용이 중복될 가능성이 있다.
- idempotency replay 판단도 lock 이후에 일어나 lock 범위를 좁히지 못하고 있다.

### 2. Async Worker / Chain
- worker cadence 와 receipt poll delay 만으로도 `REQUESTED -> CONFIRMED` 사이에 구조적 대기가 생긴다.
- demo 모드도 receipt 가 바로 보이지 않도록 만들어져 있어, 로컬 테스트여도 즉시 완료처럼 보이지 않는다.
- worker 가 잡은 job 을 순차 처리하므로 큐가 쌓일수록 뒤쪽 transfer 가 늦어질 수 있다.
- receipt 조회 실패를 `Optional.empty()` 로 처리하는 예외 경로는 실제 장애와 아직 mined 되지 않은 상태를 비슷하게 보이게 만들어 pending 시간이 길어 보일 수 있다.
- wallet funding 이 시나리오에 포함되면 동기 receipt wait 이 총 시간에 추가될 수 있다.

### 3. Mobile / Client
- create 직후 전체 remittance reload 를 먼저 수행해 첫 상태 확인이 늦다.
- `BackendRemittanceRepository.load()` 의 순차 waterfall 이 client 측 체감 지연을 늘릴 수 있다.
- polling 시작 전 `1500ms` 대기가 한 번 더 붙는다.
- remote state 를 `LOADING` 으로 바꾸는 흐름은 단순 백그라운드 fetch 가 아니라 사용자 체감 지연과 플리커를 만들 가능성이 있다.

## 아직 측정되지 않은 것
- 사용자가 말한 `7초`가 평균인지 최대값인지
- request path 와 worker path 중 어느 쪽이 더 큰 비중인지
- `demo` 와 `sepolia` 에서 병목 구조가 얼마나 다른지
- 같은 `userId` 동시 요청에서 실제 lock wait 와 lock hold 시간이 얼마나 되는지
- `RemittancePolicyService.evaluate()` 에서 DB 시간과 chain RPC 시간이 각각 얼마나 되는지
- submit 이후 worker wait, receipt wait, mobile reload, first poll start 가 각각 몇 ms 인지
- `BackendRemittanceRepository.load()` 한 번에 실제 HTTP 몇 건과 각 호출 시간이 얼마인지

## 지금 단계의 정리
- 현재는 `request path`, `async worker / chain`, `mobile / client` 세 레이어 모두에서 지연 후보가 보인다.
- 다만 아직은 코드 기준 추정이 많고, 어떤 레이어가 가장 큰 원인인지는 측정으로 확인되지 않았다.
- 다음 단계는 레이어별로 시간을 쪼개는 baseline 측정이다.

## 이 문서를 갱신하는 규칙
- 코드로 바로 확인되는 내용만 `코드로 확인한 사실`에 올린다.
- 측정 없이 의심되는 내용은 `현재 병목 후보`에 둔다.
- 실제 수치가 나오면 해당 항목을 `코드로 확인한 사실` 또는 baseline 문서로 옮긴다.
