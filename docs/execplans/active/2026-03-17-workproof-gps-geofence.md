# Source Inputs
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
  - `.private/kwanwoo/README.md`
- PRD:
  - `docs/DonDone_PRD_v1.5.md` W1, W3, W7, 6.2, 6.8, 9.2
- 기존 구현:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/WorkProofController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkplaceRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofLane1IntegrationTest.java`
- 관련 UI 탐색:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- 사용자 지시:
  - 임시 근무지 이름 `SSAFY (임시)`
  - 기준 주소 `광주광역시 광산구 하남산단 6번로 107`
  - 허용 반경 `1000m`
  - 반경 안에서만 출근/퇴근 허용
- 좌표 메모:
  - 기준 좌표는 구현 시 `35.2031092`, `126.8083831`를 사용

# Goal
WorkProof lane 1에 GPS 반경 검증을 추가해, 사용자가 선택한 근무지 기준 반경 안에서만 check-in/check-out 할 수 있게 한다. 이번 단계에서는 고용주 웹 등록 기능 대신 사용자별 임시 근무지 `SSAFY (임시)`를 자동 보장한다.

# In Scope
- `Workplace`에 허용 반경 필드 추가
- 사용자별 임시 근무지 `SSAFY (임시)` 자동 보장
- `checkIn`, `checkOut` 시 근무지 좌표와 사용자 좌표 거리 계산
- 반경 초과 시 명시적 에러 반환
- workplace 조회 응답에 반경 정보 노출
- 관련 통합 테스트와 계약 문서 갱신

# Out of Scope
- 고용주 웹 등록/회사 registry
- 다중 근무지 정책 고도화
- GPS 정확도, 위변조 탐지, 백그라운드 위치 추적
- 반경 밖 요청의 `PENDING_REVIEW` 처리
- 모바일 실연동 및 mockup 수치 동기화
- 활성 계약 생성 정책 변경

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkplaceRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkplaceResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofLane1IntegrationTest.java`

## Mobile
- 직접 수정은 없지만, backend 반경 값이 `1000m`로 고정되면 이후 WorkProof map UI와 계약 동기화 지점이 생긴다.

## Docs
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/execplans/active/2026-03-17-workproof-gps-geofence.md`

## Shared
- JWT 보호 규칙은 유지
- `ApiResponse<T>` envelope 유지

# Contract Changes
- `GET /api/workproof/workplaces`
  - workplace item에 `allowedRadiusMeters` 추가
- `POST /api/workproof/records/check-in`
  - 선택한 workplace 기준 반경 밖 좌표면 `409 Conflict`
- `POST /api/workproof/records/check-out`
  - active workproof의 workplace 기준 반경 밖 좌표면 `409 Conflict`
- 에러 코드
  - 위치 반경 초과를 설명하는 신규 business error 추가

# Security Notes
- 새 공개 endpoint는 없다.
- 기존처럼 workplace ownership과 active workproof ownership 검증을 유지한다.
- GPS 좌표는 기존과 동일하게 evidence snapshot으로 저장하고, 추가로 geofence 차단 규칙에 사용한다.

# Maintainability Notes
- 반경 계산 규칙은 `WorkProofLane1Service` 안에 흩어두지 않고 작은 helper/private method로 모아야 한다.
- 임시 SSAFY 근무지 생성 로직은 이름/주소/좌표/반경 상수를 한곳에서 관리해 이후 employer registry로 치환하기 쉽게 둔다.
- 활성 계약 정책은 이번 작업에서 바꾸지 않는다. geofence 구현이 계약 생성 흐름까지 건드리면 범위가 불필요하게 커진다.

# Implementation Steps
1. `Workplace`와 `WorkplaceResponse`에 `allowedRadiusMeters`를 추가한다.
2. `WorkProofLane1Service.getWorkplaces()`에서 사용자별 임시 `SSAFY (임시)` 근무지를 없으면 생성하도록 보장한다.
3. `checkIn`, `checkOut`에 거리 계산과 반경 검증을 추가하고, 반경 밖이면 신규 에러 코드로 실패시킨다.
4. 기존 createWorkplace 흐름은 유지하되, 새 workplace 생성 시 기본 반경 값을 일관되게 부여한다.
5. `WorkProofLane1IntegrationTest`에 임시 근무지 노출, 반경 내 성공, 반경 밖 실패를 추가한다.
6. P0 API 계약 문서에 workplace 반경 및 geofence 차단 규칙을 반영한다.

# Test Plan
- `./gradlew.bat integrationTest --tests com.workproofpay.backend.workproof.WorkProofLane1IntegrationTest --console=plain`
- 가능하면 `./gradlew.bat test --console=plain`

# Review Focus
- 임시 근무지가 사용자별로 중복 생성되지 않는지
- 반경 계산이 check-in과 check-out 모두에 동일하게 적용되는지
- 반경 초과 실패가 ownership/active contract 검증 순서를 깨지 않는지
- workplace 응답 계약 변경이 기존 클라이언트를 깨지 않는지

# Worktree Split Decision
- Single lane

`Workplace` entity, lane1 service, response DTO, integration test가 한 덩어리로 움직이고 shared contract도 함께 바뀐다. 이번 변경은 병렬 분리보다 한 lane에서 일관되게 마무리하는 편이 안전하다.

# Commit Plan
- `feat: WorkProof GPS 반경 검증 추가`
- `test: WorkProof GPS 반경 검증 통합 테스트 보강`
- `docs: WorkProof GPS 반경 계약 정리`

# Open Questions
- 없음. 이번 슬라이스에서는 반경 밖 요청을 즉시 거절하는 것으로 고정한다.

# Assumptions
- 임시 SSAFY 근무지는 사용자별 기본 workplace로 자동 생성되며, 기존 사용자 생성 workplace 정책은 유지한다.
- 기준 좌표는 주소 기준 고정값 `35.2031092`, `126.8083831`를 사용한다.
- 새 반경 정책은 모든 workplace에 적용하되, radius 값은 현재 기본 `1000m`로 둔다.
- 활성 계약이 없는 경우 기존 `ACTIVE_CONTRACT_REQUIRED` 정책을 그대로 유지한다.
