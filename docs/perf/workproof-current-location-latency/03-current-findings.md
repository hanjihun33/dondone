# Current Findings

이 문서는 WorkProof 현재 위치 조회 지연 원인을 레이어별로 정리하는 문서다.

지금 단계에서는 `코드로 확인한 사실`과 `아직 측정되지 않은 병목 후보`를 분리해서 적는다.

## 코드로 확인한 사실

### Current Location Provider
- 위치 조회는 `LocationManager` 기반이며 provider 우선순위는 `NETWORK -> GPS -> PASSIVE` 다.
- 각 provider 조회 timeout은 `NETWORK 1,200ms`, `GPS 2,500ms`, `PASSIVE 300ms` 다.
- 전체 current location 조회에는 총 `4,000ms` budget을 둔다.
- current location 실패 시 `lastKnown` fallback을 사용한다.
- `lastKnown` 허용 기준은 `age <= 10분`, `accuracy <= allowedRadius (100~1000m clamp)` 다.
- coarse-only 권한이면 조회를 진행하지 않고 `PermissionRequired` 로 반환한다.

### WorkProof Action Path
- 출근/퇴근 제출 전 위치가 최근 성공(`READY`)이고 30초 이내면 재조회를 생략한다.
- 위치 조회 실패 시 출근/퇴근 API를 호출하지 않는다.
- WorkProof 지도 화면 진입 시에도 자동 위치 조회를 수행한다.
- 지도 버튼 탭 시 위치 조회를 다시 수행한다.
- 자동/수동/submit 경로는 in-flight refresh를 공유해 중복 fetch를 막는다.

## 현재 병목 후보

### 1. Provider 순차 timeout 누적
- provider를 순차로 기다리므로 최악 지연은 `3.5초 * 3 = 10.5초`까지 커질 수 있다.
- 실내/약전파 환경에서는 GPS timeout 후 NETWORK/PASSIVE로 넘어가며 체감이 크게 늦어진다.

### 2. 출퇴근 직전 강제 재조회
- 최근 위치를 이미 확보했어도 출근/퇴근 제출 시 다시 조회한다.
- submit 전 위치 freshness 보장은 좋지만 액션 체감 지연이 늘어난다.

### 3. 중복 fetch 가능성
- 화면 진입 자동 조회, 수동 새로고침, 출퇴근 전 조회가 근접 시점에 겹칠 수 있다.
- 현재 `LOADING` 가드가 있지만 launch 전후 레이스 윈도우가 남아 있다.

### 4. fallback 허용치와 정책 일관성
- P0 반경 정책(예: 1000m) 대비 `lastKnown <= 100m` 기준이 보수적일 수 있다.
- usable 좌표를 버리고 재조회로 넘어가며 대기 시간이 늘어날 가능성이 있다.

## 아직 측정되지 않은 것
- 실제 단말에서 provider별 timeout 빈도
- 성공 케이스 중 `current` vs `lastKnown` 비율
- 화면 진입/핀 이동/출퇴근 각각의 체감 지연 분포
- 중복 fetch가 실제로 발생하는 빈도와 영향

## 지금 단계의 정리
- 현재 느림의 1차 후보는 "순차 timeout + 재조회 강제" 조합이다.
- 다만 아직은 코드 기반 추정이므로 baseline 측정이 먼저 필요하다.
- 다음 단계는 액션별(진입/수동/출퇴근) 시간 분해 기록이다.

## 최근 측정으로 확인된 것 (2026-03-25)
- USB 실기기(`R3CN4062W7V`)에서 WorkProof 진입 + `현재 내 위치로 핀 이동` 5회 반복 측정
- `refreshCurrentLocation` 6회 모두 성공했지만, 6회 모두 GPS timeout 후 network provider로 성공
- 단말 위치조회 latency:
  - 전체 6회 `avg 3520.2ms` (`min 3517`, `max 3531`)
  - 수동 탭 5회 `avg 3518.0ms`
- Prometheus 서버 지표(`http_server_requests_seconds`) delta:
  - `/api/workproof/workplaces` 평균 `29.76ms`
  - `/api/workproof/contracts/current` 평균 `18.93ms`
  - `/api/workproof/records` 평균 `24.92ms`

### 해석
- 현재 체감 지연의 주원인은 서버 응답이 아니라 provider 순차 대기(특히 GPS timeout)로 확인된다.

## 최근 측정으로 확인된 것 (2026-03-26, 최신 코드 반영)
- 실기기: `R3CN4062W7V`
- 최신 `app-debug.apk` 재빌드/재설치 후 WorkProof 화면에서 `현재 내 위치로 핀 이동` 자동 5회 측정
- 로그 시그니처로 최신 코드 반영 확인:
  - `provider priority=network,gps,passive`
  - `lastKnown maxAccuracy=1000.0m`
  - `current[gps] unavailable within timeout=2500ms`
- 위치 조회 latency(시작~성공, 5회):
  - `41ms`, `43ms`, `41ms`, `2560ms`, `2849ms`
  - `avg 1107ms`, `min 41ms`, `max 2849ms`

### 해석 (2026-03-26)
- 이전 측정(`avg 3520ms`) 대비 평균 약 `68.5%` 개선.
- `NETWORK`가 즉시 usable일 때는 40ms대까지 내려오고, NETWORK usable 조건 미충족 시에만 GPS/PASSIVE 경로로 진입해 2.5~2.8초가 발생한다.

## 우선 수정 포인트 (Fix Backlog, 2026-03-25)

아래 항목은 이번 GPS 코드/보안 리뷰에서 "우선 수정 필요"로 분류된 항목이다.

### P0 (먼저 수정)
- 앱 전송 보안 정리
  - `cleartext` 허용과 기본 HTTP API URL 설정을 배포 경로에서 제거한다.
  - 위치 좌표 + JWT가 평문으로 노출되지 않게 HTTPS 경계를 강제한다.
- 지도 SDK 미사용 환경에서도 위치 갱신 경로 보장
  - `KakaoMap` unavailable 분기에서도 `onRefreshCurrentLocation()` 트리거를 제공해 실연동 출퇴근이 막히지 않게 한다.

### P1 (다음 라운드)
- 위치조회 정책 보강
  - demo/fallback 상태에서는 불필요한 실좌표 fetch를 막고, `authenticated + remote content` 상태에서만 라이브 위치를 요구하도록 경계를 명확히 한다.
  - `lastKnown` fallback은 provider 우선순위 first-hit가 아니라 정확도/age 기반 best-candidate 선택으로 보강한다.
- 로그 민감정보 축소
  - debug 로그에서 위도/경도 원문 출력 범위를 줄이거나 비식별화한다.

### P2 (정책/백엔드 계약 정리)
- 반경검증 신뢰모델 문서화
  - 클라이언트 좌표 신뢰 한계를 명시하고, 반경검증을 보안통제로 볼지 증빙보조로 볼지 정책을 확정한다.
- 위치 데이터 최소수집/보존 정책 명시
  - 저장 기간, 노출 범위, 마스킹/절삭 기준을 문서화하고 API 응답 최소공개 원칙을 적용한다.
