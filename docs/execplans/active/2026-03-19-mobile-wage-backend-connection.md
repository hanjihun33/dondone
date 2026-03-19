# Source Inputs
- Root guidance:
  - `AGENTS.md`
- Skill guidance:
  - `.agents/skills/execplan-writer/SKILL.md`
- Product docs:
  - `docs/DonDone_PRD_v1.5.md`
- Existing wage planning/docs:
  - `docs/execplans/active/2026-03-16-wage-verification-contract.md`
  - `docs/execplans/active/2026-03-16-wage-verification-refactor.md`
  - `docs/execplans/active/2026-03-13-workproof-wage-advance-backend-start.md`
- Backend current code:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/service/DocumentsService.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`
- Mobile current code:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModelFactory.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/WageEstimator.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/AdvanceRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/WorkproofRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/RemittanceRepository.kt`

# Goal
모바일 급여점검 화면을 데모 상태 전용 계산에서 벗어나 백엔드 `wage` API와 실제로 연결한다. 1차 구현에서는 `월간 요약/참고용 추정 조회 -> 실제 입금 등록 -> verification 생성/상세 조회`까지를 실연동 최소선으로 고정하고, 급여점검 화면이 로딩/에러/인증 상태를 명시적으로 다루도록 만든다.

# In Scope
- 모바일 `data/wage` 원격 계층 신설
- 백엔드 `wage` API를 호출하는 모바일 repository 구현
- 모바일 session 계층에 wage remote state 추가
- 급여 화면을 remote-first UI 모델로 전환
- 실제 입금 등록을 `POST /api/wage/deposits` 로 연결
- 차액 확인을 `POST /api/wage/verifications` 및 `GET /api/wage/verifications/{id}` 로 연결
- 급여 화면의 로딩/에러/인증 필요/새로고침 UI 추가
- 홈/금융 홈의 급여 요약을 remote wage 결과 우선 반영하도록 최소 수정

# Out of Scope
- 백엔드 wage API 스펙 변경
- Wage Shield 후속 액션 전체 구현
  - Proof Pack 생성
  - Claim Kit 생성
  - Instant Claim 제출 준비
- 급여명세서/계약서 파싱
- 웹 급여점검 실연동
- 급여점검 전용 화면 추가 분리 또는 네비게이션 재설계
- DemoState 전체 제거
- Room/DataStore 등 별도 로컬 캐시 도입

# Affected Modules
## Backend
- 계약 변경 없음. 기존 API를 모바일이 소비한다.
- 참조 대상:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

## Mobile
- 신규:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/wage/WageRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/wage/BackendWageRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/wage/WageRemoteState.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/wage/WagePayloads.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/wage/WageUnauthorizedException.kt`
- 수정:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModelFactory.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`

## Docs
- `docs/execplans/active/2026-03-19-mobile-wage-backend-connection.md`

## Shared
- 모바일 공통 HTTP helper 재사용:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remote/BackendApiSupport.kt`
- 공통 `ApiResponse<T>` envelope 파싱 규칙 재사용

# Contract Changes
- 백엔드 계약 변경 없음. 모바일이 아래 endpoint를 소비한다.
  - `GET /api/wage/monthly-summary`
  - `GET /api/wage/estimate`
  - `POST /api/wage/deposits`
  - `POST /api/wage/verifications`
  - `GET /api/wage/verifications/{verificationId}`
- 모바일 내부 계약은 새로 생긴다.
  - `WageRemoteState`는 `LOADING`, `UNAUTHENTICATED`, `EMPTY`, `ERROR`, `CONTENT` 상태를 가져야 한다.
  - `WagePayloads`는 backend DTO shape를 최대한 그대로 보존하고, UI model 단계에서만 문구/형태 변환을 한다.
- `DemoState.wage` 값은 즉시 삭제하지 않는다.
  - 1차에서는 fallback 및 데모 seed 유지 용도로 남긴다.
  - remote wage payload가 있을 때 화면은 remote 값을 우선 사용한다.

# Security Notes
- 모든 wage API는 기존과 동일하게 JWT 기반 인증이 필요하다.
- 모바일 wage repository는 `401` 을 `WageUnauthorizedException` 으로 명시 분기해 session 정리 흐름과 연결한다.
- 급여점검 화면은 인증이 끊겼을 때 데모 성공 화면처럼 보이지 않도록 `unauthenticated` gate를 우선 노출한다.
- verification 상세는 본인 소유 데이터만 조회 가능하다는 backend 전제를 유지한다. 모바일에서 다른 사용자 리소스 재시도나 id 추측 흐름은 추가하지 않는다.
- 급여 결과 문구는 PRD 원칙대로 `참고용 추정`, `최종 급여 확정 아님`, `근거 기반 이상 탐지` 프레이밍을 유지한다.

# Maintainability Notes
- wage 실연동도 `advance/remittance/workproof` 와 동일한 구조를 따른다.
  - `Repository`
  - `RemoteState`
  - `ViewModel load/refresh`
  - `UI model mapping`
- `WageUiModel.kt` 에서 backend JSON shape를 직접 다루지 않는다. payload -> UI model 변환 경계는 명확히 유지한다.
- `DemoSessionReducer.kt` 의 `setActualDeposit` 는 즉시 삭제하지 않고, 화면에서 직접 호출하지 않는 fallback/debug 경로로만 남긴다.
- 홈/금융 홈 수정은 wage 화면 실연동을 보조하는 범위로 제한한다. 급여 요약 카드 전체 재설계는 이번 범위에 넣지 않는다.
- verification 생성 후 상태를 별도 상세 화면로 분리하지 않는다. 1차에서는 기존 wage 화면 내 갱신으로 마무리해 복잡도를 제한한다.

# Implementation Steps
1. `data/wage` 패키지를 신설하고 repository, remote state, payload 모델, unauthorized exception을 추가한다.
2. `BackendWageRepository` 에서 backend `wage` endpoint 5개를 호출하도록 구현한다.
3. `DemoSessionViewModel` 에 `wageRepository`, `_wageRemoteState`, `wageRemoteState`, `refreshWageRemoteState()`, `loadWageRemoteState(session)` 를 추가한다.
4. 인증 복원 및 로그인 성공 이후 `advance/workproof/remittance` 와 같은 타이밍에 wage remote state도 로드한다.
5. 실제 입금 입력 액션을 `submitWageDeposit(amount)` 같은 suspend 흐름으로 바꾸고 성공 시 wage remote state를 다시 불러온다.
6. verification 생성 액션을 `createWageVerification()` 로 추가하고, 생성 성공 시 `verificationId` 기준 상세를 다시 조회해 remote state를 최신화한다.
7. `DonDoneNavGraph.kt` 에서 `wageRemoteState` 를 collect 하고 `WageScreen` 에 remote state, retry, submit callback을 전달한다.
8. `WageUiModel.kt` 를 remote-first로 바꾸고 다음 우선순위를 강제한다.
   - remote `verification detail`
   - remote `monthly summary + estimate`
   - local `DemoState + WageEstimator` fallback
9. `WageScreen.kt` 에 성공 상태 외 분기를 추가한다.
   - loading
   - unauthenticated
   - error
   - empty
   - retry
10. 급여 화면의 primary CTA를 navigation-only 흐름에서 verification 생성 가능 흐름으로 조정한다.
11. `HomeUiModel.kt`, `FinanceHomeUiModel.kt` 에서 remote wage 결과가 있을 때 차액/실입금/상태 요약을 우선 반영한다.
12. 모바일 빌드와 수동 플로우를 검증한다.

# Test Plan
- 모바일 정적 검증:
  - `cd apps/dondone-mobile/android`
  - `./gradlew.bat assembleDebug`
- 가능하면 추가:
  - `./gradlew.bat test`
- 수동 검증 시나리오:
  - 로그인 후 wage 화면 진입 시 monthly summary/estimate가 로드된다.
  - 네트워크 실패 시 wage 화면이 error state와 retry를 노출한다.
  - 로그아웃 상태에서 wage 화면이 unauthenticated gate를 노출한다.
  - 실제 입금 입력 후 `POST /api/wage/deposits` 성공 및 화면 수치 갱신이 반영된다.
  - verification 생성 후 threshold, possible causes, next actions가 갱신된다.
  - 홈/금융 홈 요약 카드가 remote wage 결과를 반영한다.
- 테스트 환경 blocker가 있으면 Android SDK, JDK, backend 실행 여부를 구분해서 보고한다.

# Review Focus
- wage 원격 계층이 `advance/remittance/workproof` 패턴과 일관되게 설계되었는지
- `401`, `404`, 일반 오류가 급여 화면에서 혼동 없이 분기되는지
- remote 값과 demo fallback 값의 우선순위가 명확하고 뒤섞이지 않는지
- 실제 입금 등록 후 로컬 state를 임의로 덮어쓰지 않고 서버 재조회 기준으로 동작하는지
- verification 생성 시 PRD의 `참고용 추정 + 근거 기반 이상 탐지` 문구가 유지되는지
- 홈/금융 홈 수정이 wage 실연동 범위를 넘어서 다른 금융 흐름을 깨뜨리지 않는지

# Worktree Split Decision
- Single lane

이번 작업은 모바일 wage 데이터 계층, session load 순서, 급여 화면 UI, 홈/금융 홈 요약, auth/오류 처리 경계가 함께 움직인다. 공통 상태와 공용 UI 모델이 동시에 바뀌므로 lane을 나누면 remote state shape와 화면 계약이 계속 충돌할 가능성이 높다. shared DTO, auth 처리, 공통 response contract가 아직 움직이는 구간이므로 병렬 분할은 하지 않는다.

# Commit Plan
- `docs: add mobile wage backend connection execplan`
- `feat: add mobile wage remote repository and session state`
- `feat: connect wage screen to backend wage APIs`
- `feat: reflect remote wage summary in home and finance cards`
- `test: verify mobile wage backend connection flow`

# Open Questions
- wage 화면에서 verification 생성 직후 별도 상세 상태를 유지할지, 아니면 매번 최신 verification 하나만 보여줄지 결정이 필요하다.
- `workplaceId` 와 `month` 선택 기준을 초기 1차에서는 무엇으로 고정할지 결정이 필요하다.
  - 현재 데모 기준 월
  - 현재 주 근무지 1개
- 홈/금융 홈에서 remote wage 결과가 없을 때 fallback 문구를 어디까지 보여줄지 결정이 필요하다.

# Assumptions
- 현재 로그인 사용자는 주 근무지 1개를 사용하며, 1차 구현에서는 그 근무지를 기본 wage 조회 대상으로 본다.
- `month` 는 현재 데모 기준 월 또는 현재 시점 월 1개만 우선 지원하고, 월 전환 UI는 이번 범위에 넣지 않는다.
- backend wage API는 모바일이 소비 가능한 수준으로 이미 안정화되어 있으며 1차에서는 모바일 계약만 추가한다.
- wage verification은 append-only 성격으로 다루고, 모바일은 최신 생성 결과를 중심으로 보여준다.
- 1차 구현 목표는 “실연동된다”는 증명이지, wage 보호 흐름 전체 완성본이 아니다.
