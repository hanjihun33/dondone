# Baseline And Measurement Plan

## 원칙
측정 없이 최적화하지 않는다.

이번 작업에서 baseline은 아래 두 가지를 먼저 확보해야 한다.

- `POST /api/remittance/transfers` 응답시간
- 송금 생성부터 `CONFIRMED` 까지의 총 시간

그리고 이 둘은 반드시 아래 조건을 함께 기록해야 한다.

- `demo` 또는 `sepolia`
- 반복 횟수
- 평균값
- 최대값
- 측정 날짜

## 측정 대상

### A. Request Path
대상:
- `POST /api/remittance/transfers`

기록할 값:
- 평균 응답시간
- 최대 응답시간
- prepared statement count
- 가능하면 단계별 시간

### B. End-To-End Time
대상:
- `REQUESTED -> CONFIRMED`

기록할 값:
- 평균 총 시간
- 최대 총 시간
- submit job 대기 시간
- receipt poll 대기 시간
- mobile polling 대기 시간

### C. RPC Cost
대상:
- balance 조회
- gas 조회
- receipt 조회

기록할 값:
- 호출 수
- 구간별 시간

## 측정 시나리오
처음 baseline은 아래 고정 시나리오로 맞춘다.

- 기존 wallet 있음
- 기존 allowlist recipient 있음
- 고정 amount 사용
- 같은 시나리오 5회 이상 반복

## 모드 분리
반드시 아래 둘을 분리해서 본다.

- `demo`
  - 구조 자체의 병목을 보기 좋다.
- `sepolia`
  - 실제 외부 RPC 영향을 포함한다.

둘을 섞어서 성과로 쓰면 안 된다.

## 현재 상태
아직 baseline 수치는 공식 기록되지 않았다.

## 지금 바로 쓰는 계측 방법

이번 라운드에서는 backend 로그 대신 Micrometer metrics를 쓴다.

- endpoint scrape: `/actuator/prometheus`
- backend 전체 요청 시간:
  - `http_server_requests_seconds`
- remittance 전용 custom metric:
  - `dondone_remittance_transfer_create_seconds`
  - `dondone_remittance_policy_evaluate_seconds`
  - `dondone_remittance_wallet_lookup_seconds`
  - `dondone_remittance_worker_job_seconds`
  - `dondone_remittance_job_queue_delay_seconds`
  - `dondone_remittance_chain_operation_seconds`
  - `dondone_remittance_transfer_lifecycle_seconds`
  - `dondone_remittance_transfer_request_to_broadcast_seconds`
  - `dondone_remittance_transfer_broadcast_to_terminal_seconds`

### 태그 기준

- `dondone_remittance_transfer_create_seconds`
  - `outcome=created|replayed|blocked|error`
  - `replayed=true|false`
- `dondone_remittance_policy_evaluate_seconds`
  - `outcome=allowed|blocked|error`
  - `policy_code`
- `dondone_remittance_wallet_lookup_seconds`
  - `lookup_mode=read|for_update`
  - `outcome=success|error`
- `dondone_remittance_worker_job_seconds`
  - `job_type=submit_transfer|poll_transfer_receipt`
  - `outcome`
- `dondone_remittance_job_queue_delay_seconds`
  - `job_type=submit_transfer|poll_transfer_receipt`
- `dondone_remittance_chain_operation_seconds`
  - `mode=demo|sepolia`
  - `operation=get_balances|estimate_transfer_gas|prepare_transfer|broadcast_signed_transaction|get_receipt|is_transaction_known|fund_wallet`
  - `outcome`
- `dondone_remittance_transfer_lifecycle_seconds`
  - `terminal_status=confirmed|failed|timed_out`
- `dondone_remittance_transfer_request_to_broadcast_seconds`
  - 태그 없음
- `dondone_remittance_transfer_broadcast_to_terminal_seconds`
  - `terminal_status=confirmed|failed|timed_out`

### 해석 기준
- `http_server_requests_seconds` 와 `dondone_remittance_transfer_create_seconds` 가 같이 크면 request path 부터 본다.
- `dondone_remittance_wallet_lookup_seconds{lookup_mode="for_update"}` 가 크면 lock wait 후보를 먼저 본다.
- `dondone_remittance_worker_job_seconds{job_type="poll_transfer_receipt"}` 가 크면 worker / receipt polling 구간 비중이 큰 것이다.
- `dondone_remittance_job_queue_delay_seconds{job_type="poll_transfer_receipt"}` 가 크면 worker cadence 또는 requeue 대기 비중이 큰 것이다.
- `dondone_remittance_chain_operation_seconds{operation="get_receipt"}` 또는 `{operation="get_balances"}` 가 크면 chain RPC 비중이 큰 것이다.
- `dondone_remittance_transfer_lifecycle_seconds` 가 크고 `dondone_remittance_transfer_create_seconds` 는 낮으면 async worker 또는 chain confirmation 구간 비중이 큰 것이다.
- `dondone_remittance_transfer_request_to_broadcast_seconds` 가 낮고 `dondone_remittance_transfer_broadcast_to_terminal_seconds` 가 크면 우리 쪽보다 chain confirmation 비중이 큰 것이다.
- `dondone_remittance_transfer_request_to_broadcast_seconds` 가 크면 submit queue, signing, broadcast 전 worker 경로를 먼저 본다.
- `dondone_remittance_transfer_broadcast_to_terminal_seconds` 가 크면 broadcast 이후 receipt confirmation 구간이 총 시간의 본체다.

## 예정 측정 명령
예상 명령은 아래와 같다.

```bash
docker compose -f docker-compose.dev.yml up -d prometheus grafana
```

```bash
cd apps/dondone-backend
./gradlew bootRun
```

```bash
curl http://localhost:8080/actuator/prometheus | rg "dondone_remittance|http_server_requests"
```

```bash
open http://localhost:9090
open http://localhost:3000
```

Grafana에서 보는 기본 경로:
- `Dashboards > DonDone > DonDone Remittance Latency`
- remittance metric은 송금 흐름을 최소 1회 실행해야 패널에 값이 보인다.

```bash
cd apps/dondone-backend
./gradlew integrationTest --tests com.workproofpay.backend.remittance.RemittanceCreateTransferPerformanceIntegrationTest
```

```bash
cd apps/dondone-backend
PERF_PG_URL=... \
PERF_PG_USERNAME=... \
PERF_PG_PASSWORD=... \
PERF_PG_ALLOW_RESET=true \
./gradlew externalDockerPerfTest --tests com.workproofpay.backend.remittance.RemittanceCreateTransferExternalPostgresPerformanceIntegrationTest
```

## 측정 결과 기록 규칙
수치가 나오면 아래 형식으로 누적한다.

### Example
- date: `2026-03-20`
- mode: `demo`
- scenario: `existing wallet + existing recipient + fixed amount`
- runs: `5`
- createTransfer avg: `TBD`
- createTransfer max: `TBD`
- requested -> confirmed avg: `TBD`
- notes: `TBD`

## 1차 측정 기록

- date: `2026-03-20`
- mode: `sepolia 추정`
- scenario: `local backend + Grafana dashboard, 동일 송금 시나리오 반복`
- runs: `여러 차례 수동 실행`
- backend uptime: `2.8 mins`
- metrics endpoint rate: `0.1 req/s`
- transfer create avg: `323.097 ms`
- policy evaluate avg: `255.655 ms`
- requested -> terminal avg: `10.390 s`
- wallet lock lookup avg: `4.655 ms`
- notes:
  - `createTransfer` request path 자체는 수백 ms 수준
  - 총 지연의 본체는 request path 가 아니라 async worker / confirmation 구간으로 보임
  - `job queue delay`, `broadcast -> terminal` metric 은 추가 후 backend 재시작 뒤 재측정 필요

## 2차 측정 기록

- date: `2026-03-20`
- mode: `sepolia 추정`
- scenario: `local backend + Grafana dashboard, 동일 송금 시나리오 반복`
- runs: `여러 차례 수동 실행`
- backend uptime: `2.6 mins`
- metrics endpoint rate: `0.2 req/s`
- transfer create avg: `393.846 ms`
- policy evaluate avg: `262.704 ms`
- requested -> terminal avg: `11.237 s`
- wallet lock lookup avg: `14.598 ms`
- job queue delay avg: `260.834 ms`
- broadcast -> terminal avg: `6.610 s`
- notes:
  - request path 는 여전히 수백 ms 수준
  - wallet lock lookup 은 총 시간 대비 작은 편
  - 총 지연의 큰 비중은 `broadcast 이후 confirmation` 구간
  - `job queue delay` 도 존재하지만 11초 전체를 설명할 정도는 아님

## 3차 측정 기록

- date: `2026-03-20`
- mode: `sepolia`
- scenario: `local backend + Grafana dashboard, 동일 송금 시나리오 반복`
- runs: `여러 차례 수동 실행`
- backend uptime: `2.0 mins`
- metrics endpoint rate: `0.2 req/s`
- transfer create avg: `430.708 ms`
- policy evaluate avg: `280.998 ms`
- requested -> terminal avg: `23.310 s`
- request -> broadcast avg: `0 s (현재 기록 버그 의심)`
- wallet lock lookup avg: `3.616 ms`
- job queue delay avg: `540.813 ms`
- broadcast -> terminal avg: `21.771 s`
- notes:
  - request path 는 여전히 주병목이 아니다.
  - 현재 수치 기준 총 시간 대부분은 `broadcast 이후 confirmation` 구간이다.
  - `Request To Broadcast Avg = 0s` 는 실제 0초가 아니라 `updatedAt` 반영 시점 문제로 인한 기록 버그로 보인다.
  - 최신 transfer 실측 기준으로는 `request -> broadcast` 약 `1.54초`, `broadcast -> terminal` 약 `21.77초` 였다.
