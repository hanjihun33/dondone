# Source Inputs

## Status

- backend implementation: done
- mobile company registration flow migration: done
- backend targeted tests: done
- backend full test: done
- mobile compile verification: done

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/AuthController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/EmploymentMembership.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/EmploymentMembershipRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/admin/service/AdminEmployerCompanyService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employerauth/model/EmployerSignupCode.java`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/AuthRepository.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`
- `docs/web/auth-and-role-policy.md`
- `docs/web/implementation-slices.md`
- 현재 합의된 정책:
  - 관리자: 회사 생성, 고용주 가입 코드 발급
  - 고용주: 근로자 등록 코드 발급
  - 근로자: 앱에서 등록 코드를 입력해 회사/사업장에 소속 등록
  - 앱의 기존 `companyCode` 저장은 실제 소속 등록이 아님

# Goal

고용주가 발급한 `근로자 등록 코드`를 근로자가 앱에서 입력하면 서버가 코드를 검증하고 `EmploymentMembership`를 생성하도록 구현한다. 기존 앱의 `companyCode` 저장 흐름은 실제 등록 흐름으로 대체하거나 축소해서, 앱의 “회사 등록”이 실제 소속 생성과 일치하도록 맞춘다.

# In Scope

- backend `WorkerRegistrationCode` 도메인 추가
- employer 권한으로 근로자 등록 코드 발급/목록/폐기 API 추가
- worker 권한으로 등록 코드 redeem API 추가
- redeem 성공 시 `EmploymentMembership` 생성 또는 재활성화 규칙 추가
- mobile 회사 코드 입력 UI를 등록 코드 입력 흐름으로 전환
- mobile에서 redeem 성공 후 회사명/사업장명 표시
- 관련 문서와 테스트 추가

# Out of Scope

- 관리자 회사 생성/고용주 가입 코드 흐름 재설계
- 근로자 앱 회원가입 자체 변경
- 다중 사업장 선택 UI
- 지각/휴가/결근 정책
- `NEEDS_REVIEW` resolve command
- 첨부파일 preview/download

# Affected Modules

## Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`

## Mobile

- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/DonDoneApp.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/AuthRepository.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`
- 필요 시 auth/session 모델 파일

## Docs

- `docs/web/auth-and-role-policy.md`
- `docs/web/implementation-slices.md`
- 필요 시 worker/app 관련 정책 문서

## Shared

- 없음

# Contract Changes

- employer API 추가
  - `POST /api/employer/worker-registration-codes`
  - `GET /api/employer/worker-registration-codes`
  - `POST /api/employer/worker-registration-codes/{codeId}/revoke`
- worker API 추가
  - `POST /api/auth/me/worker-registration-code`
- worker redeem 응답에는 최소 아래 정보 포함
  - `companyId`
  - `companyName`
  - `workplaceId`
  - `workplaceName`
  - `companyCode`
- 앱의 기존 `PUT /api/auth/me/company-code`는 더 이상 “회사 등록” 의미로 쓰지 않음

# Security Notes

- 근로자 등록 코드는 `EmployerSignupCode`와 별도 저장소로 분리한다.
- raw code는 평문 조회가 필요한 운영 요구가 없다면 hash 비교를 기본으로 하고, employer 재조회 정책이 필요할 때만 encrypted code 저장을 검토한다.
- 발급/목록/폐기 API는 `ROLE_EMPLOYER`만 허용한다.
- redeem API는 인증된 worker(`ROLE_USER`)만 허용한다.
- employer scope의 `companyId/defaultWorkplaceId` 밖으로 코드를 발급하지 않도록 `EmployerAccessScopeService`로 검증한다.
- redeem 시 revoked code, 존재하지 않는 code, scope mismatch, 중복 active membership 규칙을 명시적으로 차단한다.

# Maintainability Notes

- `EmployerSignupCode` 패턴을 최대한 재사용하되, 고용주 가입 코드와 근로자 등록 코드를 같은 엔티티로 섞지 않는다. 발급 주체와 redeem 주체가 달라 책임 경계를 분리해야 한다.
- 앱의 `companyCode` 문자열 저장은 도메인상 의미가 약하므로, 이번 작업 후 “실제 등록”과 혼동되지 않게 축소하거나 제거한다.
- membership 생성 규칙은 한 서비스에 모아두고, controller/auth glue와 분리한다.

# Implementation Steps

1. backend에 `WorkerRegistrationCode` 엔티티/리포지토리/서비스를 추가한다.
2. employer controller에 발급/목록/폐기 API를 추가한다.
3. worker redeem request/response DTO와 redeem service를 추가한다.
4. redeem 시 `EmploymentMembership` 생성 또는 기존 membership 재활성화 규칙을 구현한다.
5. mobile `회사 코드 입력` UI를 `근로자 등록 코드 입력`으로 바꾸고, 호출 endpoint를 redeem API로 전환한다.
6. redeem 성공 시 앱 세션/화면에 회사명과 사업장명을 반영한다.
7. 기존 `companyCode` 업데이트 흐름의 UI 문구와 호출을 정리한다.
8. 문서와 테스트를 갱신한다.

# Test Plan

- backend targeted tests
  - employer 발급/목록/폐기 integration test
  - worker redeem integration test
  - revoked/invalid code 차단
  - duplicate active membership 처리
  - employer token으로 worker redeem 불가, worker token으로 employer issue 불가
- mobile verification
  - `cd apps/dondone-mobile/android && .\gradlew.bat :app:compileDebugKotlin`
- backend verification
  - 변경 범위 targeted tests 우선
  - 마지막에 `cd apps/dondone-backend && .\gradlew.bat test`

# Review Focus

- 앱의 “회사 등록” 의미가 실제 membership 생성과 일치하는지
- employer scope 바깥 사업장 코드 발급이 차단되는지
- redeem 성공 후 회사/사업장 정보가 앱에 일관되게 반영되는지
- 기존 `companyCode` 저장 contract와의 충돌이 남지 않는지
- membership 중복/재등록 규칙이 명확한지

# Worktree Split Decision

Single lane

backend DTO, authz, shared entity, mobile 호출 경로와 UI 문구가 동시에 바뀐다. `EmploymentMembership`와 auth contract가 같이 움직여 merge risk가 높으므로 single lane으로 처리한다.

# Commit Plan

1. `feat: add employer worker registration code api`
2. `feat: redeem worker registration code from mobile`
3. `test: cover worker company registration flow`
4. `docs: update worker company registration policy`

# Open Questions

- 등록 코드를 회사 단위로 발급할지, 기본 사업장 단위로 발급할지
- 기존 active membership이 다른 회사/사업장에 있을 때 차단할지, 병행 허용할지
- employer가 현재 active code를 다시 볼 수 있어야 하는지

# Assumptions

- MVP 기준 등록 코드는 employer의 `defaultWorkplaceId`에 묶는다.
- 같은 회사/같은 사업장 active membership이 이미 있으면 새로 만들지 않고 성공 응답만 반환한다.
- 기존 앱의 `companyCode` 문자열은 실제 등록 수단이 아니므로, 이번 작업 후 등록 UI 의미는 전부 새 redeem API로 옮긴다.
