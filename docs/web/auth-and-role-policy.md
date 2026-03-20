# Auth And Role Policy

## 목적
- 웹 전용 로그인/회원가입 흐름을 정의하되, 인증 인프라 중복을 막는다.

## 기본 원칙
- 인증 인프라는 공통으로 재사용할 수 있다.
- 역할별 프로필과 역할별 API는 분리한다.
- 웹 전용 auth 흐름이 기존 앱 auth contract를 깨지 않게 한다.

## 현재 코드 근거
### UserRole
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/UserRole.java`
  - 현재 값은 `USER`, `EMPLOYER`, `ADMIN`이다.
  - employer 전용 보호 경계는 `EMPLOYER` role을 기준으로 분리한다.

### User
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
  - `register()`는 worker signup 기본값으로 `UserRole.USER`를 유지한다.
  - employer 계정 생성은 invitation accept 서비스가 별도 경로로 처리한다.
  - 이메일은 공통 canonicalization 규칙(`trim + lowercase`)으로 저장/조회한다.

### SecurityConfig
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `/api/employer-auth/**`는 공개, `/api/employer/**`는 `ROLE_EMPLOYER`, 기존 `/api/**`는 `USER/ADMIN`으로 분리했다.
  - employer token으로 worker endpoint, worker token으로 employer endpoint 접근을 허용하지 않는다.

## 선택지 비교
### 선택지 A. worker/employer auth 시스템 완전 분리
- 장점: 경계가 명확하다.
- 단점: JWT, password, security, 계정 운영이 중복된다.
- 판단: 현재 단계에서는 과하다.

### 선택지 B. 공통 auth + 역할별 profile/API 분리
- 장점: 인증 중복을 막고 경계도 유지할 수 있다.
- 단점: role 정책과 membership 모델이 명확해야 한다.
- 판단: 현재 단계의 권장안이다.

### 선택지 C. 기존 `User`만 확장해서 모든 흐름을 같은 DTO로 처리
- 장점: 처음에는 빠를 수 있다.
- 단점: signup/login/권한/도메인 의미가 섞인다.
- 판단: 유지보수 리스크가 커서 비권장이다.

## 현재 단계 권장안
- 선택지 B를 실제 구현 기준으로 채택했다.
- 공통 auth 인프라는 재사용하고 employer web은 별도 DTO, controller, service로 격리했다.
- 역할별 profile과 권한 검사는 endpoint/service 레벨에서 분리하고 `EmployerProfile + EmploymentMembership`을 employer authz source of truth로 고정했다.
- 기존 worker auth contract는 유지하되, 공통 이메일 정책은 worker/employer 모두 같은 canonicalization 규칙으로 통일했다.

## 분리 기준
### 분리 대상
- signup DTO
- login DTO
- 가입 후 후처리
- role check
- 웹 전용 프로필 조회

### 공통 재사용 대상
- password hashing
- JWT 발급/검증
- security filter
- 공통 에러 포맷

## 역할 후보
- worker
- employer
- admin

## 역할 정책 원칙
- role은 UI 표시값이 아니라 access control 기준이다.
- employer는 worker endpoint에 접근할 수 없어야 한다.
- worker는 employer endpoint에 접근할 수 없어야 한다.
- admin은 운영 목적 범위에서만 예외를 둔다.

## 가입 흐름에서 결정해야 할 것
- employer signup이 공개 가입인지
- 회사 코드 또는 초대 기반인지
- 최초 employer가 company를 생성하는지
- worker와 employer 겸용 계정을 허용하는지

## 현재 단계 보수적 권장안
- employer는 공개 회원가입보다 초대 또는 회사 코드 기반 가입을 우선 고려
- worker/employer 겸용은 MVP에서 허용하지 않음
- 공통 이메일 계정 정책은 `trim + lowercase` canonicalization으로 구현 완료
- `SecurityConfig`에는 `/api/employer-auth/**`, `/api/employer/**`, 기존 `/api/**` 분리 규칙을 이미 반영했다.

## MVP 기본 가정
- employer 가입은 `공개 회원가입`으로 열지 않는다.
- 초기 employer 계정은 `관리자 또는 내부 운영자 초대 기반`으로 만든다.
- 초대 수락 시 employer account를 생성하고 company와 연결한다.
- worker와 employer 겸용 계정은 MVP에서 허용하지 않는다.

## 초대 토큰 보안 계약
- employer 초대 토큰은 `1회성`이어야 한다.
- employer 초대 토큰은 `만료 시각`을 가져야 한다.
- 운영자 또는 관리자는 아직 사용되지 않은 토큰을 `폐기(revoke)`할 수 있어야 한다.
- 초대 토큰은 최소한 `invitee email`, `companyId`, `defaultWorkplaceId`, `role=EMPLOYER`에 바인딩되어야 한다.
- DB row 기반으로 관리하되, 저장 값은 raw bearer token이 아니라 server-side hash여야 한다.
- 초대 수락 시 요청 email, token 바인딩 정보, target company, target default workplace binding이 불일치하면 계정을 만들지 않는다.
- 이미 사용된 토큰, 만료된 토큰, 폐기된 토큰은 모두 동일하게 실패 처리한다.

## 왜 이 가정을 택하는가
- 현재 `User.register()`는 일반 `USER` 가입 흐름만 전제하고 있다.
- 현재 `SecurityConfig`에는 employer 전용 공개 endpoint 설계가 없다.
- 공개 employer signup을 먼저 열면 회사 검증, 회사 생성, 소속 승인, 권한 오남용 방지까지 같이 설계해야 한다.
- 반면 초대 기반 가입은 웹 MVP 범위를 통제하면서도 권한 모델을 덜 흔든다.

## employer 가입 흐름 초안
1. 운영자 또는 관리자 계정이 employer 초대를 생성한다.
2. 초대에는 `companyId` 또는 그에 준하는 조직 식별자가 포함된다.
3. 초대 수락 전용 endpoint에서 employer signup DTO를 받는다.
4. 계정 생성 후 `EmployerProfile`과 `Company` 연결을 만든다.
5. 이후 `/api/employer/*` 접근은 employer role 또는 authority로 제한한다.

## 공개 endpoint 가정
- 현재 worker용 `/api/auth/signup`, `/api/auth/login`은 유지한다.
- employer용 공개 endpoint는 별도 경로로 분리한다.
- 현재 구현
  - `POST /api/employer-auth/invitations/accept`
  - `POST /api/employer-auth/login`
- 보호 endpoint
  - `GET /api/employer/profile`
- employer invitation accept endpoint는 token의 `1회성`, `만료`, `폐기`, `companyId/defaultWorkplaceId/role/email 바인딩`을 모두 검증해야 한다.
- employer login endpoint는 단순 authenticated 여부만이 아니라 employer profile 활성 상태와 회사-사업장 연결 상태도 함께 확인해야 한다.

## 추후 재검토 조건
- 실제 요구사항이 self-service employer signup을 요구할 때
- 한 회사에 첫 employer를 외부에서 직접 생성해야 할 때
- worker/employer 겸용 계정이 제품 요구사항으로 확정될 때

## 검증 포인트
- 같은 이메일로 worker/employer 겸용 허용 여부
- 사업주 초대 또는 회사 코드 기반 가입 여부
- employer가 worker API에 접근하지 못하도록 충분히 막히는지
- worker가 employer API에 접근하지 못하도록 충분히 막히는지
- 초대 토큰 재사용, 만료, 폐기, companyId/defaultWorkplaceId 불일치, email 불일치가 모두 차단되는지
- company와 default workplace가 실제로 같은 조직 축에 묶이는지
- 이메일 canonicalization 때문에 대소문자만 다른 중복 계정이 생기지 않는지

## 금지할 것
- 웹용 auth를 별도 JWT 시스템으로 중복 구현
- 역할만 다른데 완전히 같은 DTO를 억지로 재사용
- 권한 검증 없이 UI 분기만으로 접근 제한 처리
- membership 없이 role만 보고 회사 범위를 추론하는 구조

## 문서적으로 이미 확정된 운영 원칙
- 기존 앱 API contract는 당장 바꾸지 않는다.
- 웹 요구사항을 맞추기 위해 기존 auth를 전면 리팩터링하지 않는다.
- employer 기본 scope는 `EmployerProfile.defaultWorkplaceId`에 직접 둔다.
- invitation token은 DB row 기반으로 관리하되 hash 저장으로 운영한다.
- employer authz source of truth는 `EmployerProfile + EmploymentMembership`이다.
- 기존 app auth 변경이 필수처럼 보이면 재스코프 대상으로 본다.
