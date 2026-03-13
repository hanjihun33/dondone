# Source Inputs
- Root guidance: `AGENTS.md`
- Shared product docs:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
  - `docs/DonDone_P0_Functional_Spec_v0.md`
- Current private context:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/decisions/DECISIONS.md`
- Backend current code:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/demo/**`

# Goal
현재 PRD baseline 기준으로 backend 구현 착수를 시작할 수 있게 `WorkProof -> Wage -> Advance` 순서의 DTO / validation / error code / endpoint 전환 범위를 정리한다. 추가 PRD는 확장으로 흡수하되, 지금은 변경에 강한 upstream 축과 공통 규칙부터 고정한다.

# In Scope
- `workproof` 현재 코드와 공유 API 계약 간 차이 정리
- `wage` 현재 코드와 공유 API 계약 간 차이 정리
- `advance` 신규 모듈 착수 범위 정의
- 공통 backend 규칙 정리:
  - `ApiResponse<T>` envelope 유지
  - Bean Validation 적용 방향
  - `404` 은닉 / auth / 멱등 규칙 적용 지점
  - demo mode / `X-Demo-AsOf` 연계 지점 식별
- implementation-first backlog 우선순위:
  - WorkProof
  - Wage
  - Advance

# Out of Scope
- Home / Copilot / Demo Time Travel 세부 DTO 고정
- Documents / Claim / Remittance / SafePay / Vault 구현 착수
- 실제 코드 리팩터링 또는 엔티티 변경 전체 수행
- Swagger 문서 생성
- P1 범위 전체

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`
- 신규 예정: `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/**`

## Docs
- `docs/execplans/active/2026-03-13-workproof-wage-advance-backend-start.md`
- 필요 시 후속 설계 메모 또는 backend 착수 문서

## Private Context
- `.private/kwanwoo/context/CURRENT.md`
- `.private/kwanwoo/context/NEXT.md`
- `.private/kwanwoo/decisions/DECISIONS.md`
- `.private/kwanwoo/logs/2026-03-13.md`

# Requirement Clarification
- expected behavior:
  - 현재 PRD/API 문서 기준으로 바로 backend 착수를 시작할 수 있는 최소 범위와 순서가 필요하다.
- exact scope:
  - WorkProof/Wage는 현재 코드와 공유 문서 차이를 메우는 방향으로 정리하고, Advance는 신규 모듈 착수 범위를 정의한다.
- contract changes:
  - 문서상으로는 새로운 구현 단계를 위한 DTO/validation/error code 초안 범위를 정리한다. 실제 코드 반영은 후속 커밋에서 수행한다.
- security impact:
  - 공개 경로는 auth/health/swagger만 유지한다. 나머지는 JWT 보호, 타인 리소스는 `404` 은닉 원칙을 유지한다.
- non-functional impact:
  - WorkProof/Wage는 `asOf` 재현성과 월간 계산 일관성을 유지해야 하고, Advance는 멱등성과 explanation-first 흐름을 우선한다.

# Current Code Snapshot
## WorkProof
- 현재 `WorkProofController`는 `/api/workproof` 단일 CRUD + `/monthly-summary` 중심이다.
- 현재 `CreateWorkProofRequest`는 `workDate`, `clockInAt`, `clockOutAt`, 좌표, memo, editReason, attachmentCount` 중심이라 공유 계약의 `workplaces/contracts/check-in/check-out/missing/modifications` 구조와 다르다.
- 현재 `WorkProofMonthlySummaryResponse`는 reflected/pending 중심 요약이 있고, 일부 집계 재사용은 가능하지만 `integrity.recorded/reflected/verified/pending/riskFlags` shape는 추가 정리가 필요하다.

## Wage
- 현재 `WageController`는 `POST /api/wage/deposits`, `GET /api/wage/summary` 구조다.
- 현재 summary 조회는 `normalizedHourlyWage`, `paydayDay`를 query parameter로 직접 받아 공유 계약의 `workplaceId + 계약/근무 요약 기반 계산` 구조와 다르다.
- 현재 summary DTO에는 anomaly reason과 relatedWorkProofIds가 있어 재사용 가치가 있지만, 계약서의 `monthly-summary / estimate / verifications` 분리 구조와 다시 맞춰야 한다.

## Advance
- 현재 backend에 `advance` 패키지가 없다.
- 신규 모듈을 열 때는 shared 규칙, auth, WorkProof/Wage 결과 재사용 구조를 먼저 정해야 한다.

## Shared / Demo
- `ErrorCode`는 현재 workproof/wage/demo 중심 최소 코드만 있고, 도메인별 세분화가 부족하다.
- `SecurityConfig`는 auth/health/swagger만 공개 경로로 유지하고 있다.
- `demo`는 현재 `GET /api/demo/state`만 있고 `seed/reset/X-Demo-AsOf` 전체 규칙과는 차이가 있다.

# Security Notes
- `SecurityConfig` 공개 경로는 유지하고, WorkProof/Wage/Advance 신규 endpoint는 모두 보호 대상으로 둔다.
- 타인 리소스 접근은 `404` 은닉 원칙을 유지한다.
- Advance 생성 계열과 후속 문서 생성/송금 계열에는 멱등성 헤더를 강제할 수 있는 공통 처리 방향을 고려한다.
- demo mode는 일반 계정과 분리된 resolver/header 처리로 격리한다.

# Maintainability Notes
- feature-first 구조(`api/service/repo/model/adapter`)를 유지한다.
- WorkProof는 현재 단일 CRUD형 DTO를 shared 문서 계약에 맞는 use-case 단위 DTO로 분해해야 한다.
- Wage는 현재 summary/deposit 구조를 `monthly-summary / estimate / verifications` 관점으로 재배치해야 한다.
- Advance는 새로 열 때도 controller logic보다 explanation-first service contract부터 정의한다.
- 공통 error code와 validation 규칙은 도메인별로 흩어지지 않게 shared exception 규칙과 함께 정리한다.

# Implementation Steps
1. WorkProof 현재 endpoint/DTO와 공유 계약 차이를 표로 정리한다.
2. WorkProof 1차 구현 대상(use-case 단위)을 확정한다:
   - workplaces
   - contracts
   - records check-in/check-out
   - monthly-summary
   - missing/modifications는 후속 또는 같은 lane 여부 판단
3. Wage 현재 endpoint/DTO와 공유 계약 차이를 정리하고, WorkProof output을 직접 소비하는 최소 DTO를 정한다.
4. Advance 신규 모듈의 최소 endpoint/DTO/error code 초안 범위를 정한다.
5. 공통 error code / validation / auth / idempotency 적용 지점을 문서화한다.
6. 실제 구현 커밋에 들어가기 전, lane을 WorkProof -> Wage -> Advance 순으로 자른다.

# Verification
- 문서 검토:
  - 현재 코드와 공유 문서 차이가 도메인별로 설명되는지 확인
  - low-regret 영역과 후속 보류 영역이 명확히 나뉘는지 확인
  - auth / validation / error / idempotency 규칙이 빠지지 않았는지 확인
- 실행 테스트:
  - 없음. 이 단계는 착수 범위 정리 문서다.

# Worktree Split Decision
- Single lane

WorkProof/Wage/Advance는 shared DTO, error code, auth, monthly summary shape를 함께 공유하므로 병렬 lane으로 나누기 전에 upstream 계약을 먼저 고정해야 한다.

# Commit Plan
- 다음 구현 커밋 전 문서/설계 정리 커밋 1개 가능
- 이후 구현은 WorkProof / Wage / Advance로 나눠 여러 커밋 가능

# Open Questions
- WorkProof에서 `workplaces/contracts`까지 이번 구현 lane에 포함할지, records/monthly-summary 먼저 열지 여부
- W7 결과를 WorkProof monthly summary와 Advance eligibility 중 어디에 먼저 고정할지 여부
- Wage의 현재 `deposits` API를 verification 생성으로 치환할지, transition period를 둘지 여부
- Advance를 실제 패키지로 열 때 simulation policy 값을 어디까지 코드 상수로 둘지 여부

# Assumptions
- 추가 PRD는 현재 baseline을 대체하기보다 확장으로 붙을 가능성이 높다.
- 지금 구현 착수는 현재 문서 기준을 넘어서 추측 확장을 하지 않는 것을 전제로 한다.
- Home / Copilot / Demo Time Travel 세부 shape는 현재 구현 lane에서 고정하지 않는다.
- WorkProof / Wage / Advance를 먼저 안정화하면 뒤 도메인의 재작업 위험을 줄일 수 있다.
# First Implementation Slice
## Lane 1: WorkProof upstream contract 정렬
### 포함
- `POST /api/workproof/workplaces`
- `GET /api/workproof/workplaces`
- `POST /api/workproof/contracts`
- `GET /api/workproof/contracts/current?workplaceId={id}`
- `POST /api/workproof/records/check-in`
- `POST /api/workproof/records/check-out`
- `GET /api/workproof/records?month=YYYY-MM&workplaceId={id}`
- `GET /api/workproof/records/{recordId}`
- `GET /api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}`

### 후속으로 미룸
- `POST /api/workproof/attachments`
- `POST /api/workproof/records/missing`
- `POST /api/workproof/records/{recordId}/modifications`

### 이유
- workplaces/contracts/check-in/check-out/monthly-summary가 잡혀야 Wage와 Advance가 같은 upstream 데이터를 읽을 수 있다.
- attachments/missing/modifications는 P0 범위 안이지만, 먼저 고정하지 않아도 upstream 집계와 explanation-first 흐름을 시작할 수 있다.
- 현재 코드의 단일 CRUD형 `CreateWorkProofRequest`를 여러 use-case DTO로 분해하는 첫 단계로 적합하다.

## WorkProof DTO 초안 범위
### 새 request DTO 후보
- `CreateWorkplaceRequest`
- `CreateContractRequest`
- `CheckInWorkProofRequest`
- `CheckOutWorkProofRequest`
- `ListWorkProofRecordsQuery` 또는 controller query validation
- `GetCurrentContractQuery` 또는 controller query validation
- `GetWorkProofMonthlySummaryQuery` 또는 controller query validation

### 새 response DTO 후보
- `WorkplaceResponse`
- `CurrentContractResponse`
- `WorkProofRecordResponse`
- `WorkProofRecordListItemResponse`
- `WorkProofMonthlySummaryResponse` 재정의

### 기존 DTO 처리 방향
- `CreateWorkProofRequest`, `UpdateWorkProofRequest`, `WorkProofResponse`는 현재 CRUD형 shape라 lane 1에서 바로 재사용하지 않는다.
- 월간 summary DTO는 이름은 유지하되 필드 shape를 공유 계약 기준으로 교체할 수 있다.

## WorkProof validation / error code 1차 후보
### validation 포인트
- `month`는 `YYYY-MM` 형식 필수
- `workplaceId` 필수
- check-in/check-out의 `deviceAt`, `latitude`, `longitude` 필수
- check-out은 check-in 이후 시각이어야 함
- contract 생성 시 `payUnit`, `basePayAmount` 필수
- `DAILY`는 `dailyWorkMinutes`, `MONTHLY`는 `monthlyWorkMinutes` 기본값 정책 허용
- 동일 workDate 중복 기록 금지

### error code 후보
- `WORKPLACE_NOT_FOUND`
- `ACTIVE_CONTRACT_REQUIRED`
- `ACTIVE_CONTRACT_EXISTS`
- `ACTIVE_CONTRACT_NOT_FOUND`
- `ACTIVE_WORKPROOF_EXISTS`
- `ACTIVE_WORKPROOF_NOT_FOUND`
- `WORK_DATE_ALREADY_EXISTS`
- `CHECK_OUT_BEFORE_CHECK_IN`
- `MODIFICATION_REASON_REQUIRED` (후속 lane)
- `INVALID_MISSING_RECORD_TIME` (후속 lane)

## Wage / Advance 착수 조건
### Wage
- WorkProof monthly summary에서 아래 값이 안정화돼야 한다.
  - `workDayCount`
  - `totalWorkMinutes`
  - `overtimeMinutes`
  - `nightMinutes`
  - `modifiedRecordCount`
  - `integrity.verifiedMinutes`
  - `integrity.pendingMinutes`
- 이후 `GET /api/wage/monthly-summary`, `GET /api/wage/estimate`, `POST /api/wage/verifications` 분해를 시작한다.

### Advance
- WorkProof integrity와 contract/current shape가 안정화된 뒤 신규 패키지를 연다.
- 1차는 아래 endpoint만 연다.
  - `GET /api/advance/eligibility?workplaceId={id}`
  - `POST /api/advance/requests`
- 목록/상세는 eligibility / request 생성 뒤로 미룰 수 있다.

# 2026-03-13 상태 업데이트
## 현재 상태
- 상태: WorkProof lane 1 구현 커밋 완료, 브랜치 마감 전 문서/검증 동기화 진행 중
- 현재 브랜치: `feature/workproof-lane1-dto-validation`
- 관련 커밋:
  - `c409c4b` `feat: WorkProof lane1 DTO 초안 추가`
  - `c33b6f4` `test: WorkProof lane1 DTO 검증 테스트 추가`
  - `370814a` `feat: WorkProof lane1 흐름 구현`

## 완료된 구현 범위
### DTO / validation / error code
- lane 1 request/response DTO 초안 추가:
  - `CreateWorkplaceRequest`
  - `CreateContractRequest`
  - `CheckInWorkProofRequest`
  - `CheckOutWorkProofRequest`
  - `GetCurrentContractQuery`
  - `ListWorkProofRecordsQuery`
  - `GetWorkProofMonthlySummaryQuery`
- lane 1 공통 응답 DTO 초안 추가:
  - `WorkplaceResponse`
  - `CurrentContractResponse`
  - `WorkProofRecordResponse`
  - `WorkProofRecordListItemResponse`
  - `WorkProofRecordListResponse`
  - `WorkProofMonthlySummaryContractResponse`
- `WorkProofPayUnit`, lane 1 draft validator, shared `ErrorCode` 확장 반영

### controller / service / model / repo
- 아래 lane 1 endpoint를 `WorkProofController`와 `WorkProofLane1Service`로 연결 완료:
  - `POST /api/workproof/workplaces`
  - `GET /api/workproof/workplaces`
  - `POST /api/workproof/contracts`
  - `GET /api/workproof/contracts/current`
  - `POST /api/workproof/records/check-in`
  - `POST /api/workproof/records/check-out`
  - `GET /api/workproof/records`
  - `GET /api/workproof/records/{recordId}`
  - `GET /api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}`
- 신규 영속 모델 추가:
  - `Workplace`
  - `WorkContract`
- `WorkProof`에 workplace/contract 기반 lane 1 필드와 상태 전이 로직 추가
- 신규 repository 추가:
  - `WorkplaceRepository`
  - `WorkContractRepository`
- `WorkProofRepository`에 lane 1 조회 메서드 확장

### 테스트
- `WorkProofLane1DraftValidatorTest` 추가 및 단위 검증 통과
- `WorkProofLane1IntegrationTest` 추가:
  - lane 1 happy path
  - 인증/충돌 규칙
  - 타인 리소스 은닉 규칙
- 통합 테스트 메서드와 단계별 블록에 시나리오 주석 추가

## 이번 lane에서 의도적으로 미룬 범위
- `POST /api/workproof/attachments`
- `POST /api/workproof/records/missing`
- `POST /api/workproof/records/{recordId}/modifications`
- Wage `monthly-summary / estimate / verifications` 구현
- Advance 패키지 착수
- ERD 확정본과 migration SQL 작성

## 검증 상태
- 완료:
  - `./gradlew.bat test --tests com.workproofpay.backend.workproof.WorkProofLane1DraftValidatorTest`
  - `./gradlew.bat integrationTest --tests "com.workproofpay.backend.workproof.WorkProofLane1IntegrationTest"`
- 남은 환경 이슈:
  - IntelliJ `integrationTest` 실행에서는 Testcontainers가 Docker 엔진을 안정적으로 감지하지 못하는 IDE 환경 문제가 남아 있음

## 남은 브랜치 마감 항목
1. IntelliJ `integrationTest` 환경 이슈를 MR/로그에 IDE 한정 블로커로 명시
2. `.private/kwanwoo/context/CURRENT.md`, `NEXT.md`, `logs/2026-03-13.md` 동기화
3. 이 execplan을 현재 구현 기준으로 유지한 뒤, WorkProof 브랜치가 닫히면 archive 이동 여부 판단

## 다음 추천 슬라이스
- 현재 브랜치 마감 후 새 `feature/*` 브랜치에서 Wage lane 1 skeleton 착수
- 우선순위:
  - `GET /api/wage/monthly-summary`
  - `GET /api/wage/estimate`
  - 관련 응답 DTO와 계산 근거 shape 정리
