# Source Inputs
- 사용자 제공 Spring Boot 부팅 로그
  - `companies.overtime_rounding_unit`, `companies.scheduled_clock_in_time`, `companies.scheduled_clock_out_time`
  - `correction_requests.reason_code`
  - 기존 행이 있는 상태에서 `ALTER TABLE ... ADD COLUMN ... NOT NULL` 실패
- 코드 탐색
  - `apps/dondone-backend/src/main/resources/application.yml`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/Company.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/correction/model/CorrectionRequest.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerWorkplaceSettingsService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerCorrectionRequestService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerIssueReadModelService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/response/WorkProofCorrectionRequestResponse.java`
- 기존 테스트
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerWorkplaceSettingsIntegrationTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/workproof/WorkProofIntegrationTest.java`

# Goal
- 기존 데이터가 남아 있는 PostgreSQL dev DB에서도 DonDone 백엔드가 부팅되도록 스키마 호환성을 복구한다.
- 새 attendance policy / correction reason 필드의 현재 API 계약은 유지하되, 레거시 null 상태를 코드에서 안전하게 흡수한다.

# In Scope
- `Company`, `CorrectionRequest`의 레거시 null 허용 경로 정리
- 기본값 해석 로직 추가
- Hibernate `ddl-auto=update` 환경에서 컬럼이 nullable로 추가되도록 엔티티 매핑 조정
- 부팅 회귀를 막기 위한 테스트 보강

# Out of Scope
- Flyway/Liquibase 도입
- 전체 DB migration 체계 개편
- 운영 DB 수동 데이터 정제 스크립트 작성
- attendance policy 기능 자체의 계약 변경

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/Company.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/correction/model/CorrectionRequest.java`
- 기본값을 사용하는 employer/workproof 서비스 또는 응답 매핑 경로
- 관련 backend integration/unit test

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-24-backend-startup-schema-compatibility.md`

## Shared
- 없음

# Contract Changes
- 외부 API DTO 필드 추가/삭제 없음
- 레거시 DB 행에서 비어 있는 값은 아래 기본값으로 해석
  - `scheduledClockInTime`: `09:00`
  - `scheduledClockOutTime`: `18:00`
  - `overtimeRoundingUnit`: `FIFTEEN_MINUTES`
  - `reasonCode`: `OTHER`
- DB 레벨에서는 해당 신규 컬럼을 nullable로 두어 기존 행이 있는 개발 DB에서도 추가 가능하게 한다.

# Security Notes
- auth/authz 규칙 변경 없음
- 노출 경로 변경 없음
- 토큰 처리 변경 없음

# Maintainability Notes
- 현재 저장소는 `ddl-auto=update` 기반 dev 흐름이므로, 새 필드 도입 시 레거시 row 호환 기본값을 엔티티 경계에서 흡수하는 편이 가장 작은 변경이다.
- 서비스 레이어마다 null 방어를 흩뿌리지 않고 엔티티 getter 쪽에서 일관된 기본값을 제공해야 유지보수 비용이 낮다.
- 이번 수정은 부팅 복구가 목적이므로 migration framework 전환까지 확장하지 않는다.

# Implementation Steps
1. 부팅 실패를 유발한 신규 필드의 매핑과 사용 지점을 확인한다.
2. `Company`, `CorrectionRequest`에 레거시 null 허용 및 기본값 해석 로직을 추가한다.
3. 기본값 경로가 employer/workproof 응답과 정책 계산에 그대로 반영되는지 확인한다.
4. 레거시 null row 시나리오를 검증하는 테스트를 추가한다.
5. 관련 테스트를 실행해 회귀 여부를 확인한다.

# Test Plan
- `EmployerWorkplaceSettingsIntegrationTest`에 레거시 null policy 값 반환 시 기본값을 검증하는 테스트 추가
- 필요 시 correction reason 기본값 해석 테스트 추가
- 실행 명령
  - `.\gradlew.bat test --tests com.workproofpay.backend.employer.EmployerWorkplaceSettingsIntegrationTest --tests com.workproofpay.backend.employer.EmployerCorrectionRequestIntegrationTest --tests com.workproofpay.backend.employer.EmployerIssueReadModelIntegrationTest --tests com.workproofpay.backend.workproof.WorkProofIntegrationTest`

# Review Focus
- nullable DB 컬럼 허용이 신규 생성/수정 플로우의 계약을 약화시키지 않는지
- 레거시 null 기본값이 API 응답과 정책 계산에서 일관되게 적용되는지
- `DevEmployerInitializer`가 부분 마이그레이션 상태의 DB에서도 더 이상 부팅을 깨지 않는지

# Worktree Split Decision
- Single lane

엔티티 매핑과 기본값 해석이 공통 read path 여러 곳에 동시에 영향을 주므로 병렬 분리는 오히려 계약 드리프트와 머지 충돌 위험이 높다.

# Commit Plan
1. `docs: add backend startup schema compatibility execplan`
2. `fix: make backend startup tolerant to legacy null schema rows`

# Open Questions
- 없음

# Assumptions
- 현재 문제는 개발/데모 DB 부팅 복구가 우선이며, 운영용 정식 migration 체계 전환은 별도 작업으로 다룬다.
- 레거시 correction request에 사유 코드가 없던 경우 `OTHER`로 해석해도 현재 제품 의미와 충돌하지 않는다.
- 레거시 company attendance policy가 비어 있던 경우 기본 출근 `09:00`, 퇴근 `18:00`, 반올림 `FIFTEEN_MINUTES`가 기존 시드/테스트 의도와 일치한다.
