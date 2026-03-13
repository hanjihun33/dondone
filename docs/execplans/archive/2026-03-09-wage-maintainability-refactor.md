## Source Inputs

- Repository `AGENTS.md`
- `.agents/skills/review-checklist/SKILL.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageDeposit.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/repo/WageDepositRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageDepositResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageSummaryResponse.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

## Goal

`WageService`의 생성, 조회, 계산, DTO 조립 책임을 정리하고 `WorkProofService`와의 불필요한 결합을 줄여 유지보수성과 정확성을 개선한다.

## In Scope

- `WageDeposit` 생성 규칙을 엔티티 팩토리 메서드로 이동
- `WageDepositResponse`, `WageSummaryResponse`에 정적 팩토리 메서드 추가
- `WageService` 계산 로직을 전용 calculator로 분리
- 연월 파싱과 시간 단위 변환을 wage 내부 collaborator로 이동
- 최신 입금 조회를 repository 메서드로 이동
- anomaly trigger 0원 케이스 보정
- 관련 통합 테스트 보강 및 전체 백엔드 테스트 실행

## Out of Scope

- API 요청/응답 필드 변경
- DB 스키마 변경
- Wage 정책 자체 변경
- WorkProof/Wage 외 다른 도메인 리팩토링

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageDeposit.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/repo/WageDepositRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageDepositResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageSummaryResponse.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

### Mobile

- 변경 없음

### Docs

- 이 실행계획 문서

### Shared

- 변경 없음

## Contract Changes

- 외부 API 계약 변경 없음
- anomaly trigger 내부 계산 하한만 보정

## Security Notes

- 인증/인가 규칙 변경 없음
- 사용자 소유 입금 조회 규칙은 repository와 service에서 기존과 동일하게 유지

## Maintainability Notes

- `WageService`가 `WorkProofService`의 연월 파싱/시간 변환에 의존하면 도메인 경계가 흐려지고 후속 리팩토링 영향 범위가 넓어진다.
- 요약 계산과 DTO 조립은 서비스에서 가장 복잡한 부분이므로 calculator와 DTO 팩토리로 분리해야 한다.
- 최신 입금 선택 규칙은 조회 책임이므로 메모리 필터링보다 repository 메서드가 유지보수에 유리하다.

## Implementation Steps

1. 실행계획 문서로 리팩토링 범위를 고정한다.
2. `WageDeposit`, `WageDepositResponse`, `WageSummaryResponse`에 팩토리 메서드를 추가한다.
3. `WageSummaryCalculator`를 추가해 금액/임계치/상태 계산과 시간 변환을 이동한다.
4. repository에 최신 입금 조회 메서드를 추가하고 `WageService`를 단순화한다.
5. anomaly trigger 하한 테스트와 `asOf` 기준 최신 입금 선택 테스트를 보강한다.
6. `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`로 회귀 확인한다.

## Test Plan

- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`
- `GRADLE_USER_HOME=/tmp/gradle-dondone ./gradlew test --no-daemon`

## Review Focus

- 0원/소액 임계치에서 오탐지가 제거됐는지
- `WageService`가 `WorkProofService` 내부 helper에 덜 의존하는지
- 최신 입금 조회가 `asOf` 기준으로 정확히 동작하는지
- DTO 응답 값과 기존 테스트 기대값이 유지되는지

## Worktree Split Decision

Single lane

`wage` 서비스, repository, DTO, 테스트가 함께 바뀌고 `workproof` 지표를 같이 쓰므로 병렬 작업 시 충돌 가능성이 높다. 단일 레인으로 처리한다.

## Commit Plan

- `refactor: Wage 서비스 계산과 조회 책임 정리`
- `docs: Wage 리팩토링 실행계획 추가`

## Open Questions

- `WageSummaryResponse.status`를 향후 enum으로 바꿀지 여부는 이후 API 계약 정리 시점에 검토가 필요하다.

## Assumptions

- 현재 P0에서는 `normalizedHourlyWage`를 계속 query param으로 받는다.
- anomaly trigger는 최소 1원 이상이어야 0차액이 오탐지되지 않는다.
