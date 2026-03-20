## Source Inputs
- `docs/perf/remittance-transfer-latency/02-baseline-and-measurement-plan.md`
- `docs/perf/remittance-transfer-latency/03-current-findings.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java`

## Goal
`송금 완료 확인 중` 상태가 불필요하게 오래 유지되거나 중간에 멈춘 것처럼 보이는 문제를 줄인다.

## In Scope
- remittance receipt 조회 오류를 pending과 구분하도록 backend polling 경로 보정
- mobile remittance polling이 단일 조회 오류로 조용히 종료되지 않도록 보정
- 관련 perf/change log 문서 업데이트

## Out of Scope
- vault polling 경로 수정
- remittance request path 최적화
- Grafana 대시보드 추가 변경

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/ChainReceiptLookupException.java`

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`

### Docs
- `docs/perf/remittance-transfer-latency/05-change-log.md`

### Shared
- 없음

## Contract Changes
- 외부 API 계약 변경 없음
- backend 내부 adapter 예외 표현만 명확화

## Security Notes
- auth/authz 규칙 변경 없음
- actuator, token, exposed path 변경 없음

## Maintainability Notes
- `Optional.empty()` 하나로 pending/error를 함께 표현하는 구조는 worker 해석을 흐리므로 이번 범위에서 최소한의 예외 타입으로 분리한다.
- mobile polling 종료 조건은 remittance와 vault가 유사하게 중복되어 있으므로 이번 변경은 remittance에만 국한하고, vault는 후속 분리 과제로 남긴다.

## Implementation Steps
1. Sepolia receipt 조회 IO 오류를 별도 예외로 올린다.
2. wallet funding 경로는 해당 예외를 즉시 실패로 보지 않고 기존 timeout loop 안에서 계속 재시도한다.
3. mobile remittance polling에서 일반 예외 발생 시 즉시 종료하지 않고 남은 시도 동안 계속 재시도한다.
4. mobile remittance polling 종료 시 마지막 silent refresh로 최신 상태를 한 번 더 동기화한다.
5. 변경 기록 문서를 갱신한다.

## Test Plan
- backend: `cd apps/dondone-backend && ./gradlew compileJava`
- mobile: 컴파일은 이번 턴에서 생략하고 코드 경로/상태 전이만 점검

## Review Focus
- receipt 조회 오류가 funding flow를 불필요하게 깨지 않는지
- mobile polling이 무한 loop나 중복 polling을 만들지 않는지
- terminal 도달 시 기존 UI 상태 반영이 유지되는지

## Worktree Split Decision
Single lane

backend adapter와 mobile polling이 같은 증상에 직접 연결되어 있고, 변경 규모도 작아 한 lane에서 처리하는 편이 merge risk가 낮다.

## Commit Plan
1. `fix: remittance polling terminal sync 보정`

## Open Questions
- vault polling도 동일한 silent stop 구조를 함께 보정할지 여부

## Assumptions
- 현재 사용자 증상은 remittance flow 기준이다.
- mobile에서 polling 종료 후 마지막 silent refresh는 허용 가능한 추가 호출이다.
