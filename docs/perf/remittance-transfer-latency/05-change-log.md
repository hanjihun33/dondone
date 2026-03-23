# Change Log

## 2026-03-20
- 송금 지연 개선 작업을 `docs/perf/remittance-transfer-latency/` 폴더로 분리했다.
- 작업 문서를 역할별로 나눴다.
  - 문제와 목표
  - baseline과 측정 계획
  - 현재까지의 원인 후보
  - 개선 전략
- 앞으로 새 원인, 측정 결과, 개선 기록은 이 폴더에 계속 누적한다.
- 서브 에이전트 리뷰를 다시 돌려서 `request path`, `async worker / chain`, `mobile / client` 세 구간을 처음부터 재검토했다.
- `06-review-map.md` 는 이번 리뷰 라운드에서 실제로 읽은 파일 기준으로 다시 정리했다.
- `03-current-findings.md` 는 레이어별 병목 후보와 아직 측정되지 않은 항목이 바로 보이게 다시 정리했다.
- backend baseline 계측 방식을 임시 로그에서 `Micrometer + Actuator + Prometheus scrape` 기준으로 전환하기 시작했다.
- remittance 전용 metric 이름과 태그 규칙을 `RemittanceMetrics` 로 모으는 방향으로 바꿨다.
- 같은 포트 scrape 를 위해 `/actuator/health`, `/actuator/prometheus` 보안 경로도 같이 손본다.
- `docker-compose.dev.yml` 에 Prometheus, Grafana 를 추가하고 로컬 backend `bootRun` 경로를 scrape 하도록 연결했다.
- `requested -> terminal` 총 시간을 보는 lifecycle metric을 추가했다.
- Grafana 대시보드에 wallet lock lookup, requested to terminal 패널을 추가했다.
- `job queue delay` 와 `broadcast -> terminal` metric 을 추가해 비동기 대기 시간을 더 좁혀 볼 수 있게 했다.
- 최신 측정값을 baseline 문서에 1차/2차 기록으로 남겼다.
- request path, async worker/chain, mobile/client 세 갈래로 서브 에이전트 리뷰를 돌려 현재 병목 후보를 다시 정리했다.
- remittance worker 기본값을 `poll-interval-ms=500`, `receipt-poll-delay-seconds=0` 으로 낮춰 1차 실험을 진행하기 쉽게 바꿨다.
## 2026-03-20 Remittance Terminal Polling Fix
- `SepoliaRemittanceBlockchainGateway.getReceipt()` 가 RPC IO 오류를 `pending receipt` 와 동일하게 `Optional.empty()` 로 내려보내던 흐름을 분리했다.
- remittance worker는 이제 receipt 조회 RPC 오류를 일반 pending으로 오해하지 않고 예외 처리 경로로 보낸다.
- wallet funding receipt wait 루프는 transient receipt lookup 오류를 허용하고 timeout 전까지 계속 재시도한다.
- mobile remittance polling은 `getTransferDetail()` 한 번 실패로 즉시 종료하지 않고 남은 시도 동안 계속 재시도한다.
- mobile remittance polling 종료 시 마지막 `refreshRemittanceRemoteStateSilently()` 를 한 번 더 수행해 tracker 상태를 최신 backend 상태와 다시 맞춘다.
- remittance poll job은 `ChainReceiptLookupException` 에 대해 더 이상 공통 실패 처리의 `+3초` backoff 로 가지 않고, 기존 `nextPollAt()` 기준으로 바로 재큐한다.
- `request -> broadcast` 시간을 분리 측정하기 위해 `dondone_remittance_transfer_request_to_broadcast_seconds` metric 과 Grafana 패널을 추가했다.
- Sepolia 실측 결과를 문서에 반영했다.
  - 최신 transfer 기준 `request -> broadcast` 약 `1.54초`
  - 최신 transfer 기준 `broadcast -> terminal` 약 `21.77초`
  - 현재 `Request To Broadcast Avg` 패널은 `updatedAt` 반영 시점 문제로 `0s`로 보일 수 있어 후속 보정이 필요하다.
