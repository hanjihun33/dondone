# DonDone Performance Tracks

이 디렉터리는 성능 개선 기록을 주제별로 분리해서 관리한다.

## 트랙 목록
- `remittance-transfer-latency/`
  - 송금 생성부터 terminal 상태까지의 지연 개선 기록
- `workproof-current-location-latency/`
  - WorkProof 현재 위치 조회(GPS) 지연 개선 기록

## 분리 원칙
- 서로 다른 도메인(예: 송금, 위치조회)은 같은 문서에 섞지 않는다.
- 각 도메인은 자체 `README.md` 와 `01~06` 문서 세트를 유지한다.
- 새 측정값/새 원인/새 개선안은 해당 도메인 폴더에만 누적한다.

