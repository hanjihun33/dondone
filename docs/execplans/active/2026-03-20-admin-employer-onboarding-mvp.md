# Source Inputs

## Implementation Update

- implemented contract:
  - `POST /api/admin/employers/companies`
  - `GET /api/admin/employers/companies`
  - admin login uses existing `POST /api/auth/login`
- final response policy:
  - create response returns raw `employerSignupCode` once
  - list response does not return raw signup code
  - list response returns issuance status and `latestEmployerSignupCodeIssuedAt`
- company onboarding now binds `Company.defaultWorkplaceId` so admin/company reads do not depend on inferring the workplace from signup-code rows.
- current implementation creates a placeholder default workplace and defers real address/radius configuration to the employer settings flow.

- `apps/dondone-web/src/pages/admin/AdminPage.tsx`
- `apps/dondone-web/src/pages/auth/LoggedOutPage.tsx`
- `apps/dondone-web/src/shared/auth/session.ts`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/AuthController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/Company.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/EmployerSignupCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevUserInitializer.java`
- 사용자 결정:
  - 서비스 관리자가 회사 등록, 기본 사업장 생성, 고용주 회사코드 발급을 담당
  - 지금 대화 안에서 admin lane까지 이어서 구현
  - 근로자용 등록 코드는 이번 범위에서 제외

# Goal

관리자 최소 MVP를 구현해 서비스 관리자가 web에서 회사를 등록하고, 기본 사업장을 만들고, 고용주 회사코드를 발급할 수 있게 한다. 고용주 회원가입에서 이미 사용하는 `EmployerSignupCode` 저장소를 그대로 재사용해 admin lane과 employer lane을 연결한다.

# In Scope

- dev admin 계정 seed 추가
- admin 로그인 시 backend `/api/auth/login` 재사용
- admin session에 access token 저장
- `POST /api/admin/employers/companies` 추가
- `GET /api/admin/employers/companies` 추가
- 회사 생성 시 기본 사업장과 고용주 회사코드를 함께 생성
- admin page를 company onboarding 중심 화면으로 교체 또는 축소 재구성
- admin backend/web 테스트와 문서 갱신

# Out of Scope

- admin 승인 기반 가입 요청 플로우
- 고용주 회사코드 재발급/폐기
- 근로자용 등록 코드 발급
- remittance admin 기능 재구성
- admin profile 전용 화면

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevUserInitializer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/Workplace.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkplaceRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/EmployerSignupCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/repo/EmployerSignupCodeRepository.java`
- 신규 `apps/dondone-backend/src/main/java/com/workproofpay/backend/admin/**`
- 신규 admin integration test

## Mobile

- 없음

## Docs

- `docs/execplans/active/2026-03-20-admin-employer-onboarding-mvp.md`
- `docs/web/auth-and-role-policy.md`
- `docs/web/implementation-slices.md`

## Shared

- `apps/dondone-web/src/shared/auth/session.ts`
- `apps/dondone-web/src/shared/api/client.ts`
- `apps/dondone-web/src/shared/api/*`

# Contract Changes

- admin login은 별도 endpoint를 만들지 않고 `/api/auth/login`으로 `ADMIN` role 계정도 허용
- 신규 admin API:
  - `POST /api/admin/employers/companies`
  - `GET /api/admin/employers/companies`
- create request:
  - `companyName`
  - `companyCode`
  - `workplaceName`
  - `address`
  - `detailAddress`
  - `latitude`
  - `longitude`
  - `allowedRadiusMeters`
- create/list response:
  - `companyId`
  - `companyName`
  - `companyCode`
  - `defaultWorkplaceId`
  - `defaultWorkplaceName`
  - `employerSignupCode`
  - `createdAt`

# Security Notes

- admin API는 계속 `/api/admin/**` + `ROLE_ADMIN`으로 보호
- admin login은 일반 auth login을 쓰더라도 role check는 각 admin API에서 보장
- employer signup code는 raw code를 응답으로 1회 노출하지만 DB에는 hash만 저장
- companyCode와 employer signup code는 다른 개념이므로 응답/문구에서 분리

# Maintainability Notes

- admin lane은 onboarding에 필요한 최소 surface만 구현하고, 기존 remittance admin과 섞지 않는다
- `EmployerSignupCode` 발급 로직은 admin service 한 곳으로 모아 이후 재발급/폐기 기능이 같은 owner를 갖게 한다
- admin session token 저장은 manager session 구조를 확장하되, 현재 라우트 가드와 AppShell 변경이 최소가 되도록 한다

# Implementation Steps

1. admin 사용 가능한 seed 계정과 `User.registerAdmin()`을 추가한다.
2. admin session이 backend token을 저장하도록 auth/session과 로그인 화면을 수정한다.
3. admin onboarding API DTO/controller/service를 만들고, 회사 + 기본 사업장 + 고용주 회사코드 생성 흐름을 구현한다.
4. list API로 현재 등록된 회사와 고용주 회사코드를 조회할 수 있게 한다.
5. admin page를 company onboarding UI로 바꾸고 create/list를 backend와 연결한다.
6. auth policy와 implementation slice 문서를 현재 구조에 맞게 갱신한다.
7. admin backend integration test와 web typecheck를 실행한다.

# Test Plan

- backend targeted tests
  - 신규 admin onboarding integration test
  - `.\gradlew.bat test --tests ...admin... --tests ...employerauth...`
- 검증 포인트
  - admin 로그인 성공
  - admin token 없이 admin endpoint 접근 차단
  - 회사 생성 시 company/workplace/signup code가 함께 생성
  - list response에 raw employer signup code가 기대대로 노출
- frontend
  - `cd apps/dondone-web && .\\node_modules\\.bin\\tsc.cmd -b`

# Review Focus

- admin auth를 `/api/auth/login` 재사용해도 role 혼선이 없는지
- companyCode와 employer signup code를 UI/DTO에서 혼동하지 않는지
- 회사 생성 트랜잭션이 company, workplace, signup code를 일관되게 묶는지
- admin page를 real backend로 바꾸면서 기존 mock 의존이 남지 않는지

# Worktree Split Decision

Single lane

admin auth/session, backend admin API, company/workplace/signup code contract, admin page UI가 동시에 움직인다. 공용 세션 구조와 auth flow가 바뀌므로 분리 구현은 merge risk가 높다.

# Commit Plan

1. `feat: admin 회사 온보딩 api 추가`
2. `feat: admin 웹 회사 등록 연동`
3. `test: admin 회사 온보딩 검증 추가`
4. `docs: admin 온보딩 정책 정리`

# Open Questions

- create 응답에서 employer signup code를 raw string으로 계속 반환할지, 생성 직후에만 보여줄지
- admin page에서 company list에 기존 remittance mock 섹션을 남길지 완전히 교체할지

# Assumptions

- 최소 MVP에서는 회사 생성 시 고용주 회사코드 1개를 자동 발급한다.
- 회사코드(`Company.companyCode`)는 사람이 식별하는 회사 코드이고, 고용주 회사코드는 employer signup 전용 비밀 코드다.
- 관리자 승인 플로우는 이번 범위 밖이며, 생성 즉시 고용주 회사코드는 사용 가능하다.
