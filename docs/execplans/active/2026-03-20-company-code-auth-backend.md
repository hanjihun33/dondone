# Source Inputs

- 사용자 요청: 회사코드를 `DONDONE2026` 형식으로 정하고 백엔드에도 구현
- [AGENTS.md](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/AGENTS.md)
- [apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/AuthController.java](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/AuthController.java)
- [apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java)
- [apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java)
- [apps/dondone-backend/src/test/java/com/workproofpay/backend/auth/AuthProfileIntegrationTest.java](/mnt/c/Users/SSAFY/Desktop/workspace/S14P21C202/apps/dondone-backend/src/test/java/com/workproofpay/backend/auth/AuthProfileIntegrationTest.java)

# Goal

회원가입 계정에 회사코드를 저장하고, 하이픈 없는 영문 대문자+숫자 형식으로 검증하며, 로그인/내 정보 응답에서도 해당 값을 노출한다.

# In Scope

- `users` 엔티티에 회사코드 필드 추가
- `SignupRequest` 에 회사코드 필드와 검증 규칙 추가
- `AuthService` 에 회사코드 정규화 및 저장 로직 추가
- `LoginResponse`, `MeResponse` 에 회사코드 반영
- 관련 auth 통합 테스트 갱신
- 개발용 시드 사용자 회사코드 설정

# Out of Scope

- 회사코드 기반 조직/근무지 매핑 로직
- 회사코드 변경 API
- 모바일과 백엔드 연동 wiring
- 별도 조직 엔티티 도입

# Affected Modules

## Backend

- `auth/api`
- `auth/service`
- `auth/model`
- `shared/bootstrap`
- auth integration tests

## Mobile

- 없음

## Docs

- 본 실행 계획 문서

## Shared

- 없음

# Contract Changes

- `POST /api/auth/signup` 요청 본문에 `companyCode` 필드 추가
- `POST /api/auth/login` 응답에 `companyCode` 필드 추가
- `GET /api/auth/me` 응답에 `companyCode` 필드 추가
- 형식 규칙: 서버 저장값 기준 `^[A-Z0-9]{6,12}$`

# Security Notes

- 인증/인가 규칙은 변경하지 않음
- 회사코드는 민감정보로 취급하지 않지만 입력값 검증을 강제함
- 하이픈/공백/특수문자는 서버에서 허용하지 않음

# Maintainability Notes

- 회사코드 정규화 규칙은 `AuthService` 에 흩어놓지 말고 전용 helper 로 캡슐화
- `User` 생성 팩토리와 응답 매핑 모두 회사코드 필드를 일관되게 반영
- 조직 도메인을 아직 도입하지 않으므로 이번 변경은 `User` 단일 소유로 유지

# Implementation Steps

1. `User` 엔티티와 생성 팩토리에 `companyCode` 필드 추가
2. `SignupRequest` 에 필드/Swagger/Validation 추가
3. `AuthService` 에 회사코드 정규화 및 검증 로직 추가
4. `LoginResponse`, `MeResponse`, 개발 시드 사용자에 회사코드 반영
5. auth 통합 테스트에서 signup/login/me 계약 검증 추가

# Test Plan

- `AuthProfileIntegrationTest` 에 signup/login/me 회사코드 응답 검증
- 회사코드 유효성 실패 케이스 추가
- `AuthLoginIntegrationTest` 에 login 응답 회사코드 검증
- 가능하면 `./gradlew test --tests ...auth...`

# Review Focus

- 서버가 소문자 입력을 일관되게 대문자로 정규화하는지
- signup validation 과 service normalization 이 중복 충돌 없이 동작하는지
- login/me 응답 계약이 테스트와 Swagger 설명에 맞는지
- 기존 사용자 생성 코드 경로가 모두 새 필드를 채우는지

# Worktree Split Decision

Single lane

인증 DTO, 서비스, 엔티티, 응답 계약이 함께 움직이므로 병렬 분리가 오히려 충돌 위험을 높인다.

# Commit Plan

- `feat(backend): add company code to auth profile contracts`

# Open Questions

- 없음

# Assumptions

- 회사코드는 당분간 조직 식별용 문자열만 저장하며 별도 참조 무결성은 요구하지 않음
- 최소 길이는 예시와 모바일 UX를 고려해 6자로 둔다
- 기존 프로필 수정 API에서는 회사코드 변경을 허용하지 않는다
