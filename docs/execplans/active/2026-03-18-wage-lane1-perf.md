# Source Inputs
- `AGENTS.md`
- `docs/CODEX_WORKFLOW.md`
- `.agents/skills/backend-performance-improvement/SKILL.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkProofRepository.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

# 목표
`GET /api/wage/estimate` 경로에서 lane1 read path의 중복 DB 작업을 줄이고, 같은 조건에서 방어 가능한 before/after 수치를 남긴다.

# 성능 단계
- `기본기 최적화 단계 (baseline optimization)`

# 대상 Endpoint
- Primary benchmark: `GET /api/wage/estimate`
- 구조상 follow-up target: `GET /api/wage/monthly-summary`, `POST /api/wage/verifications`

# 왜 이 대상을 고르는가
- P0 사용자 가치가 이미 있는 endpoint다.
- `WageService.loadLane1Context(...)`가 lane1 데이터를 여러 upstream 호출로 조합하고 있다.
- 코드상 같은 `userId + month + workplaceId` 입력에 대해 ownership 확인과 월 범위 workproof 조회가 반복될 가능성이 높다.
- 작은 구조 수정으로 query-count 감소를 만들 수 있고, API contract를 건드리지 않을 가능성이 높다.

# In Scope
- H2 기반 query-count baseline 하네스를 추가한다.
- Wage lane1 로딩을 단일 snapshot 로더로 정리한다.
- 기존 response DTO와 validation 동작은 유지한다.
- before/after 결과를 사례 문서로 남긴다.
- 실제 작업에서 드러난 교훈이 있으면 스킬에 환류한다.

# Out Of Scope
- wage response shape 변경
- request DTO 변경
- DB schema 또는 index migration
- caching
- unrelated `advance` 또는 `workproof` 흐름 리팩터링
- broad Querydsl conversion

# 기대 동작
- `GET /api/wage/estimate` 응답 payload는 기존과 같아야 한다.
- `GET /api/wage/monthly-summary`와 `POST /api/wage/verifications`는 shared lane1 path를 쓰므로 기능 계약을 유지해야 한다.
- 에러 동작은 유지해야 한다.
  - invalid `month` 또는 `workplaceId`는 validation 경로 유지
  - missing workplace는 `WORKPLACE_NOT_FOUND`
  - missing active contract는 `ACTIVE_CONTRACT_NOT_FOUND`

# Contract Impact
- API contract change 없음
- DB schema change 없음
- mobile contract change 없음

# Security Impact
- authn/authz rule change 없음
- workplace ownership check 유지
- verification resource ownership semantics 유지
- 쿼리를 줄이기 위해 보호 자원 semantics를 우회하지 않음

# 성능 가설
- 기존 `WageService.loadLane1Context(...)`는 사실상 아래 비용을 중복 발생시킨다.
  - workplace ownership lookup 반복
  - month-scoped workproof list lookup 반복
- 단일 lane1 snapshot loader로 묶으면 primary benchmark endpoint의 prepared statement 수를 줄일 수 있다.

# 성공 기준
- `GET /api/wage/estimate`에서 prepared statement count가 의미 있게 감소한다.
- 응답 payload는 유지된다.
- 회귀 테스트가 동작한다.

# 측정 계획
1. Hibernate statistics 기반 integration harness를 추가한다.
2. fixture를 고정한다.
   - user 1명
   - workplace 1개
   - active contract 1개
   - reflected workproof 여러 건
3. 리팩터링 전 prepared statement count를 기록한다.
4. service refactor를 적용한다.
5. 같은 시나리오로 다시 측정한다.
6. 결과와 잔여 리스크를 사례 문서에 남긴다.
7. Docker/Testcontainers가 막히면 H2 회귀 테스트와 외부 Docker PostgreSQL 실측을 분리한다.

# 구현 단계
1. target endpoint용 query-count 테스트를 추가한다.
2. baseline query-count를 확인한다.
3. lane1 loading을 snapshot 방식으로 정리한다.
4. benchmark test와 기존 wage integration coverage를 다시 확인한다.
5. 리뷰 문서에 before/after 수치를 남긴다.
6. 실제 실행에서 드러난 guardrail이 있으면 스킬을 보강한다.

# 테스트 전략
- integration:
  - `GET /api/wage/estimate`용 query-count 회귀 테스트
  - 기존 wage integration test로 기능 회귀 확인
- PostgreSQL 실측:
  - 환경이 허용되면 외부 Docker PostgreSQL 경로로 별도 실행
- 환경 blocker가 있으면 문서에 명시

# 리뷰 포인트
- target endpoint에서 query-count가 실제로 줄었는가
- response contract가 유지됐는가
- workplace ownership 또는 active-contract error handling이 약해지지 않았는가
- 새 lane1 path가 다시 중복 호출 구조를 만들지 않는가

# Worktree Split Decision
- `Single lane`

shared service path와 shared response assembly를 함께 건드리므로 단일 레인으로 처리한다.

# 가정
- 첫 사례에서는 query-count가 가장 방어하기 쉬운 primary metric이다.
- 로컬 시간값은 흔들릴 수 있으므로 deterministic SQL-count evidence를 먼저 확보한다.
- repo에 이미 있는 wage integration coverage로 shared lane1 path refactor를 안전하게 감당할 수 있다.
