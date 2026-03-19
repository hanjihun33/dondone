## Source Inputs

- `docs/DonDone_PRD_v1.5.md`
  - Remittance allowlist / testnet flow (`6.6`, `7E`)
- `docs/DonDone_P0_API_Contract_v0.md`
  - Auth signup/me contract
  - Remittance recipients contract
- `docs/DonDone_P0_Functional_Spec_v0.md`
  - Auth in-scope / profile edit currently 미구현 상태
- 코드 탐색
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/auth/presentation/LoginScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`

## Goal

회원 전화번호를 DonDone의 정식 사용자 데이터로 추가하고, 모바일 회원가입/내 정보 수정/UI 상태를 이를 기준으로 갱신한다. 이어서 계좌·지갑 관리 화면의 `휴대폰 번호로 찾기`를 로컬 데모 디렉터리 대신 실제 백엔드 recipient search API와 연결한다.

## In Scope

- 백엔드 `User` 전화번호 저장 필드 추가 및 중복 검증
- auth signup/login/me 응답에 전화번호 반영
- auth `내 정보 수정` API 추가
- remittance `전화번호 기반 수신자 검색` API 추가
- 모바일 signup 폼에 전화번호 입력 추가
- 모바일 세션 저장/복원에 전화번호 반영
- 메뉴 화면에 `내 정보 수정` UI 추가
- 계좌·지갑 관리 화면의 전화번호 검색을 실제 API 기반으로 교체
- 관련 단위/통합 테스트와 계약 문서 업데이트

## Out of Scope

- SMS 본인인증
- 연락처 업로드/주소록 권한 연동
- 부분 일치 검색, 추천 검색
- 지갑 수정/삭제 기능
- 프로필 이미지, 추가 개인정보 필드
- Flyway 도입 같은 인프라성 스키마 관리 체계 변경

## Affected Modules

### Backend

- `auth/api`
- `auth/service`
- `auth/model`
- `auth/repo`
- `remittance/api`
- `remittance/service`
- `remittance/repo`
- `remittance/api/dto/**`
- `shared/exception`

### Mobile

- `data/auth/**`
- `data/remittance/**`
- `app/session/**`
- `feature/auth/presentation/LoginScreen.kt`
- `feature/menu/presentation/**`
- `feature/finance/presentation/AccountManageScreen.kt`
- `app/navigation/DonDoneNavGraph.kt`

### Docs

- `docs/DonDone_P0_API_Contract_v0.md`
- 필요 시 functional spec 메모 보강

### Shared

- 전화번호 정규화/마스킹 규칙
- auth/remittance 계약 동기화

## Contract Changes

- `POST /api/auth/signup`
  - request: `phoneNumber` 추가
- `POST /api/auth/login`
  - response data: `phoneNumber` 추가
- `GET /api/auth/me`
  - response data: `phoneNumber` 추가
- `PUT /api/auth/me`
  - request: `name`, `phoneNumber`
  - response data: 수정된 사용자 프로필
- `POST /api/remittance/recipients/search`
  - response: 검색 후보 목록
  - request: `phoneNumber`
  - 각 후보는 `candidateUserId`, `displayName`, `maskedPhoneNumber`, `walletAddressMasked`, `alreadyRegistered`

## Security Notes

- 전화번호는 서버 저장 전 숫자만 남기도록 정규화한다.
- 사용자 전화번호는 unique 제약을 둔다.
- remittance search는 query string 대신 request body로 전화번호를 받는다.
- remittance search 응답은 마스킹된 전화번호/주소만 제공하고, 실제 지갑 주소는 recipient 등록 시 서버가 `candidateUserId`로 해석한다.
- 자기 자신 검색 결과는 제외한다.
- wallet 미생성 사용자와 전화번호 미등록 사용자는 검색 대상에서 제외한다.
- SMS verification은 이번 범위 밖이므로 `verified` 상태를 도입하지 않고, 문서와 코드에 비인증 번호 기반 MVP임을 남긴다.

## Maintainability Notes

- 전화번호 정규화 규칙은 controller/view 레이어가 아니라 backend service 전용 유틸 또는 domain helper 한 곳에서 관리한다.
- 모바일 signup/profile/account-manage에서 같은 포맷팅 규칙이 반복되지 않도록 공용 helper로 묶는다.
- remittance search 응답은 add-recipient 전용 DTO로 두고 기존 recipient DTO와 섞지 않는다.
- 기존 demo fallback 흐름은 비인증 상태에서만 남기고, 인증 상태에서는 실제 API 결과만 사용하도록 경계를 분명히 한다.

## Implementation Steps

1. backend auth 계약 확장
   - `User`, `SignupRequest`, `LoginResponse`, `MeResponse`, `AuthService`, `ErrorCode`, `UserRepository` 수정
2. backend profile update 추가
   - `PUT /api/auth/me`, request DTO, service 로직, validation, 중복 처리 추가
3. backend remittance phone search 추가
  - search request/response DTO, repository query, service filtering, controller endpoint 추가
  - recipient 등록 시 `targetUserId` 해석 경로 추가
4. mobile auth/session 확장
   - `AuthSession`, `AuthRepository`, `BackendAuthRepository`, `AuthSessionStore`, `DemoSessionViewModel` 수정
5. mobile UI 반영
   - signup 폼 전화번호 입력
   - 메뉴 화면 내 `내 정보 수정` bottom sheet 추가
6. account-manage search 실연동 교체
   - 로컬 디렉터리 대신 remittance search API 사용
   - loading / empty / error 상태 유지
7. 테스트 및 문서 업데이트

## Test Plan

- Backend
  - auth signup validation/unit or integration
  - duplicate phone number conflict
  - `PUT /api/auth/me` success / duplicate phone / unauthorized
  - remittance search success / self excluded / wallet 없는 사용자 제외 / invalid phone 400
- Mobile
  - `DemoSessionViewModelTest` signup/profile update 검증
  - auth session store phone persistence
  - account-manage UI model or repository parsing 회귀
- Verification commands
  - backend: `cd apps/dondone-backend && ./gradlew test`
  - mobile: `cd apps/dondone-mobile/android && ./gradlew --no-daemon -Pkotlin.incremental=false :app:testDebugUnitTest :app:compileDebugKotlin`

## Review Focus

- auth 계약 변경이 모바일/백엔드 사이에서 누락 없이 반영됐는지
- 전화번호 정규화/중복 처리 위치가 한 군데로 유지되는지
- remittance search가 self/user-without-wallet/user-without-phone 케이스를 제대로 제외하는지
- 메뉴 프로필 수정 UI가 실패/제출 중 상태를 명확히 보여주는지
- 기존 비로그인 demo 흐름이 깨지지 않았는지

## Worktree Split Decision

Single lane

auth DTO, session payload, profile update, remittance search contract가 동시에 바뀌므로 병렬 lane으로 나누면 merge risk가 높다. shared entity와 mobile session contract가 함께 움직이는 작업이라 한 lane에서 일관되게 마무리하는 편이 안전하다.

## Commit Plan

- `feat(backend): add phone number to auth profile and recipient search`
- `feat(mobile): support phone-based signup profile edit and recipient lookup`
- `docs(contract): update auth and remittance phone number APIs`

## Open Questions

- 없음. 이번 슬라이스는 `SMS 인증 제외`를 전제로 MVP 범위로 고정한다.

## Assumptions

- 전화번호는 한국 휴대폰 형식 중심으로 받고 저장은 숫자만 유지한다.
- 가입과 프로필 수정 모두 전화번호는 필수다.
- 기존 사용자 중 phoneNumber가 없는 레코드는 테스트/로컬 환경 기준으로 허용하고, 검색 대상에서만 제외한다.
- 검색 응답에 실제 `walletAddress`를 포함해 바로 recipient 등록할 수 있게 한다.
