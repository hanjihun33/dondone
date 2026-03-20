# Source Inputs

- `apps/dondone-web/src/pages/auth/LoggedOutPage.tsx`
- `apps/dondone-web/src/pages/auth/SignUpPage.tsx`
- `apps/dondone-web/src/shared/api/employer.ts`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/api/EmployerAuthController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/service/EmployerAuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/EmployerSignupCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/EmployerProfile.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/EmployerAuthIntegrationTest.java`
- `docs/web/auth-and-role-policy.md`
- 사용자 결정:
  - 관리자 기능은 아직 없지만 이후 붙일 수 있어야 함
  - 지금은 고용주 회원가입을 닫기 위해 테스트 가능한 임시 회사 코드 1개를 제공
  - 나중에 관리자 회사 등록/코드 발급 기능이 생기면 같은 저장소와 같은 signup 흐름에 연결

# Goal

고용주 회원가입을 `임시 회사 코드 + 담당자명 + 이메일 + 비밀번호` 흐름으로 마무리한다. 현재 관리자 발급 기능은 없으므로 seed/manual bootstrap 코드 1개를 사용해 기능 테스트를 가능하게 하고, 이후 관리자 기능이 생기면 같은 `EmployerSignupCode` 저장소와 `POST /api/employer-auth/signup` 계약에 바로 이어붙을 수 있게 만든다.

# In Scope

- `POST /api/employer-auth/signup` 계약을 `companyCode + displayName + email + password` 기준으로 수정
- signup 성공 시 `User.name`, `EmployerProfile.displayName`에 담당자명 저장
- web `/signup` 화면을 회사 코드 입력형으로 전환
- 로그인 화면에서 `/signup` 진입이 실제로 동작하도록 UX 정리
- dev seed에 테스트용 임시 회사 코드 유지
- auth/signup 문서와 테스트 갱신

# Out of Scope

- 관리자 웹/관리자 API 구현
- 관리자 승인 기반 가입 요청 플로우
- worker/mobile 회사 등록 코드 플로우
- employer profile 편집 화면
- invitation token 제거 마이그레이션

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/service/EmployerAuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employerauth/EmployerAuthIntegrationTest.java`

## Mobile

- 없음

## Docs

- `docs/execplans/active/2026-03-20-web-employer-signup-code-flow.md`
- `docs/web/auth-and-role-policy.md`
- `docs/web/implementation-slices.md`

## Shared

- 없음

# Contract Changes

- `POST /api/employer-auth/signup`
- request body:
  - `companyCode`
  - `displayName`
  - `email`
  - `password`
- signup 성공 응답은 기존 employer login과 동일한 `EmployerAuthResponse`
- web `/signup` visible field:
  - `회사 코드`
  - `담당자명`
  - `이메일`
  - `비밀번호`
  - `비밀번호 확인`

# Security Notes

- 현재 회사 코드는 관리자 발급 기능 대신 seed/manual bootstrap으로 준비하지만, 검증은 계속 DB row + hash 기반으로 유지한다.
- raw code는 DB에 저장하지 않고 hash로 비교한다.
- 이메일 canonicalization과 중복 검사는 기존 employer auth 정책을 그대로 따른다.
- 회사 코드가 유효하더라도 company/workplace binding 검증은 계속 수행한다.

# Maintainability Notes

- `EmployerSignupCode`는 임시 구현이 아니라 이후 관리자 발급/재발급/폐기 기능의 정식 저장소로 계속 사용한다.
- signup 화면은 지금부터 query string 의존을 제거해, 관리자 기능이 붙어도 입력 UX만 최소 수정으로 유지되게 한다.
- `User.name`과 `EmployerProfile.displayName`은 모두 담당자명을 저장해, 이후 employer profile 편집 기능이 생겨도 일관된 출발점을 갖게 한다.

# Implementation Steps

1. `EmployerSignupRequest`를 `companyCode + displayName` 기준으로 수정한다.
2. `EmployerAuthService.signup()`에서 회사 코드 hash 조회 후 employer 계정과 profile에 담당자명을 저장하도록 바꾼다.
3. `DevEmployerInitializer`의 임시 회사 코드를 테스트 가능한 값으로 유지한다.
4. `SignUpPage`를 회사 코드 입력형으로 바꾸고 더 이상 query `code`에 의존하지 않게 한다.
5. 로그인 화면에서 회원가입 진입이 현재 정책과 충돌하지 않도록 안내 문구를 정리한다.
6. auth policy / implementation slice 문서를 현재 계약에 맞게 갱신한다.
7. employer auth integration test와 web typecheck를 실행한다.

# Test Plan

- backend targeted tests
  - `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employerauth.EmployerAuthIntegrationTest`
- 검증 포인트
  - valid companyCode + displayName 입력 시 employer signup 성공
  - invalid/revoked companyCode 실패
  - mixed-case email duplicate 실패 유지
  - signup 후 profile displayName이 담당자명으로 반영
- frontend
  - `cd apps/dondone-web && .\\node_modules\\.bin\\tsc.cmd -b`

# Review Focus

- companyCode 기반 signup과 기존 invitation token 흐름이 충돌 없이 공존하는지
- signup 성공 후 employer scope가 즉시 활성화되는지
- 로그인 화면에서 `/signup` 진입이 실제 동작하는지
- 이후 관리자 기능이 들어와도 같은 테이블/같은 API에 이어붙일 수 있는지

# Worktree Split Decision

Single lane

auth DTO, service, seed, web signup, 문서가 모두 같이 움직이고 공용 계약이 바뀌므로 분리 구현은 merge risk가 높다. 특히 signup request shape와 onboarding 문구가 동시에 바뀌기 때문에 한 lane에서 닫는 편이 안전하다.

# Commit Plan

1. `feat: finish employer signup with temporary company code`
2. `test: cover employer company code signup flow`
3. `docs: update employer signup policy`

# Open Questions

- 로그인 화면에 개발용 임시 회사 코드를 노출할지
- 이후 관리자 기능에서 회사 코드 발급/재발급/폐기를 별도 admin lane으로 분리할지

# Assumptions

- 현재는 관리자 기능이 없으므로 dev/manual bootstrap 코드 1개로 기능 테스트를 수행한다.
- 이후 관리자 기능은 같은 `EmployerSignupCode` 저장소와 `POST /api/employer-auth/signup` 계약 위에 이어붙인다.
- worker 회사 등록 코드는 별도 lane으로 유지한다.
