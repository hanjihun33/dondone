# Source Inputs
- 사용자 요청:
  - `docs/web/README.md`와 `docs/web/implementation-slices.md` 기준으로 Slice 2 `Auth and profile foundation` 범위 구체화
  - `auth-and-role-policy`, `shared-entity-validation`, review note를 반영한 execplan 작성
  - `invitation token contract`, `employer role/profile`, `membership authz`를 이번 slice 범위로 고정
  - 기존 앱 API 변경 제외, 웹 전용 경계 유지
- 기준 문서:
  - `docs/web/README.md`
  - `docs/web/implementation-slices.md`
  - `docs/web/auth-and-role-policy.md`
  - `docs/web/shared-entity-validation.md`
  - `docs/web/employer-worker-domain-map.md`
  - `docs/web/employer-web-api-map.md`
  - `docs/reviews/active/2026-03-19-web-employer-doc-validation-review.md`
- 현재 코드 근거:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/AuthController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/UserRole.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `apps/dondone-web/src/app/router.tsx`
  - `apps/dondone-web/src/app/AppShell.tsx`

# Goal
기존 worker 앱 auth 계약은 유지한 채, 웹 전용 Slice 2에서 employer 초대 수락, employer 로그인, employer 프로필 부트스트랩, membership 기반 authorization source of truth를 구현할 수 있는 최소 코드 경계와 계약을 고정한다.

# Status
- 상태: `implemented`
- 완료일: `2026-03-19`
- 검증: `apps/dondone-backend`에서 `./gradlew test` 통과

# Outcome Summary
- `POST /api/employer-auth/invitations/accept`, `POST /api/employer-auth/login`, `GET /api/employer/profile`를 backend에 구현했다.
- `UserRole.EMPLOYER`, `EmployerProfile`, `EmployerInvitationToken`, `EmploymentMembership`, `EmployerAccessScope` 기반 foundation을 추가했다.
- `SecurityConfig`에서 `/api/employer-auth/**` 공개, `/api/employer/**` employer 전용, 기존 `/api/**` worker/admin 전용 경계를 명시했다.
- 리뷰 반영으로 invitation token은 DB row 기반 hash 저장으로 전환했고, 이메일 canonicalization은 worker/employer 공통 정책으로 맞췄다.
- company-workplace scope 위조를 막기 위해 `Workplace.companyId` 보조 연결과 company-workplace binding 검증을 추가했다.

# In Scope
- backend에 웹 전용 public auth 표면 추가:
  - `POST /api/employer-auth/invitations/accept`
  - `POST /api/employer-auth/login`
- backend에 웹 전용 protected profile 표면 추가:
  - `GET /api/employer/profile`
- employer 전용 role/profile foundation 추가:
  - `UserRole` 또는 authority 체계에 `EMPLOYER` 추가
  - `EmployerProfile`에 `accountId`, `companyId`, `defaultWorkplaceId`, `displayName`, `status`를 둔 최소 조직 연결 모델 도입
- invitation token contract 구체화:
  - `inviteeEmail`, `companyId`, `defaultWorkplaceId`, `role=EMPLOYER`, `expiresAt`, `revokedAt`, `usedAt`, `invitedByAccountId`
  - 토큰은 1회성, 만료, 폐기, email/company/role 바인딩 검증을 통과해야만 수락 가능
- membership authz foundation 추가:
  - employer web authorization source of truth를 `EmployerProfile + EmploymentMembership`으로 고정
  - future employer read-model에서 재사용할 `EmployerAccessScope` 또는 동등한 authorization helper/service 추가
  - bare ID 대상 재검증 로직이 membership을 통해 target company/workplace 범위를 확인하도록 기반 코드와 테스트 준비
- 웹 앱에서 employer 세션 부트스트랩 최소 범위 추가:
  - employer login/accept API 클라이언트
  - employer profile fetch 및 route guard 진입점
  - 기존 dashboard/workers/settings mock 화면을 employer 인증 완료 후 진입하는 구조로 연결할 최소 shell 조정

# Out of Scope
- 기존 `/api/auth/*`, `/api/workproof/*`, `/api/wage/*` 계약 변경
- mobile 앱 auth 또는 DTO 변경
- self-service employer signup 공개 오픈
- dashboard/workers/issues/settings read-model 본 구현
- correction request 상태 전이 구현
- worker/employer 겸용 계정 허용
- `Workplace.user`, `WorkProof.user` 제거 또는 파괴적 스키마 교체
- workplace switcher, multi-company, multi-membership 지원

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`
  - 공통 JWT/password 인프라 재사용
  - `UserRole`, `User.register()` 확장 또는 employer 전용 생성 경로 추가
- 신규 employer feature package 후보:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/api/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/service/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/repo/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/**`
- 신규 employer profile/authz package 후보:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/security/**`

## Mobile
- 없음

## Docs
- `docs/web/implementation-slices.md`
- `docs/web/auth-and-role-policy.md`
- `docs/web/shared-entity-validation.md`
- `docs/web/employer-worker-domain-map.md`
- `docs/execplans/active/2026-03-19-web-auth-profile-foundation.md`

## Shared
- `apps/dondone-backend/src/main/resources/**`
  - 신규 테이블/seed가 필요하면 migration 또는 bootstrap 리소스 추가
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/auth/**`
- 신규 employer/authz 테스트 패키지
- `apps/dondone-web/src/app/**`
- 필요 시 `apps/dondone-web/src/shared/**`

# Contract Changes
- 기존 앱 API response/DTO는 바꾸지 않는다.
- 웹 전용 auth request/response DTO를 별도 정의한다.
- `POST /api/employer-auth/invitations/accept`
  - request: `token`, `email`, `password`, `displayName`
  - success response: `accessToken`, `expiresIn`, `employerId`, `companyId`, `companyName`, `defaultWorkplaceId`, `defaultWorkplaceName`
  - error cases:
    - token 재사용/만료/폐기
    - token email/company/role 바인딩 불일치
    - 이미 존재하는 email
    - company 또는 default workplace 연결 누락
- `POST /api/employer-auth/login`
  - request: `email`, `password`
  - success response: `accessToken`, `expiresIn`, `employerId`, `companyId`, `companyName`, `defaultWorkplaceId`, `defaultWorkplaceName`
  - error cases:
    - invalid credentials
    - employer profile 미존재
    - employer profile inactive
    - company/default workplace 연결 누락
- `GET /api/employer/profile`
  - response: `employerId`, `displayName`, `email`, `companyId`, `companyName`, `companyCode`, `defaultWorkplaceId`, `defaultWorkplaceName`, `status`
  - `connectedWorkerCount` 같은 집계 필드는 Slice 4로 미룬다.
- invitation token 저장 계약은 DB row 우선으로 잡는다.
  - 이유: 1회성, revoke, usedAt 검증과 감사 추적을 server state로 단순하게 강제할 수 있다.
- membership authz contract
  - employer 권한 범위는 request `companyId`로 받지 않는다.
  - 서버가 `EmployerProfile.companyId`와 `defaultWorkplaceId`를 해석한다.
  - future bare-ID endpoint는 대상 레코드를 `EmploymentMembership.companyId/workplaceId`로 재검증한다.

# Security Notes
- `SecurityConfig`에 `/api/employer-auth/**` permitAll, `/api/employer/**` employer role 제한을 명시적으로 추가한다.
- employer token으로 worker 전용 endpoint 접근을 허용하지 않는다. worker token으로 employer endpoint 접근도 허용하지 않는다.
- invitation accept는 token 검증과 email binding 검증이 모두 통과하기 전 계정을 생성하지 않는다.
- login 성공 조건에 단순 JWT 발급뿐 아니라 `EmployerProfile.status`, `companyId`, `defaultWorkplaceId` 유효성 검사를 포함한다.
- employer authorization source of truth는 `EmployerProfile + EmploymentMembership`이다. 레거시 `Workplace.user`, `WorkProof.user`는 employer authz에 사용하지 않는다.
- 초대 토큰 실패 사유는 외부에 과도하게 노출하지 않고 동일 계열 오류로 처리한다.

# Maintainability Notes
- 기존 `auth` 패키지를 employer 전용 DTO/엔드포인트로 오염시키지 말고, 공통 인증 인프라만 재사용한다.
- role 확장과 employer profile 도입은 최소 경계로 자른다. dashboard/workers/settings read-model까지 이번 slice에 섞지 않는다.
- membership authz는 이후 Slice 3~5에서 재사용할 service/helper로 고정하고, controller마다 ad-hoc query를 새로 쓰지 않는다.
- employer web 전용 DTO는 worker DTO와 분리해서 유지한다.
- 신규 조직 엔티티는 `추가 테이블 + 보조 연결` 접근을 유지하고 기존 worker 레거시 소유 컬럼을 바로 제거하지 않는다.

# Implementation Steps
1. backend auth/security 경계에 employer 전용 공개/보호 endpoint를 추가했다.
2. `UserRole.EMPLOYER`와 employer account 생성 경로를 도입하고 기존 `/api/auth/signup` 흐름은 유지했다.
3. `EmployerInvitationToken`, `EmployerProfile`, `Company`, `EmploymentMembership` 저장 모델을 도입했다.
4. invitation accept/login/profile bootstrap에 company-default workplace binding 검증을 연결했다.
5. `EmployerAccessScope` 기반 helper를 추가해 향후 `/api/employer/*` endpoint 재사용 기반을 만들었다.
6. 리뷰 반영으로 invitation token hash 저장, 이메일 canonicalization 공통화, company-workplace mismatch 차단을 적용했다.
7. integration/security tests로 token contract, role 분리, membership authz source of truth, email normalization 회귀를 검증했다.
8. 문서 계약과 실제 구현이 맞도록 Slice 2 기준 문서를 갱신했다.

# Remaining Follow-ups
- `Workplace.companyId`는 현재 employer 검증용 additive 연결이므로, app/web 공통 리팩터링 시점에 공용 필수 소속 키 승격 여부를 다시 결정해야 한다.
- DB 차원의 `lower(email)` 유니크 인덱스는 아직 추가하지 않았으므로, 장기 hardening 후보로 남긴다.
- invitation 발급/운영 bootstrap endpoint는 이번 slice 범위에 포함하지 않았고 seed/bootstrap 흐름으로 한정했다.
- Slice 3에서는 `EmployerAccessScope`를 재사용해 workplace settings 계약과 효력 시점 규칙을 고정해야 한다.

# Test Plan
- backend integration
  - employer invitation accept 성공
  - employer invitation token 재사용 차단
  - employer invitation token 만료 차단
  - employer invitation token 폐기 차단
  - token email/company/role binding 불일치 차단
  - employer login 성공
  - employer profile inactive 또는 연결 누락 login 차단
  - worker token으로 `GET /api/employer/profile` 접근 시 `403`
  - employer token으로 worker 전용 보호 endpoint 접근 시 차단되는지 확인
- backend authz regression
  - legacy `Workplace.user`, `WorkProof.user` 값이 employer scope 허용 근거로 섞이지 않는 테스트
  - membership 기준 cross-company, cross-workplace target 검증 helper 테스트
- web verification
  - employer 로그인 후 profile bootstrap 성공
  - employer 미인증 상태에서 보호 라우트 차단
  - 로그인 실패/토큰 실패/프로필 fetch 실패에 대한 loading/error state 분리
- 실행 명령
  - `cd apps/dondone-backend && ./gradlew test`
  - `cd apps/dondone-web && npm run build`

# Review Focus
- invitation token contract가 review note의 testing gap을 실제 테스트 항목으로 닫는지
- employer role/profile 추가가 기존 `/api/auth/*` 및 worker flow를 건드리지 않는지
- membership authz source of truth가 `EmployerProfile + EmploymentMembership`으로 일관되게 유지되는지
- `defaultWorkplaceId`가 scope를 넓히는 client 입력 없이 서버에서만 해석되는지
- employer 전용 DTO와 endpoint가 worker DTO/API와 섞이지 않는지

# Worktree Split Decision
Single lane

이번 slice는 `role`, `SecurityConfig`, `EmployerProfile`, `EmploymentMembership`, employer auth DTO/API 계약이 동시에 움직인다. shared DTO는 건드리지 않더라도 auth/security와 조직 범위 source of truth가 함께 바뀌므로 병렬 레인으로 나누면 merge risk가 높다.

# Commit Plan
1. `docs: scope web auth and profile foundation slice`
2. `feat: add employer invitation auth and profile foundation`
3. `test: cover employer auth token and membership authorization`

# Open Questions
- `defaultWorkplaceId`를 장기적으로도 `EmployerProfile`에 직접 둘지, workplace switcher 도입 시 preference 엔티티로 분리할지
- `Workplace.companyId`를 언제 worker 앱까지 포함한 공용 필수 소속 키로 승격할지
- invitation 발급/운영 bootstrap 흐름을 Slice 3 이전에 별도 endpoint로 열 필요가 있는지

# Assumptions
- Slice 2의 목표는 employer web이 로그인과 최소 프로필 부트스트랩까지 가능한 foundation을 만드는 것이지, worker list/dashboard read-model을 여는 것이 아니다.
- invitation token 저장은 signed token 단독보다 DB row 기반이 MVP 검증과 운영 회수에 유리하고, 저장 값은 hash로 유지한다.
- employer 한 명은 MVP에서 company 1곳, default workplace 1곳만 가진다.
- worker/employer 겸용 계정은 허용하지 않는다.
- 기존 app auth/API를 바꾸지 않고도 web 전용 endpoint와 DTO를 추가하는 방식으로 범위를 닫을 수 있다.
- employer authz source of truth는 `EmployerProfile + EmploymentMembership`이고, `Workplace.companyId`는 binding 검증용 보조 연결로 취급한다.
