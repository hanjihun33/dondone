# Source Inputs
- 사용자 요청:
  - Slice 4 `Worker directory and dashboard read-model` backend 시작
  - `EmployerAccessScope + EmploymentMembership` 기준 read-model scope 확정
  - `GET /api/employer/workers`부터 DTO/API/service/test 뼈대 구현
  - 가능하면 `GET /api/employer/dashboard/summary`까지 진행
- 기준 문서:
  - `docs/web/README.md`
  - `docs/web/implementation-slices.md`
  - `docs/web/employer-web-api-map.md`
  - `docs/web/employer-worker-domain-map.md`
  - `docs/web/shared-entity-validation.md`
  - `docs/execplans/active/2026-03-19-web-workplace-settings-foundation.md`
  - `docs/reviews/active/2026-03-19-web-workplace-settings-followups.md`
- 현재 코드 근거:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerAccessScope.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerAccessScopeService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/EmploymentMembership.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkProofRepository.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/EmployerWorkplaceSettingsIntegrationTest.java`

# Goal
Slice 4 foundation으로 employer web read-model 첫 구간을 열고, `GET /api/employer/workers`와 가능하면 `GET /api/employer/dashboard/summary`를 현재 백엔드가 실제로 증명 가능한 조직/출퇴근 데이터 기준으로 안전하게 제공한다.

# In Scope
- employer worker directory read-model endpoint 추가
  - `GET /api/employer/workers`
- employer dashboard foundation endpoint 추가
  - `GET /api/employer/dashboard/summary`
  - `GET /api/employer/dashboard/attendance-board`
- `EmployerAccessScope.defaultWorkplaceId` + `EmploymentMembership.companyId/workplaceId` 기반 조회 scope 고정
- worker list/search/pagination 최소 DTO 추가
- 현재 도메인으로 안전하게 계산 가능한 attendance status/read-model summary 고정
- Slice 4 관련 backend 테스트 추가
- Slice 4 계약이 바뀌는 문서 갱신

# Out of Scope
- 기존 `/api/workproof/*`, `/api/auth/*`, `/api/wage/*` API 변경
- correction request flow 본구현
- mobile 변경
- multi-workplace switcher 구현
- worker detail endpoint (`GET /api/employer/workers/{workerId}`) 본구현
- worker profile 전용 엔티티 추가
- 휴가/결근 canonical source 신설

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/dto/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/repo/UserRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkProofRepository.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/**`

## Mobile
- 없음

## Docs
- `docs/web/implementation-slices.md`
- `docs/web/employer-web-api-map.md`
- 본 실행계획 문서

## Shared
- `docs/web/employer-worker-domain-map.md`
- `docs/web/shared-entity-validation.md`

# Contract Changes
- 신규 query DTO
  - `query`
  - `statuses`
  - `page`
  - `size`
- 신규 worker list response
  - `workers[]`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
- worker row foundation 필드
  - `workerId`
  - `name`
  - `email`
  - `recordStatus`
  - `reflectionStatus`
  - `attendanceStatus`
  - `employeeCode`, `team`, `role`, `phone`, `avatarUrl`는 현재 source가 없으면 `null`
- attendance board response
  - `weekStart`
  - `weekEnd`
  - `rows[]`
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
  - `hasNext`
- attendance board row/day foundation 필드
  - row: `workerId`, `name`, `role`, `avatarUrl`, `days[]`
  - day: `date`, `recordStatus`, `reflectionStatus`, `attendanceStatus`, `workedMinutes`
- dashboard summary foundation 필드
  - `activeWorkerCount`
  - `workingCount`
  - `completedCount`
  - `needsReviewCount`
  - `noRecordCount`
  - `asOf`
- 이번 slice에서는 `leaveCount`, `absentCount`, job role/team/employeeCode의 실데이터 source를 새로 만들지 않는다.

# Security Notes
- `/api/employer/**` role 제한은 기존 `SecurityConfig`를 그대로 사용한다.
- employer scope는 항상 `EmployerProfile.companyId + defaultWorkplaceId`로 해석한다.
- worker 조회 가능 대상은 active `EmploymentMembership`로만 제한한다.
- 레거시 `Workplace.user`, `WorkProof.user`는 read-model authz source가 아니라 read-model 조합 입력으로만 본다.
- `workerId` 같은 bare ID detail endpoint는 이번 slice 범위 밖이라 열지 않는다.
- workproof status는 scope workplace에 매달린 오늘 기록만 읽고, 다른 workplace 기록은 read-model에 섞지 않는다.
- attendance board는 주간 범위를 읽더라도 해당 workplace에 걸린 record만 조합한다.

# Maintainability Notes
- worker list/dashboard summary는 같은 scoped worker snapshot 조합 로직을 공유해 중복 status 계산을 피한다.
- worker profile 전용 엔티티가 아직 없으므로 없는 필드를 임의로 합성하지 않는다.
- foundation 단계에서는 dedicated read table 없이 service 조합으로 시작하되, 상태 필터와 pagination이 커지면 Querydsl/read-model 분리 후보로 남긴다.
- 휴가/결근 같은 미정 상태를 무리하게 흉내 내지 말고, 현재 `WorkProof`가 증명하는 `working/completed/needs_review/no_record`만 노출한다.
- `recordStatus`는 기존 app API와 같은 뜻으로 `CHECKED_IN/CHECKED_OUT`만 쓰고, review 여부는 `reflectionStatus`에서 표현한다.

# Implementation Steps
1. Slice 4 execplan을 추가하고 현재 코드 기준 계약/가정/리스크를 문서화한다.
2. `EmploymentMembershipRepository`, `WorkProofRepository`에 scoped read-model 조합용 최소 query를 추가한다.
3. employer worker directory query/response DTO와 controller/service를 추가한다.
4. scoped active membership + today workproof 조합으로 worker attendance status를 계산한다.
5. search/status filter/pagination을 service layer에서 foundation 수준으로 고정한다.
6. 같은 scoped snapshot 조합을 재사용해 dashboard summary endpoint를 추가한다.
7. 주간 `attendance-board` query/response를 추가하고, 일별 `recordStatus/reflectionStatus/attendanceStatus`를 조합한다.
8. integration test로 scope/authz/search/status/summary/board 회귀를 닫는다.
9. Slice 4 상태와 contract 차이를 docs에 반영한다.

# Test Plan
- `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.EmployerWorkerReadModelIntegrationTest --tests com.workproofpay.backend.employer.EmployerWorkplaceSettingsIntegrationTest --tests com.workproofpay.backend.employer.EmployerAccessScopeServiceTest`
- 필요 시 전체 회귀:
  - `cd apps/dondone-backend && .\\gradlew.bat test`
- 검증 항목:
  - scope workplace active membership만 worker list에 노출
  - legacy workplace owner 불일치가 read-model 권한에 영향 주지 않음
  - search/status/pagination 기본 동작
  - worker token으로 employer read-model 접근 차단
  - dashboard summary가 현재 도메인으로 증명 가능한 status만 집계
  - attendance board가 weekStart/week range, row filter, 일별 raw status 축을 일관되게 노출

# Review Focus
- 조회 대상이 `EmploymentMembership` 기준으로 DB/service 수준에서 제한되는지
- status 계산이 다른 workplace 또는 과거 날짜 기록을 섞지 않는지
- worker profile 부재 필드를 임의 데이터로 채우지 않는지
- dashboard summary가 휴가/결근 같은 미정 상태를 오해되게 노출하지 않는지
- 이후 `worker detail`, `correction flow`가 붙을 때 재사용 가능한 조합 구조인지

# Worktree Split Decision
Single lane

이번 작업은 shared authz scope, membership query, workproof read-model 조합, API contract, 테스트, 문서가 함께 움직인다. shared DTO와 read-model source of truth가 아직 고정 단계라 병렬 레인으로 나누면 merge risk가 높다.

# Commit Plan
1. `docs: scope slice4 worker directory read-model`
2. `feat: add employer worker directory read-model foundation`
3. `test: cover employer worker read-model scope and summary`

# Open Questions
- worker profile 전용 엔티티 없이 `employeeCode/team/role/phone/avatarUrl`를 언제 어떤 source로 채울지
- 지각/휴가/결근 상태는 `WorkProof`만으로 부족한데, 추후 schedule/leave/correction 모델 중 무엇을 canonical source로 삼을지
- `attendance-board`는 week-based read-model을 별도 query로 뺄지, foundation snapshot 조합을 확장할지

# Remaining Follow-ups
- `GET /api/employer/workers/{workerId}` 계약과 target membership 재검증을 다음 순서로 남긴다.
- worker profile canonical source 부재로 `employeeCode/team/role/phone/avatarUrl`는 계속 nullable foundation으로 남긴다.
- `late/leave/absent` mockup 상태는 canonical source가 생길 때까지 열지 않는다.
- 후속 작업 순서는 `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`에서 계속 관리한다.

# Assumptions
- employer 계정은 MVP에서 회사 1곳과 default workplace 1곳만 직접 관리한다.
- worker 1명은 MVP에서 활성 membership 1건만 가진다.
- worker list/dashboard summary는 `오늘, 현재 default workplace` 기준 read-model로 시작한다.
- attendance board는 `weekStart`가 없으면 현재 주 일요일을 기준으로 7일 범위를 반환한다.
- 현재 코드에 worker profile 전용 source가 없으므로 `employeeCode/team/role/phone/avatarUrl`는 nullable foundation field로 둔다.
- dashboard summary는 휴가/결근을 정확히 표현할 canonical source가 준비되기 전까지 `working/completed/needs_review/no_record` 집계로 제한한다.
