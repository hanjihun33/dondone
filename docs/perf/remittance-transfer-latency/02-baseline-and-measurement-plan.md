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
  - `dondone_remittance_chain_operation_seconds`

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
- `dondone_remittance_chain_operation_seconds`
  - `mode=demo|sepolia`
  - `operation=get_balances|estimate_transfer_gas|prepare_transfer|broadcast_signed_transaction|get_receipt|is_transaction_known|fund_wallet`
  - `outcome`

### 해석 기준
- `http_server_requests_seconds` 와 `dondone_remittance_transfer_create_seconds` 가 같이 크면 request path 부터 본다.
- `dondone_remittance_wallet_lookup_seconds{lookup_mode="for_update"}` 가 크면 lock wait 후보를 먼저 본다.
- `dondone_remittance_worker_job_seconds{job_type="poll_transfer_receipt"}` 가 크면 worker / receipt polling 구간 비중이 큰 것이다.
- `dondone_remittance_chain_operation_seconds{operation="get_receipt"}` 또는 `{operation="get_balances"}` 가 크면 chain RPC 비중이 큰 것이다.

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
