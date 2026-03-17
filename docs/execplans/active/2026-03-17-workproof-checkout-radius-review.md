# Source Inputs
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
  - `.private/kwanwoo/README.md`
- PRD:
  - `docs/DonDone_PRD_v1.5.md` W3, W4, W7, 6.2, 6.8
- 직전 구현:
  - `docs/execplans/active/2026-03-17-workproof-gps-geofence.md`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProofFinancialStatus.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofRequestValidator.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofLane1IntegrationTest.java`
- 리뷰 피드백:
  - 반경 밖 check-out을 hard reject하지 말고 기록은 남긴 뒤 review 대상으로 분류하는 방향 제안
- 사용자 합의:
  - 반경 밖에서 퇴근은 허용
  - 위치 플래그를 남기고 후속 설명/수정 여지를 둔다

# Goal
WorkProof lane 1에서 반경 밖 check-out을 막지 않고 저장하되, 금융 반영 대상에서는 제외하고 review 가능한 상태와 플래그로 남긴다.

# In Scope
- `checkOut`의 반경 밖 요청을 허용
- `WorkProof`에 check-out 위치 불일치 플래그 저장
- 내부 상태에 `NEEDS_REVIEW` 표현 추가
- lane 1 record detail/list 응답에 review 상태와 risk flag 노출
- lane 1 monthly summary risk flag 반영
- 관련 테스트와 API 계약 문서 갱신

# Out of Scope
- employer 콘솔 review workflow
- 반경 밖 check-in 허용 정책 변경
- GPS 정확도 보정, 위변조 탐지
- 모바일 실연동/UI 수정
- review record 전용 승인/해제 API

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProofFinancialStatus.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofRequestValidator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofRecordResponse.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofLane1IntegrationTest.java`

## Mobile
- 직접 수정은 없지만, check-out 응답의 `reflectionStatus`/risk flag 계약 변화는 WorkProof UI 상태 표현과 후속 연동 포인트가 된다.

## Docs
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/execplans/active/2026-03-17-workproof-checkout-radius-review.md`

## Shared
- JWT/auth 규칙 변화 없음

# Contract Changes
- `POST /api/workproof/records/check-out`
  - 반경 밖이어도 `200 OK`
  - 응답의 `reflectionStatus`는 `NEEDS_REVIEW`
  - 응답에 위치 불일치 risk flag 노출
- `GET /api/workproof/records/{recordId}`
  - `reflectionStatus`와 `riskFlags[]`를 명시적으로 노출
- `GET /api/workproof/records`
  - 반경 밖 check-out record는 `reflectionStatus=NEEDS_REVIEW`
- `GET /api/workproof/monthly-summary?month=...&workplaceId=...`
  - `integrity.workproofRiskFlags[]`에 반경 밖 check-out 신호 반영

# Security Notes
- 새 공개 endpoint는 없다.
- ownership/authz 규칙은 그대로 유지한다.
- 위치 불일치 record도 evidence snapshot은 그대로 저장하고, 반영 가능 여부만 상태로 구분한다.

# Maintainability Notes
- `checkIn` hard reject와 `checkOut` review 처리 분기는 service helper 수준에서 명확히 갈라야 한다.
- review 이유는 단순 상태 enum만으로 숨기지 말고 별도 플래그를 남겨 이후 employer review나 UI 설명에서 재사용 가능하게 둔다.
- lane 1 응답의 reflection 상태 계산을 한 helper로 모아 list/detail 간 drift를 피한다.

# Implementation Steps
1. `WorkProof`에 check-out outside radius 플래그와 `NEEDS_REVIEW` 상태 표현을 추가한다.
2. `checkOut`에서 반경 판정을 에러 대신 상태 전환으로 바꾸고, check-in은 기존 hard reject를 유지한다.
3. lane 1 list/detail response에 reflection status와 risk flag 노출을 맞춘다.
4. monthly summary의 aggregate risk flag에 check-out outside radius 신호를 추가한다.
5. review record가 이후 수정 가능한지 최소 정책을 맞추고 관련 validator를 조정한다.
6. 통합 테스트와 API 계약 문서를 갱신한다.

# Test Plan
- `./gradlew.bat integrationTest --tests com.workproofpay.backend.workproof.WorkProofLane1IntegrationTest --console=plain`
- 가능하면 `./gradlew.bat test --console=plain`

# Review Focus
- check-in은 계속 hard reject되고 check-out만 review 처리로 바뀌는지
- review 상태 record가 reflected 집계에 섞이지 않는지
- list/detail/action 응답이 같은 reflection semantics를 쓰는지
- risk flag 명명과 노출 위치가 후속 employer/mobile flow와 충돌하지 않는지

# Worktree Split Decision
- Single lane

`WorkProof` entity 상태, lane1 service, response DTO, contract docs, 통합 테스트가 함께 움직인다. 공통 상태 의미가 바뀌는 작업이라 한 lane에서 일관되게 정리하는 편이 안전하다.

# Commit Plan
- `feat: WorkProof 반경 밖 퇴근 review 처리`
- `docs: WorkProof checkout review 계약 정리`

# Open Questions
- 없음. 이번 슬라이스에서는 반경 밖 check-out을 `NEEDS_REVIEW`로 고정한다.

# Assumptions
- review record는 월간 reflected 집계와 finance readiness 계산에서 제외한다.
- check-in은 계속 반경 밖 hard reject를 유지한다.
- risk flag 코드는 `CHECK_OUT_OUTSIDE_RADIUS` / aggregate는 `CHECK_OUT_OUTSIDE_RADIUS_PRESENT`로 정리한다.
