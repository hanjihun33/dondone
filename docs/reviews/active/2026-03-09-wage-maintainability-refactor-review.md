## Scope

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageDeposit.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/repo/WageDepositRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageDepositResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageSummaryResponse.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

## Findings

없음.

## Checks

- `WageService`가 `WorkProofService`의 연월 파싱/시간 변환 helper에 더 이상 의존하지 않는지 확인
- 최신 입금 선택이 repository 쿼리 메서드로 이동했는지 확인
- `anomalyTriggerAmount`가 최소 1 이상으로 보정되어 0차액 오탐지가 사라졌는지 확인
- 통합 테스트에 `asOf` 기준 최신 입금 선택과 0차액 케이스가 포함됐는지 확인

## Residual Risks

- `WageSummaryResponse.status`는 여전히 문자열이라 후속 API 계약 정리 시 enum 전환 여지가 있다.
- `normalizedHourlyWage`를 query param으로 받는 임시 계약은 유지된다.
