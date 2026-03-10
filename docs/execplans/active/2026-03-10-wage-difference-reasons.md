## Source Inputs

- `docs/DonDone_PRD_v1.5.md` sections 7D, 13.5
- Repository `AGENTS.md`
- Existing backend `wage` contract and tests
- Existing mobile exploration for Week 2 wage difference UI needs

## Goal

`GET /api/wage/summary` 응답에 차액 사유/근거 요약 DTO를 추가해, 2주차 차액 결과 화면의 원인 카드와 근거 링크 UI가 바로 사용할 수 있는 최소 백엔드 계약을 제공한다.

## In Scope

- `wage summary` 응답에 차액 사유 DTO 추가
- 사유 코드, 제목, 설명, 관련 WorkProof ID 목록 제공
- 현재 계산 가능한 신호만 사용한 최소 룰 구성
- 기존 `wage summary` 및 `demo state` 통합 테스트 보강

## Out of Scope

- 새 엔드포인트 추가
- DB 스키마 변경
- Proof Pack/Claim Kit 문서 생성
- SafePay 정책 판단 API
- Remittance/receipt API
- 법률적/최종 급여 판정 표현

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageSummaryResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofMonthlyMetrics.java`

### Mobile

- 직접 수정 없음
- Week 2 차액 결과 화면에서 원인 카드/근거 링크에 사용할 계약 추가

### Docs

- 본 실행 계획

### Shared

- 공통 `ApiResponse` 형식 유지
- 인증/인가 규칙 변경 없음

## Contract Changes

- `GET /api/wage/summary` 응답에 `reasons` 배열 추가
- 각 reason은 최소 아래 필드를 가진다:
  - `code`
  - `title`
  - `description`
  - `relatedWorkProofIds`
- 기존 필드와 상태 코드는 유지해 하위 호환성을 최대한 보존한다

## Security Notes

- 새 정보는 모두 이미 사용자 본인 소유 데이터에서 파생된 값만 사용한다
- 타 사용자 식별자나 교차 사용자 근거를 노출하지 않는다
- 기존 JWT 보호, `SecurityConfig`, 공개 경로 allowlist는 변경하지 않는다

## Maintainability Notes

- 차액 사유 룰은 controller가 아니라 `wage` 계산 계층에서 조립한다
- `status`, `anomalyDetected`, `reasons`가 서로 다른 규칙 소유자를 갖지 않게 한 곳에서 계산한다
- 모바일용 문구를 위해 하드코딩 문자열이 늘어나더라도, 의미 있는 코드와 일관된 설명 구조를 유지한다

## Implementation Steps

1. 실행 계획을 추가하고 단일 레인 범위를 고정한다.
2. `WageSummaryResponse`에 reason DTO를 추가한다.
3. `WageSummaryCalculator`에서 최소 사유 룰과 설명 문구를 계산한다.
4. `WageService`에서 기존 summary 조립 흐름에 reasons를 연결한다.
5. `WageDemoIntegrationTest`에 reason 응답 검증을 추가한다.
6. `./gradlew test --no-daemon`으로 회귀를 확인한다.

## Test Plan

- `WageDemoIntegrationTest`
  - 입금 미기록 시 `DEPOSIT_MISSING` 노출 확인
  - 차액 임계 초과 시 `DIFFERENCE_OVER_THRESHOLD` 노출 확인
  - 연장/야간 근무가 있을 때 관련 reason 노출 확인
  - `relatedWorkProofIds`가 reflected WorkProof 범위를 벗어나지 않는지 확인
- 전체 백엔드 회귀
  - `./gradlew test --no-daemon`

## Review Focus

- 추가된 `reasons`가 기존 상태 필드와 모순되지 않는지
- 문구가 참고용/근거 중심이며 법적 판단처럼 보이지 않는지
- reason 계산이 `wage` 계층에 과도한 UI 결합을 만들지 않는지
- `demo state`를 통해 같은 계약이 일관되게 노출되는지

## Worktree Split Decision

Single lane

`wage summary` 응답 계약, 계산기, 테스트가 동시에 움직이는 작은 변경이라 분리 이점이 없고 DTO 충돌 위험만 높다.

## Commit Plan

- Commit 1: `docs: wage difference reasons 실행계획 추가`
- Commit 2: `feat: wage summary 차액 사유 DTO 추가`

## Open Questions

- 모바일이 `relatedWorkProofIds`만으로 충분한지, 아니면 차후 제목/시간까지 포함한 근거 미리보기가 필요한지
- `differenceAmount < 0`인 초과 입금 케이스에 별도 사유 코드를 둘지 여부

## Assumptions

- 2주차 첫 슬라이스는 “원인 카드/근거 링크를 여는 최소 계약”만 제공하면 충분하다
- 차액 사유는 우선 룰 기반 정적 문구로 제공하고, 고도화된 reason engine은 후속 작업으로 남긴다
- `relatedWorkProofIds`는 reflected WorkProof 기준으로만 제공한다
