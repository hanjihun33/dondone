## Source Inputs
- `docs/DonDone_PRD_v1.5.md` WorkProof P0 방향
- `docs/DonDone_P0_API_Contract_v0.md` WorkProof check-in/check-out/list 계약
- `docs/DonDone_P0_Functional_Spec_v0.md` WorkProof 핵심 흐름 및 규칙
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/WorkProofController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`

## Goal
안드로이드 앱의 출근/퇴근 버튼이 백엔드 WorkProof API와 실제로 통신하고, 로그인된 사용자는 백엔드 기준 출퇴근 상태와 월 기록을 앱 UI에서 보도록 연결한다.

## In Scope
- 안드로이드에 WorkProof 원격 repository/state 추가
- 로그인 세션 기반 WorkProof 데이터 로드/재로드
- 출근/퇴근 버튼을 `/api/workproof/records/check-in`, `/api/workproof/records/check-out`에 연결
- 근무지/활성 계약 조회 및 실연동 전제 충족
- 활성 계약 부재 시 데모용 기본 계약 자동 생성
- 원격 응답을 기존 `DemoState.workproof` UI 모델로 변환
- WorkProof/Home 화면에 loading/error/success 상태 반영
- 필요한 범위의 모바일 테스트 추가

## Out of Scope
- 실제 GPS 권한 요청 및 실시간 위치 수집
- WorkProof 수정 이력/첨부 실연동
- 웹 대시보드 연동
- 백엔드 API shape 변경

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/dto/request/CreateContractRequest.java`
- 기존 WorkProof API 재사용, 가능하면 서버 계약 변경 없음

### Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModelFactory.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/workproof/presentation/WorkproofScreen.kt`
- 신규 `data/workproof/*`

### Docs
- 본 실행 계획 문서

### Shared
- 없음. 공통 API envelope과 auth session을 재사용

## Contract Changes
- 백엔드 API 계약 변경은 목표가 아니다.
- 모바일 내부 계약은 아래를 추가한다.
- `WorkproofRemoteState`: loading/unauthenticated/empty/error/content
- `WorkproofRemotePayload`: workplace, contract, record list, active record, 메시지
- 기존 `DemoState.workproof`는 UI 렌더용 shape로 유지하되 원격 payload에서 동기화한다.

## Security Notes
- WorkProof API는 JWT 보호 경로를 그대로 사용한다.
- 모바일은 기존 `AuthSession` access token을 재사용한다.
- 타인 근무지/기록 접근은 백엔드 소유권 검증에 맡기고 모바일은 workplaceId를 백엔드 조회 결과에서만 선택한다.
- 계약 자동 생성은 로그인 사용자 자신의 workplace에만 수행한다.

## Maintainability Notes
- 출퇴근 실연동은 `AdvanceRepository` 패턴처럼 별도 `WorkproofRepository`로 분리해 `DemoSessionViewModel`의 책임 폭증을 막는다.
- UI 전체를 원격 전용 상태로 재작성하지 않고, 기존 `DemoState.workproof`를 동기화하는 어댑터를 둬 현재 화면 구조를 유지한다.
- 데모 fallback과 실연동 로직 경계가 흐려지지 않도록 원격 로드/동기화/요청 수행을 repository와 viewmodel helper로 고정한다.

## Implementation Steps
1. 모바일 WorkProof 원격 payload/state/repository를 추가하고 근무지 조회, 현재 계약 조회, 기본 계약 생성, 기록 목록 조회, check-in/check-out 호출을 구현한다.
2. `DemoSessionViewModel`에 WorkProof 원격 상태와 로드/동기화 helper를 추가하고 로그인/로그아웃/초기 복구 시 함께 갱신한다.
3. 출근/퇴근 액션을 로그인 여부에 따라 분기해, 인증 시 원격 호출 후 목록 재조회 및 `DemoState.workproof` 동기화, 비인증 시 기존 데모 reducer 유지로 연결한다.
4. Home/WorkProof UI model과 화면에 loading/error/success 텍스트를 추가해 실연동 상태를 명시한다.
5. 최소 단위 테스트를 추가하고 앱/백엔드 검증을 실행한다.

## Test Plan
- 모바일 unit test
- `DemoSessionViewModel`의 WorkProof 로드/출근/퇴근 성공 경로
- 계약 자동 생성 또는 빈 상태 fallback 경로
- 인증 만료/에러 메시지 반영 경로
- 가능하면 백엔드 기존 WorkProof integration test 재실행

## Review Focus
- 로그인 직후/복구 직후 WorkProof와 Advance 원격 상태가 서로 충돌하지 않는지
- 활성 계약 자동 생성이 불필요하게 반복되지 않는지
- check-in/check-out 후 UI가 원격 기준으로 즉시 갱신되는지
- 비인증 데모 흐름이 유지되는지
- 오류 메시지와 로딩 상태가 Home/WorkProof에서 모두 일관되게 보이는지

## Worktree Split Decision
- Single lane

공유 DTO, 인증 세션, ViewModel, WorkProof UI 모델이 동시에 움직이므로 병렬 분할 시 충돌 위험이 높다. 한 lane에서 모바일 원격 계층과 UI 동기화를 함께 마무리한다.

## Commit Plan
- `feat/mobile-workproof-remote-state`
- `test/mobile-workproof-session`

## Open Questions
- 기본 계약 자동 생성의 기준 금액을 어떤 데모 값으로 둘지
- 현재 좌표를 데모 seed 좌표로 계속 보낼지, workplace 좌표로 정렬할지

## Assumptions
- 이번 작업은 P0 데모 범위이므로 기본 계약은 `DAILY`, `96,000 KRW`, `480분`, `effectiveFrom=오늘`로 자동 생성한다.
- 실제 GPS 권한 연동이 없으므로 check-in/check-out 요청 좌표는 현재 앱이 보유한 `workproof.currentLatitude/currentLongitude`를 사용한다.
- 원격 workplace 좌표를 앱 상태에 동기화해 반경 판정을 맞춘다.
