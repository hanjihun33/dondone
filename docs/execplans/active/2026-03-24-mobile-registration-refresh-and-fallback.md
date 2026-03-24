## Source Inputs
- 사용자 재현 내용: 등록 코드 성공 후 홈/근무는 fallback 유지, 재로그인 후 메뉴는 `소속 정보 없음`
- 모바일 탐색 메모
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`
- backend 탐색 메모
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/LoginResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/MeResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/WorkerCompanyRegistrationResponse.java`

## Goal
근로자 앱에서 회사 등록 코드 입력 직후 실연동 상태가 즉시 반영되도록 하고, fallback 상태를 실제 데이터처럼 오해하지 않도록 홈/근무/문서 UX를 정리한다.

## In Scope
- 등록 코드 redeem 성공 후 모바일 원격 상태 즉시 재로딩
- 재로그인 후 메뉴에 회사/근무지 소속 정보 유지
- 홈 fallback 상태를 `회사 등록 필요` 중심으로 전환
- 근무/문서 등 상세 fallback 상태에 `가상 예시 데이터` 안내 추가
- 데모 seed에서 오해를 유발하는 날짜/문구 완화

## Out of Scope
- 송금/예치 지갑 상태 문구 세분화 (`잔액 확인중`, `지갑 준비 중`, `실패`)
- remittance/vault API 동작 변경
- 근무 데이터 자체 seed 구조 대개편
- 웹 이슈 큐/설정 화면 변경

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/LoginResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/MeResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- 필요 시 auth controller/integration test

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/demo/DemoSeedFactory.kt`

### Docs
- 본 실행 계획 문서

### Shared
- `AuthSession`이 담는 회사/근무지명 계약과 이를 소비하는 메뉴/홈/근무 화면의 의미 정렬

## Contract Changes
- backend `LoginResponse`와 필요 시 `MeResponse`에 `companyName`, `workplaceName` 추가 검토
- mobile login 파서가 위 필드를 세션에 저장하도록 동기화
- registration redeem 직후에는 기존 세션 갱신뿐 아니라 remote state 재로딩 호출

## Security Notes
- auth surface 변경이므로 기존 JWT/권한 규칙은 유지하고, 응답 필드 확장만 허용
- 회사/근무지명은 이미 worker-registration 응답에서 내려가므로 신규 민감정보 노출로 보지 않는다

## Maintainability Notes
- 메뉴가 `AuthSession`, 홈/근무가 `DemoState`/remote payload를 따로 보는 구조를 더 벌리지 않는다
- fallback 여부 판정은 화면마다 중복 계산하지 않고 UiModel 또는 session/viewmodel 레벨의 명시적 상태로 묶는 쪽을 우선 검토한다
- remittance/vault 상태 문구 개선 범위는 이번 slice에서 건드리지 않는다

## Implementation Steps
1. backend auth 응답 계약을 확인하고 login/me 응답에 회사명/근무지명 추가 여부를 결정한다.
2. mobile `BackendAuthRepository`가 로그인/restore/updateProfile/redeem 이후 세션 정보를 일관되게 저장하도록 정리한다.
3. `DemoSessionViewModel`의 등록 코드 성공 경로에서 `onAuthenticated(updatedSession)` 또는 동등한 전체 refresh 경로를 사용한다.
4. 홈 UiModel에서 fallback 근무 상태일 때 날짜/가상 기록 대신 `회사 등록 필요` 문구를 노출한다.
5. 근무/문서 fallback 화면에 `가상 예시 데이터` 배지 또는 안내문을 추가한다.
6. `DemoSeedFactory`의 `2026-03-28` 등 과도한 데모 날짜를 약화하거나 제거한다.
7. 관련 mobile/backend 테스트를 최소 범위로 추가/갱신한다.

## Test Plan
- backend
  - auth login 응답 필드 검증 integration test
  - worker registration 후 재로그인 시 소속 정보 유지 검증
- mobile
  - ViewModel 단위 검증 또는 최소 컴파일 검증
  - 등록 코드 성공 직후 홈/근무/메뉴 상태 갱신 수동 확인
- 수동
  - 회사 미등록 상태 홈 문구 확인
  - 회사 등록 직후 홈/근무 실연동 전환 확인
  - 재로그인 후 메뉴 소속 정보 유지 확인

## Review Focus
- 등록 직후 refresh가 중복 호출이나 세션 만료 처리 꼬임을 만들지 않는지
- login response 필드 확장이 기존 모바일/웹 클라이언트를 깨지 않는지
- fallback 안내가 실연동 상태와 혼동되지 않도록 조건이 정확한지
- 데모 문구 정리가 remittance/vault 영역까지 불필요하게 번지지 않는지

## Worktree Split Decision
Single lane

auth 응답 계약과 모바일 session/UI 상태가 동시에 움직여야 하므로 shared DTO와 상태 의미가 아직 같이 바뀐다. 병렬 lane으로 나누면 merge risk가 높다.

## Commit Plan
- `fix: 근로자 앱 등록 직후 상태 갱신 수정`
- `fix: 모바일 fallback 근무 안내 문구 정리`

## Open Questions
- 홈의 `회사 등록 필요`를 근무 미연결 전용으로만 노출할지, 원격 로드 실패에도 같은 문구를 재사용할지
- 메뉴 소속 정보는 login 응답 확장으로 해결할지, mobile에서 workproof payload로 세션을 보강할지

## Assumptions
- 이번 slice에서는 지갑 상태 문구 분기를 건드리지 않는다.
- 회사 등록 후 실연동 가능한 backend/workproof/contract 기반은 이미 이전 커밋으로 정리되었다.
