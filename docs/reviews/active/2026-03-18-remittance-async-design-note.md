# Remittance Async Design Note

## 배경
- DonDone remittance는 외부 체인 RPC와 토큰 전송을 포함하므로, HTTP 요청 하나로 끝나는 동기 CRUD처럼 다루면 중간 실패와 재시도에서 정합성 문제가 생긴다.
- 현재 MVP는 Redis/Kafka 없이 Spring + JPA + PostgreSQL 범위에서 구현 중이므로, 운영 복잡도보다 상태 추적 가능성과 복구 가능성을 우선한다.

## 선택한 구조
- 정책과 상태의 source of truth는 백엔드 DB다.
- 실제 체인 연동은 `RemittanceBlockchainGateway` adapter 뒤로 숨긴다.
- 비동기 처리는 `jobs` 테이블 + `@Scheduled` worker로 수행한다.

## Transfer 상태머신
- `REQUESTED`
  - 송금 요청이 생성됐고 아직 signed tx 가 없음
- `SIGNED`
  - signed tx 와 tx hash 가 준비됨
  - 아직 브로드캐스트 성공은 확정되지 않음
- `BROADCASTED`
  - signed tx 브로드캐스트를 성공 처리했고 receipt polling 대상임
- `CONFIRMED`
  - receipt 성공
- `FAILED`
  - 정책 외 사유로 더 이상 진행하지 않는 terminal state
- `TIMED_OUT`
  - receipt 가 일정 시간 안에 확인되지 않아 운영 개입이 필요한 terminal state

## 왜 DB queue 인가
- 현재 요구사항은 저트래픽 Sepolia demo와 운영 가능성 증명이다.
- DB queue 는 별도 브로커 없이도 다음을 만족한다.
  - job 상태 추적
  - transfer 와 job 의 관계 디버깅
  - retry / timeout / recovery action 구현
- 정확히 한 번 처리보다는, idempotency 와 상태머신으로 중복 실행 위험을 줄이는 전략을 택했다.

## 고려한 failure mode
- wallet funding 실패 후 반쪽 지갑 고착
- signed tx 생성 후 브로드캐스트 실패
- 브로드캐스트 후 receipt 미도착
- duplicate recipient / duplicate transfer request
- 같은 사용자 동시 송금 요청

## 복구 전략
- wallet 은 `PENDING/FUNDED/FAILED` 상태를 갖고, `FAILED` 는 retry funding 이 가능하다.
- transfer 는 terminal state 에서 관리자 retry action 으로 다시 worker 경로에 태울 수 있다.
- `BROADCASTED` stuck transfer 는 receipt poll 재큐잉으로 복구한다.
- `FAILED/TIMED_OUT` transfer 는 `REQUESTED` 로 되돌린 뒤 새 submit flow 로 복구한다.

## 확장 포인트
- 멀티 인스턴스나 처리량 증가 시 `jobs` claim 로직 강화 또는 Redis/Broker 로 분리할 수 있다.
- 운영 단계에서 팀 기준이 정해지면 schema ownership 은 migration-first 로 전환 가능하다.
