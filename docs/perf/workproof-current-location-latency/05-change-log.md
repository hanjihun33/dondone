# Change Log

## 2026-03-26
- 출근/퇴근 submit 직전 위치 강제 재조회를 조건부로 변경했다.
  - 최근 성공 위치(`READY`)가 30초 이내면 submit 시 재조회 생략
  - stale 또는 실패 상태면 기존처럼 재조회 수행
- 자동 조회/수동 새로고침/submit 재조회가 겹칠 때 위치 fetch가 중복 실행되지 않도록 in-flight 공유 가드를 추가했다.
  - 기존 `LOADING` 상태 체크만으로 남아있던 launch 전후 레이스 윈도우를 제거
  - 동일 시점 중복 호출은 기존 fetch를 `await` 하도록 변경
- `lastKnown` 허용 정확도 기준을 정책 반경과 일치시키도록 조정했다.
  - 고정 `100m` 대신 `allowedRadiusMeters`를 기준으로 사용
  - 모바일 클라이언트에서 `100m ~ 1000m` 범위로 clamp 적용
  - 기본 반경값도 `1000m`로 상향해 초기 상태와 정책 간 불일치 축소
- 관련 코드:
  - `WorkproofCurrentLocationUiState`에 `lastResolvedAtMillis` 및 freshness 판단 함수 추가
  - `DemoSessionWorkproofHandlers.submitWorkproofAction(...)`에서 freshness 기반 분기 적용
  - `DemoSessionWorkproofHandlers.refreshWorkproofCurrentLocationInternal()`에 in-flight dedupe 추가
  - `CurrentLocationProvider.fetch(...)`가 `maxLastKnownAccuracyMeters`를 받도록 확장
  - `CurrentLocationProvider` current usable age를 provider별로 분리
    - `GPS`: `20s` 유지(엄격)
    - `NETWORK`, `PASSIVE`: `30s` 허용
- 실기기 재측정(`R3CN4062W7V`, 최신 apk 재설치 후):
  - WorkProof `현재 내 위치로 핀 이동` 5회
  - latency: `45ms`, `9ms`, `64ms`, `43ms`, `38ms`
  - summary: `avg 40ms`, `max 64ms`

## 2026-03-25
- WorkProof 현재 위치 지연 개선 기록을 `docs/perf/workproof-current-location-latency/` 폴더로 분리했다.
- 송금 성능 트랙과 문서가 섞이지 않도록 전용 `README` 와 `01~06` 문서 세트를 만들었다.
- 코드 리뷰 기반 초기 병목 후보를 정리했다.
  - provider 순차 timeout 누적
  - 출퇴근 직전 강제 재조회
  - 중복 fetch 가능성
  - fallback 기준 보수성
- baseline 수집을 위한 측정 항목과 기록 형식을 정의했다.
- USB 실기기 + Prometheus 동시 측정 1차를 수행했다.
  - WorkProof refresh 6회 모두 GPS timeout 후 network provider 성공
  - 단말 위치조회 평균 약 `3.52s`
  - 같은 구간 서버 WorkProof API 평균 처리시간은 `18~30ms` 범위
  - 지연 주원인이 backend가 아닌 단말 provider 대기로 좁혀졌다.
- GPS 기능 서브에이전트 분리 리뷰 결과를 성능 트랙에 fix backlog로 반영했다.
  - `03-current-findings.md`에 `우선 수정 포인트 (Fix Backlog)` 섹션 추가
  - P0/P1/P2 우선순위로 수정 포인트를 분리해 다음 작업 라운드 입력으로 고정
