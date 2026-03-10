## Scope

- `WorkProofService`에 남아 있던 검증과 시간/월간 요약 계산 책임 분리

## What Changed

- `WorkProofRequestValidator`를 추가해 생성 검증 로직을 서비스에서 분리
- `WorkProofMetricsCalculator`를 추가해 개별 응답 변환, 연장/야간 시간 계산, 월간 요약 계산을 서비스에서 분리
- `WorkProofService`는 조회, 사용자 로딩, 가시성 필터링, 저장/오케스트레이션 중심으로 정리

## Verification

- 실행 명령: `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`
- 결과: 성공

## Findings

- WorkProof 관련 계산과 validation 변경 지점이 전용 클래스로 모여 후속 수정 범위가 줄었다.
- `WageService`는 기존처럼 `WorkProofService.minutesToHours(...)`를 사용하되, 실제 계산 구현은 calculator로 위임된다.

## Residual Risks

- `WorkProofService`가 여전히 `findUser`, `parseYearMonth`, 조회 필터링까지 맡고 있어 이후 조회 전략이 더 복잡해지면 loader/query 계층 분리도 검토할 수 있다.
- validator는 현재 create 전용이라 update 흐름이 생기면 재사용 구조를 다시 점검해야 한다.
