# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/prd-breakdown/SKILL.md`
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- PRD:
  - `docs/DonDone_PRD_v1.5.md` 7E, 7F, 12.5
- 기존 계약 문서:
  - `docs/DonDone_P0_API_Contract_v0.md` 7.1 ~ 7.5
- 최근 백엔드 변경:
  - `git show 02b24fa` (`feat: remittance 송금 백엔드 추가`)
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/RemittanceIntegrationTest.java`
- 현재 모바일 구조:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/BackendAdvanceRepository.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/workproof/BackendWorkproofRepository.kt`

# Goal
안드로이드 앱의 송금 플로우를 최근 백엔드 remittance 구현과 실제로 연결한다. 로그인된 사용자는 서버 지갑 생성/잔액, 수신자 허용목록, 송금 precheck, 송금 생성, 상태 추적 결과를 앱에서 확인하고 진행할 수 있어야 하며, 기존 디자인 톤을 유지한 범위에서 필요한 setup/empty/error 화면을 추가한다.

# In Scope
- 안드로이드 remittance 원격 state / repository 추가
- 로그인 세션 기반 remittance 데이터 로드 및 새로고침
- `POST /api/remittance/wallets/me` 기반 서버 지갑 생성 또는 재조회
- `GET /api/remittance/wallets/me/balance` 기반 잔액 동기화
- `GET /api/remittance/recipients`, `POST /api/remittance/recipients` 연동
- `POST /api/remittance/transfers/precheck`, `POST /api/remittance/transfers` 연동
- `GET /api/remittance/transfers`, `GET /api/remittance/transfers/{transferId}` 기반 상태 추적
- 송금 화면에 loading / empty / error / success 상태 반영
- 수신자 없음, 지갑 funding 실패/대기, 정책 차단, 송금 진행 상태를 기존 디자인 기반으로 표시
- 메뉴 영수증 세션이 실제 tx hash / 상태를 remittance 원격 상태와 동기화하도록 보강
- 필요한 범위의 모바일 unit test 추가

# Out of Scope
- 백엔드 remittance API shape 변경
- 관리자 remittance ops API 모바일 연동
- 영수증 PDF 생성 / 문서 저장 / presigned URL 연동
- 수신자 수정(`PUT /api/remittance/recipients/{recipientId}`) 전용 편집 화면
- 실제 실자금 / 메인넷 / 자동 송금
- 별도 운영자 콘솔 UI

# Affected Modules
## Backend
- 계약 참조만 사용, 구현 변경은 기본적으로 없음
- 필요 시 테스트/문서 검증만 수행

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModelFactory.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
- 신규 `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/*`
- 신규 또는 보강 테스트 `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/**`

## Docs
- `docs/execplans/active/2026-03-19-mobile-remittance-backend-connection.md`
- 계약 차이가 확인되면 후속 문서 메모 필요 여부 검토

## Shared
- `BuildConfig.DONDONE_API_BASE_URL`
- 기존 `AuthSession` access token 재사용
- 공통 `ApiResponse<T>` envelope 파싱 재사용

# Contract Changes
- 백엔드 외부 계약 변경은 목표가 아니다.
- 모바일 내부 계약을 아래처럼 추가/변경한다.
- 신규 remittance 원격 payload/state
  - wallet summary
  - wallet balance summary
  - recipient list
  - latest transfer list / active transfer detail
  - action 상태(precheck/create/polling/create recipient)
- `TransferUiModel` 은 demo seed만 보지 않고 remittance 원격 상태를 받아 화면을 구성한다.
- 메뉴 영수증 UI 모델은 demo tx hash 대신 원격 transfer detail 우선 값을 사용한다.
- 실제 백엔드 계약은 `docs/DonDone_P0_API_Contract_v0.md` 보다 구체적으로 아래를 따른다.
  - recipient 응답은 `name`, `photoUrl`, `cooldownUntil` 없이 `alias`, `relation`, `allowed`, `recentlyUpdated`, `updatedAt` 를 사용한다.
  - transfer 생성은 `202 Accepted` 가 아니라 `201 Created` / replay 시 `200 OK` 로 응답한다.
  - `Idempotency-Key` 는 문서 초안의 필수 규칙과 일치하게 실제 구현에서도 필수다.
  - 금액은 현재 `amountAtomic` / `assetSymbol=dUSDC` 기준으로 동작한다.

# Security Notes
- 모든 remittance 모바일 호출은 기존 `Authorization: Bearer <token>` 세션을 재사용한다.
- 로그인 전에는 원격 호출을 하지 않고 기존 demo flow 또는 안내 상태로 유지한다.
- `Idempotency-Key` 는 모바일에서 요청마다 새 UUID를 생성해 중복 전송을 피한다.
- 정책 차단 사유(`RECENT_RECIPIENT_CONFIRMATION_REQUIRED`, `HIGH_AMOUNT_CONFIRMATION_REQUIRED`, `INSUFFICIENT_WALLET_BALANCE`)는 사용자 문구로 표시하되 민감정보를 추가 저장하지 않는다.
- tx hash, wallet address는 테스트넷 공개 정보로만 취급하고 access token/세션 값은 로그나 UI에 노출하지 않는다.
- 허용 목록이 아닌 수신자나 타 사용자 송금 접근은 백엔드 소유권 검증을 그대로 따른다.

# Maintainability Notes
- remittance 실연동은 `advance`, `workproof` 와 동일하게 별도 `data/remittance` 계층으로 분리해 `DemoSessionViewModel` 에서 네트워크 세부 구현이 새지 않게 한다.
- 기존 `DemoState.remittance` 전체를 원격 모델로 치환하지 말고, 현재 화면이 의존하는 최소 UI 데이터만 동기화하거나 별도 원격 상태를 함께 주입해 변경 범위를 제어한다.
- 기존 송금 화면의 시각적 톤은 유지하되, 상태별 UI 분기는 `TransferUiModel` 로 끌어올려 composable 내부 조건 분산을 줄인다.
- PRD 필수 문구인 테스트넷 안내는 화면 모델이 일관되게 소유하게 해 여러 composable에 하드코딩하지 않는다.
- 메뉴 영수증과 송금 tracker가 같은 transfer detail 정보를 중복 계산하지 않도록 공통 매핑을 한 곳에서 소유한다.

# Implementation Steps
1. remittance 백엔드 계약과 PRD 요구를 기준으로 모바일 사용 시나리오를 고정한다.
2. `data/remittance` 패키지에 payload, remote state, repository, 예외 타입을 추가한다.
3. repository에서 wallet create-or-return, balance 조회, recipient 조회/등록, precheck, create transfer, transfer list/detail polling을 구현한다.
4. `DemoSessionViewModel` 에 remittance 원격 상태, 액션 상태, 초기 로드/재로드, 송금 액션 helper를 추가한다.
5. 인증 성공/복구/로그아웃 시 remittance 상태도 `advance`, `workproof` 와 같은 방식으로 동기화한다.
6. `TransferUiModel` 을 remittance 원격 상태 기반으로 확장한다.
7. `TransferScreen` 에 아래 상태 UI를 추가한다.
   - remittance loading
   - 지갑 생성/seed funding 안내
   - 수신자 empty + 등록 CTA
   - 정책 확인 문구
   - 송금 진행 / 최종 상태
8. 기존 디자인 톤을 유지한 수신자 등록 입력 화면 또는 sheet를 추가한다.
9. 메뉴 영수증 세션을 원격 transfer detail과 동기화한다.
10. remittance 관련 unit test와 가능한 빌드 검증을 실행한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew :app:testDebugUnitTest`
- 가능하면 `cd apps/dondone-mobile/android && ./gradlew :app:assembleDebug`
- 필요한 unit test
  - remittance repository JSON 파싱 / 에러 매핑
  - `TransferUiModel` 의 loading / wallet pending / recipient empty / policy block / confirmed 상태 매핑
  - `MenuUiModel` 의 실제 tx hash 우선 반영
  - `DemoSessionViewModel` 의 로그인 후 remittance 로드, 송금 성공, 인증 만료 처리
- 필요 시 백엔드 확인
  - `cd apps/dondone-backend && ./gradlew test --tests com.workproofpay.backend.remittance.RemittanceIntegrationTest`

# Review Focus
- 모바일이 실제 백엔드 DTO shape를 따르고 있는가
- authenticated / unauthenticated / error / empty 상태가 명확히 분리되는가
- 지갑 미생성/seed funding pending/failed 상태가 사용자에게 이해 가능하게 보이는가
- 최근 수정 수신자 확인, 고액 확인, 잔액 부족이 적절히 안내되는가
- `Idempotency-Key` 처리와 재시도 시나리오가 중복 송금을 유발하지 않는가
- 메뉴 영수증과 tracker 화면이 같은 상태를 일관되게 보여주는가
- 기존 demo 송금 UX가 로그인 전 fallback으로 깨지지 않는가

# Worktree Split Decision
- Single lane

공유 ViewModel, remittance UI model, 메뉴 영수증 모델, 인증 세션, 공통 navigation이 함께 바뀐다. 공통 계약과 상태 흐름이 동시에 움직여 충돌 위험이 높으므로 단일 lane으로 처리한다.

# Commit Plan
- `docs: remittance 모바일 연동 실행계획`
- `feat: 모바일 remittance 백엔드 연동`
- `test: 모바일 remittance 상태/모델 검증`

# Open Questions
- 없음. 이번 슬라이스에서는 실제 백엔드 구현 계약을 우선 기준으로 사용한다.

# Assumptions
- 로그인된 사용자가 송금 화면에 진입하면 모바일은 `POST /api/remittance/wallets/me` 로 지갑을 create-or-return 하며, 최초 setup은 자동 처리한다.
- transfer 상태 `REQUESTED`, `SIGNED`, `BROADCASTED` 는 모바일에서 모두 진행 중 상태로 묶고, `CONFIRMED`, `FAILED`, `TIMED_OUT` 만 최종 상태로 분리한다.
- 수신자 관계 입력은 백엔드 enum(`FAMILY`, `SPOUSE`, `PARENT`, `CHILD`, `SIBLING`, `RELATIVE`, `FRIEND`, `OTHER`)으로 제한한다.
- 송금 금액 입력은 현재 모바일 UX를 유지하되 백엔드 계약에 맞춰 `dUSDC atomic` 으로 변환한다.
- PRD 필수 문구인 "현재는 테스트넷 데모입니다. 실제 자금 이동이 발생하지 않습니다."를 송금 흐름에 포함한다.
