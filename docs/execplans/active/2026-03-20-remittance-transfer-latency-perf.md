# Remittance Transfer Latency Perf

## Purpose
이 문서는 송금 지연 개선 작업의 진입 문서다.

실제 측정, 진단, 개선 기록은 아래 폴더에서 계속 관리한다.

- [README.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/README.md)
- [01-problem-and-goal.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/01-problem-and-goal.md)
- [02-baseline-and-measurement-plan.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/02-baseline-and-measurement-plan.md)
- [03-current-findings.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/03-current-findings.md)
- [04-improvement-strategy.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/04-improvement-strategy.md)
- [05-change-log.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/05-change-log.md)

## Goal
송금 지연을 실무형 방식으로 줄이고, before/after 결과를 포트폴리오에 그대로 쓸 수 있게 남긴다.

## Scope
- baseline 측정
- 병목 진단
- Micrometer 기반 관측 정비
- 단계별 개선
- 결과 기록

## Related Review
- [2026-03-20-remittance-create-transfer-latency-review.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/reviews/active/2026-03-20-remittance-create-transfer-latency-review.md)

## Implementation Steps
1. Micrometer + Actuator + Prometheus scrape 기준으로 baseline 관측 경로를 먼저 맞춘다.
2. 가장 큰 병목 1개를 고른다.
3. 작은 범위로 개선한다.
4. 같은 조건으로 재측정한다.
5. 결과를 perf 폴더와 review 문서에 반영한다.

## Test Plan
- `integrationTest` 기반 remittance 성능 테스트
- 외부 PostgreSQL 반복 측정 테스트
- 기존 remittance 기능 회귀 테스트

## Review Focus
- 실제 느린 구간을 수치로 설명할 수 있는가
- 가설과 확인된 사실이 분리되어 있는가
- 같은 사용자 송금 안전성이 유지되는가

## Worktree Split Decision
- `Single lane`

공통 시나리오와 공통 측정 기준이 중요하므로 첫 라운드는 단일 레인으로 진행한다.

## Commit Plan
- `docs: remittance latency workspace 정리`
- `test: remittance latency baseline harness 추가`
- `perf: remittance request path 병목 완화`
- `perf: remittance async latency 단축`
- `perf: remittance mobile polling 최적화`
- `docs: remittance latency before-after 결과 정리`
