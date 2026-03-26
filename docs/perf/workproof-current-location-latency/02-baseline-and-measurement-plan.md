# Baseline And Measurement Plan

## 원칙
측정 없이 최적화하지 않는다.

이번 트랙의 baseline은 아래 두 가지를 먼저 확보해야 한다.

- `refreshWorkproofCurrentLocation()` 1회 완료 시간
- 출근/퇴근 버튼 탭부터 위치확인 완료까지의 대기 시간

그리고 이 둘은 반드시 아래 조건을 함께 기록한다.

- 단말 모델 / OS 버전
- 위치 서비스 상태(GPS on/off, 실내/실외)
- 반복 횟수
- 평균값 / 최대값
- 측정 날짜

## 측정 대상

### A. Current Location Fetch Path
대상:
- `CurrentLocationProvider.fetch()`

기록할 값:
- 평균 fetch 시간
- 최대 fetch 시간
- 성공률
- 결과 상태 분포(`READY`, `PERMISSION_REQUIRED`, `LOCATION_DISABLED`, `ERROR`)

### B. Provider Segment
대상:
- `GPS -> NETWORK -> PASSIVE` 순차 조회 구간

기록할 값:
- 어떤 provider에서 성공했는지
- provider별 timeout 발생 빈도
- `lastKnown` fallback 사용 비율

### C. User Action Wait
대상:
- 화면 진입 자동 조회
- "현재 내 위치로 핀 이동" 탭
- 출근/퇴근 버튼 탭

기록할 값:
- 사용자 액션부터 상태 `READY`까지 시간
- 액션 실패율
- 중복 fetch 발생 건수

## 측정 시나리오
처음 baseline은 아래 고정 시나리오로 맞춘다.

- 시나리오 1: WorkProof 화면 최초 진입(자동 조회)
- 시나리오 2: 화면 진입 후 "현재 내 위치로 핀 이동" 5회 반복
- 시나리오 3: 인증 사용자 기준 `clockIn` 실행 전후
- 시나리오 4: 실내/실외 각각 5회 이상 반복

## 계측 방법

### 1) Logcat 기반(즉시 가능)
`CurrentLocationProvider` 와 `WorkproofHandlers` debug log를 사용한다.

```bash
adb logcat -v time | rg "CurrentLocationProvider|WorkproofHandlers"
```

확인 포인트:
- `refreshCurrentLocation: started`
- `current[provider] ...`
- `lastKnown[provider] ...`
- `refreshCurrentLocation: success ...`
- `refreshCurrentLocation: error ...`

### 2) 보강 계측(권장)
다음 값을 로그/metric으로 추가해 구간 시간을 명확히 남긴다.

- `workproof_location_fetch_total_ms`
- `workproof_location_provider_attempt_ms{provider}`
- `workproof_location_provider_timeout_count{provider}`
- `workproof_location_last_known_used_count`

## 현재 상태
- 정식 baseline 수치는 아직 기록되지 않았다.
- 현재는 코드 리뷰 기반 병목 가설만 정리된 상태다.

## 측정 결과 기록 규칙
수치가 나오면 아래 형식으로 누적한다.

### Example
- date: `2026-03-25`
- device: `TBD`
- scenario: `WorkProof 진입 자동 조회`
- runs: `5`
- fetch avg: `TBD`
- fetch max: `TBD`
- success rate: `TBD`
- lastKnown usage: `TBD`
- notes: `TBD`

## 1차 측정 기록 (USB + Prometheus)
- date: `2026-03-25`
- device: `R3CN4062W7V (실기기, Android 13 계열)`
- scenario:
  - WorkProof 화면 진입(자동 조회 1회)
  - `현재 내 위치로 핀 이동` 5회 탭
- runs: `총 6회 refresh (자동 1 + 수동 5)`

### 단말 위치조회(logcat) 결과
- `refreshCurrentLocation` 성공/실패: `6 성공 / 0 실패`
- GPS timeout 발생: `6/6`
- network provider 성공: `6/6`
- 전체 refresh latency(ms): `avg 3520.2`, `min 3517`, `max 3531`
- 수동 탭 5회 latency(ms): `avg 3518.0`, `min 3517`, `max 3520`

### 서버 처리시간(Prometheus counter delta) 결과
- 측정 구간: 동일 시나리오 실행 전/후 `http_server_requests_seconds_{count,sum}` 비교
- `/api/workproof/workplaces`: `3건`, 평균 `29.76ms`
- `/api/workproof/contracts/current`: `1건`, 평균 `18.93ms`
- `/api/workproof/records`: `1건`, 평균 `24.92ms`

### notes
- 이번 구간에서는 서버 처리시간(<30ms)보다 단말 provider 대기(약 3.5초)가 지연의 대부분을 차지했다.
