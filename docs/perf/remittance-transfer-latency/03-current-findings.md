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

## 서브 에이전트 리뷰 통합

### Request Path
- 현재 측정값 기준 request path 는 총 11초의 주병목이 아니다.
- 다만 저비용 개선 후보는 남아 있다.
  - wallet lock 을 너무 이른 시점에 잡는다.
  - cheap rule 보다 balance / gas 조회가 먼저 수행된다.
  - `precheck()` 와 `createTransfer()` 가 evaluation 경로를 중복 호출할 수 있다.
- 즉 request path 는 `지금 당장 가장 큰 병목은 아니지만`, 다음 라운드에 정리할 가치가 있는 구간이다.

### Async Worker / Chain
- 현재 수치 기준 가장 강한 병목 후보는 이 구간이다.
- `poll-interval-ms=2000` 와 `receipt-poll-delay-seconds=2` 조합만으로도 `broadcast 이후`에 구조적 수 초 대기가 붙는다.
- `Broadcast To Terminal Avg = 6.610 s` 는 이 구조와 직접 맞물린다.
- `getReceipt()` 에서 RPC 오류와 실제 pending receipt 를 모두 `Optional.empty()` 로 다루는 구조는 provider 문제를 단순 대기처럼 보이게 만들 수 있다.
- 현재 `Job Queue Delay Avg = 260.834 ms` 는 낮은 편이지만, worker 가 만기 job 를 순차 처리하는 구조라 부하가 늘면 빠르게 민감해질 수 있다.
- demo gateway 는 receipt 지연 기준이 broadcast 시점이 아니라 prepare 시점이라, demo 기준 `broadcast -> terminal` 값이 실제보다 낙관적으로 보일 수 있다.

### Mobile / Client
- backend 병목과 별개로 체감 지연을 늘리는 구조가 있다.
- create 직후 `refreshRemittanceRemoteStateSilently()` 전체 reload 를 먼저 기다린 뒤 polling 을 시작한다.
- polling loop 는 첫 조회 전에 항상 `1500ms` delay 를 넣는다.
- terminal 상태를 detail 로 이미 받았어도 completion path 에서 다시 전체 reload 를 기다린다.
- 즉 backend 시간을 줄여도 mobile 측 waterfall 을 줄이지 않으면 체감 개선이 덜 보일 수 있다.

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

## 최근 측정으로 확인된 것
- `Transfer Create Avg` 가 수백 ms 수준이면 request path 는 총 지연의 본체가 아닐 가능성이 높다.
- `Wallet Lock Lookup Avg` 가 낮으면 row lock 대기 자체는 현재 시나리오에서 1차 병목이 아니다.
- `Requested To Terminal Avg` 가 수 초 이상 크면, 남은 병목은 worker 대기 또는 broadcast 이후 confirmation 구간에 있다.
- `Request To Broadcast Avg` 를 추가해 request 생성 이후 실제 chain broadcast 전까지의 서버 구간을 분리해서 보기로 했다.
- 이 구간을 더 좁히기 위해 `job queue delay` 와 `broadcast -> terminal` metric 을 추가했다.
- 2026-03-20 1차 측정값:
  - `Transfer Create Avg`: `323.097 ms`
  - `Policy Evaluate Avg`: `255.655 ms`
  - `Requested To Terminal Avg`: `10.390 s`
  - `Wallet Lock Lookup Avg`: `4.655 ms`
- 2026-03-20 2차 측정값:
  - `Transfer Create Avg`: `393.846 ms`
  - `Policy Evaluate Avg`: `262.704 ms`
  - `Requested To Terminal Avg`: `11.237 s`
  - `Wallet Lock Lookup Avg`: `14.598 ms`
- `Job Queue Delay Avg`: `260.834 ms`
- `Broadcast To Terminal Avg`: `6.610 s`
- 2026-03-20 3차 측정값:
  - `Transfer Create Avg`: `430.708 ms`
  - `Policy Evaluate Avg`: `280.998 ms`
  - `Requested To Terminal Avg`: `23.310 s`
  - `Request To Broadcast Avg`: `0 s (기록 버그 의심)`
  - `Wallet Lock Lookup Avg`: `3.616 ms`
  - `Job Queue Delay Avg`: `540.813 ms`
  - `Broadcast To Terminal Avg`: `21.771 s`
- 현재 수치만 보면 request path 보다 `broadcast 이후 confirmation` 구간이 더 큰 병목 후보다.
- `Job Queue Delay Avg` 는 존재하지만, 단독으로 전체 11초를 설명할 정도는 아니다.
- mobile/client 는 별도 waterfall 이 있어서 backend 개선만으로는 체감 시간이 기대만큼 줄지 않을 수 있다.
- PostgreSQL 실측으로 확인한 최신 transfer `tr_b28661dc56e34e5e` 는 다음과 같았다.
  - transfer `created_at`: `17:56:37.588`
  - `SUBMIT_TRANSFER` 완료: `17:56:39.132`
  - transfer `CONFIRMED updated_at`: `17:57:00.900`
  - 즉 `request -> broadcast` 는 약 `1.54초`, `broadcast -> terminal` 은 약 `21.77초` 였다.
- 이 실측 기준으로는 현재 병목의 대부분이 `우리 서버 내부`보다는 `Sepolia confirmation` 구간에 있다.

## 우선순위별 병목 요약

### 1순위: Async Worker / Chain
- 관련 코드:
  - [application.yml](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/resources/application.yml#L33)
  - [application.yml](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/resources/application.yml#L34)
  - [RemittanceJobWorker.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java#L45)
  - [RemittanceJobWorker.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java#L140)
  - [RemittanceJobWorker.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java#L284)
  - [SepoliaRemittanceBlockchainGateway.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java#L204)
- 왜 병목인가:
- `poll-interval-ms=2000`, `receipt-poll-delay-seconds=2` 조합만으로 `broadcast 이후` 구조적 대기가 붙는다.
- `Broadcast To Terminal Avg = 6.610 s` 가 이 구간의 비중이 크다는 근거다.
- `getReceipt()` 에서 RPC 오류와 실제 pending receipt 를 비슷하게 처리해 provider 문제도 대기 시간으로 흡수될 수 있다.
- 최신 실측에서는 `Broadcast To Terminal Avg = 21.771 s` 로 더 커졌고, 실제 transfer / job 시각 비교에서도 대부분의 시간이 이 구간에 있었다.

### 2순위: Mobile / Client
- 관련 코드:
  - [DemoSessionViewModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt#L523)
  - [DemoSessionViewModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt#L1214)
  - [DemoSessionViewModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt#L1582)
  - [DemoSessionViewModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt#L1584)
  - [BackendRemittanceRepository.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt#L26)
- 왜 병목인가:
  - create 직후 전체 remittance reload 를 먼저 기다린 뒤 polling 을 시작한다.
  - polling 시작 전 1.5초 delay 가 한 번 더 들어간다.
  - terminal 상태를 detail 로 받아도 completion path 에서 다시 전체 reload 를 기다린다.

### 3순위: Request Path
- 관련 코드:
  - [TransferService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java#L71)
  - [TransferService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java#L88)
  - [RemittancePolicyService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java#L41)
  - [RemittancePolicyService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java#L91)
- 왜 병목인가:
  - lock 시점이 이르고, cheap rule 보다 balance/gas 조회가 먼저 수행된다.
  - `precheck()` 와 `createTransfer()` 가 evaluation 경로를 중복 호출할 수 있다.
  - 다만 현재 측정값상 `Transfer Create Avg` 가 수백 ms 수준이라, 총 11초의 주원인은 아니다.
  - 최신 실측에서도 `request -> broadcast` 는 약 `1.54초` 수준이라 총 `23.31초`의 주병목은 아니다.

## 이 문서를 갱신하는 규칙
- 코드로 바로 확인되는 내용만 `코드로 확인한 사실`에 올린다.
- 측정 없이 의심되는 내용은 `현재 병목 후보`에 둔다.
- 실제 수치가 나오면 해당 항목을 `코드로 확인한 사실` 또는 baseline 문서로 옮긴다.
