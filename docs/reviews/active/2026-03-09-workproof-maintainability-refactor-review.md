## Scope

- `WorkProofService` 유지보수성 리팩토링
- 생성, 판정, DTO 조립 책임 일부를 `workproof` 도메인 객체와 응답 DTO로 이동

## What Changed

- `WorkProof`에 `record(...)`, `isReflected()`, `isEdited()`, `workedMinutes()` 추가
- `WorkProofResponse.from(...)`, `WorkProofMonthlySummaryResponse.from(...)`, `WorkProofMonthlyMetrics.empty(...)` 추가
- `WorkProofService`를 생성, 조회 필터링, 월간 요약 계산 단계로 분리
- overtime/night/hour 계산의 매직 넘버를 상수로 정리
- `WorkProofController`, `DemoStateService`에서 요약 DTO 팩토리를 사용하도록 정리

## Verification

- 실행 명령: `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`
- 결과: 성공

## Findings

- API 계약과 테스트 기대값은 유지됐다.
- 서비스 메서드에서 엔티티 생성 규칙과 응답 조립 책임이 줄어들어 후속 수정 범위가 좁아졌다.

## Residual Risks

- `validateRequest()`는 여전히 서비스에 남아 있어, 향후 수정 API가 생기면 전용 validator 분리 여부를 다시 판단해야 한다.
- overtime/night 계산은 아직 서비스 책임이므로 급여 정책이 복잡해지면 별도 계산기로 분리할 필요가 있다.
