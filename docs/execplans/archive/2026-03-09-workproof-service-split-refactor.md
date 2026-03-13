## Source Inputs

- Repository `AGENTS.md`
- `docs/execplans/active/2026-03-09-workproof-maintainability-refactor.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

## Goal

`WorkProofService`에 남아 있는 검증과 시간/월간 요약 계산 책임을 전용 협력 클래스로 분리해, 서비스가 조회와 오케스트레이션에 집중하도록 정리한다.

## In Scope

- `CreateWorkProofRequest` 생성 검증을 전용 validator로 분리
- WorkProof 응답 조립, 야간/연장 계산, 월간 요약 계산을 전용 calculator로 분리
- `WorkProofService`는 저장/조회/의존성 연결 중심으로 축소
- 기존 통합 테스트 재실행

## Out of Scope

- API 계약 변경
- 엔티티/DB 스키마 변경
- Wage/Demo 비즈니스 규칙 변경
- 프론트/모바일 변경

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofRequestValidator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofMetricsCalculator.java`
- 필요 시 `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

### Mobile

- 변경 없음

### Docs

- 이 실행계획 문서

### Shared

- 변경 없음

## Contract Changes

- 외부 API 계약 변경 없음

## Security Notes

- validation 오류 코드와 인증/인가 흐름은 기존과 동일하게 유지

## Maintainability Notes

- 생성 검증 로직은 수정 API가 생길 때 재사용될 가능성이 높아 서비스 private 메서드보다 명시적 validator가 낫다.
- 야간/연장/월간 요약 계산은 서비스에서 가장 복잡한 부분이라 별도 calculator로 분리해야 테스트와 변경 영향 범위를 줄일 수 있다.
- 서비스는 “무엇을 한다”를, validator/calculator는 “어떻게 판단/계산한다”를 맡는 구조가 유지보수에 유리하다.

## Implementation Steps

1. 남은 책임 분리 범위를 계획 문서로 고정한다.
2. `WorkProofRequestValidator`를 추가하고 `WorkProofService.create()`에서 사용한다.
3. `WorkProofMetricsCalculator`를 추가하고 응답 변환/월간 요약/시간 계산을 이동한다.
4. `WorkProofService`의 관련 private 메서드를 제거하고 호출 지점을 정리한다.
5. 백엔드 테스트를 재실행한다.

## Test Plan

- `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`

## Review Focus

- 서비스에서 검증/계산 책임이 충분히 빠졌는지
- validator와 calculator가 과도한 범용 util 클래스가 되지 않았는지
- 월간 요약과 개별 응답 계산 결과가 기존과 동일한지

## Worktree Split Decision

Single lane

같은 `workproof/service` 하위에서 책임을 재배치하는 작업이라 충돌 가능성이 높고, 서비스/validator/calculator 간 경계가 아직 고정 중이므로 단일 레인으로 처리한다.

## Commit Plan

- `refactor: WorkProof 검증과 계산 책임 분리`
- `docs: WorkProof 서비스 분리 실행계획 추가`

## Open Questions

- 이후 Wage 도메인도 같은 방식의 calculator 분리가 필요한지 추후 검토가 필요하다.

## Assumptions

- 현재 WorkProof 계산 규칙은 구조만 재배치하고 값은 유지해야 한다.
