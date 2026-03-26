# WorkProof Current Location Latency

이 폴더는 DonDone WorkProof의 현재 위치 조회 지연 개선 작업을 계속 추적하는 작업 공간이다.

처음 보는 사람은 아래 순서로 읽으면 된다.

1. `01-problem-and-goal.md`
2. `02-baseline-and-measurement-plan.md`
3. `03-current-findings.md`
4. `04-improvement-strategy.md`
5. `05-change-log.md`
6. `06-review-map.md`

## 이 폴더를 왜 따로 두는가
- 이 트랙은 송금 성능과 원인/측정 대상이 다르다.
- 위치 권한, 단말 센서 상태, provider 응답 지연 같은 모바일 조건이 핵심이다.
- `remittance-transfer-latency` 문서와 섞으면 원인 추적과 before/after 설명이 어려워진다.

## 운영 원칙
- 새 사실을 발견하면 먼저 `03-current-findings.md` 에 추가한다.
- 실제 수치를 얻으면 `02-baseline-and-measurement-plan.md` 를 갱신한다.
- 개선 방향이 바뀌면 `04-improvement-strategy.md` 를 갱신한다.
- 실제 수정/측정 실행 내역은 날짜와 함께 `05-change-log.md` 에 남긴다.

## 관련 문서
- 실행계획 진입 문서:
  - [2026-03-25-mobile-workproof-current-location.md](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/docs/execplans/active/2026-03-25-mobile-workproof-current-location.md)
- GPS/geofence 관련 선행 계획:
  - [2026-03-17-workproof-gps-geofence.md](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/docs/execplans/active/2026-03-17-workproof-gps-geofence.md)

