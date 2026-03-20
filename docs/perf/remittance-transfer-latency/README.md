# Remittance Transfer Latency

이 폴더는 DonDone 송금 지연 개선 작업을 계속 추적하는 작업 공간이다.

처음 보는 사람은 아래 순서로 읽으면 된다.

1. `01-problem-and-goal.md`
2. `02-baseline-and-measurement-plan.md`
3. `03-current-findings.md`
4. `04-improvement-strategy.md`
5. `05-change-log.md`
6. `06-review-map.md`

## 이 폴더를 왜 따로 두는가
- 이번 작업은 단순 구현 계획이 아니라 성능 개선 캠페인에 가깝다.
- 새 원인, 새 측정 결과, 새 개선안이 계속 추가될 가능성이 크다.
- `execplans` 와 `reviews` 에 모든 내용을 몰아넣으면 처음 보는 사람이 따라가기 어렵다.

## 운영 원칙
- 새 사실을 발견하면 먼저 `03-current-findings.md` 에 추가한다.
- 실제 수치를 얻으면 `02-baseline-and-measurement-plan.md` 를 갱신한다.
- 개선 방향이 바뀌면 `04-improvement-strategy.md` 를 갱신한다.
- 실제로 수정하거나 측정한 내용은 날짜와 함께 `05-change-log.md` 에 남긴다.

## 관련 문서
- 실행계획 진입 문서:
  - [2026-03-20-remittance-transfer-latency-perf.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/execplans/active/2026-03-20-remittance-transfer-latency-perf.md)
- 현재까지의 병목 리뷰:
  - [2026-03-20-remittance-create-transfer-latency-review.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/reviews/active/2026-03-20-remittance-create-transfer-latency-review.md)
