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
