# Mobile Remittance Async Completion Notice

## Source Inputs
- [docs/DonDone_PRD_v1.5.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/DonDone_PRD_v1.5.md)
- [DemoSessionViewModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt)
- [DemoSessionReducer.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt)
- [DonDoneNavGraph.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt)
- [HomeUiModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeUiModel.kt)
- [HomeScreen.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/HomeScreen.kt)
- [TransferUiModel.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/remittance/presentation/TransferUiModel.kt)
- [BackendRemittanceRepository.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/remittance/BackendRemittanceRepository.kt)
- [AuthSessionStore.kt](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/AuthSessionStore.kt)
- [TransferService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java)
- [RemittancePolicyService.java](/mnt/c/Users/SSAFY/Desktop/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java)
- [docs/perf/remittance-transfer-latency/03-current-findings.md](/mnt/c/Users/SSAFY/Desktop/S14P21C202/docs/perf/remittance-transfer-latency/03-current-findings.md)

## Goal
- Sepolia confirmation 지연 동안 사용자를 tracker 화면에 붙잡아 두지 않고, 송금 요청 성공 직후 홈으로 복귀시킨 뒤 비동기 완료/실패 결과를 홈 배너로 1회 안내하고 재진입 시에도 복원되게 만든다.

## In Scope
- `createTransfer` 성공 후 홈으로 즉시 복귀하는 모바일 네비게이션 이벤트 추가
- remittance terminal 상태를 감지해 홈에서 보이는 비동기 완료 배너 상태 추가
- 중복 노출 방지를 위한 클라이언트 로컬 소비 마커(`dismissed transfer id`) 저장
- 앱 초기 로드/재진입 시 최신 remittance 상태를 기반으로 완료 배너 복원
- 관련 모바일 단위 테스트와 홈 UI 테스트 갱신

## Out of Scope
- remittance blockchain confirmation 성능 개선
- FCM 푸시 알림 연동
- backend unread terminal event 전용 endpoint 추가
- 송금 내역/알림 센터 신설
- 송금 정책/DTO/API 계약 변경

## Affected Modules
### Backend
- 기존 `GET /api/remittance/transfers`, `GET /api/remittance/transfers/{transferId}` 계약 확인만 수행한다.
- 이번 라운드는 backend 코드 수정 없이 모바일-only 구현을 우선한다.

### Mobile
- `DemoSessionViewModel`
- `DonDoneNavGraph`
- `HomeUiModel`
- `HomeScreen`
- `TransferUiModel`
- 새 remittance completion notice local store
- `DemoSessionViewModelTest`
- `HomeUiModelTest`

### Docs
- 실행 계획 문서 갱신

### Shared
- 없음

## Contract Changes
- 없음
- 기존 remittance API/DTO를 그대로 사용한다.

## Security Notes
- auth/authz 변경 없음
- 송금 요청 계약이나 token 처리 변경 없음
- 로컬 소비 마커 저장은 transfer id만 저장하고 access token 등 민감정보는 저장하지 않는다.

## Maintainability Notes
- `DemoState.remittance.status`는 도메인 상태이고, 1회성/지속성 UI 알림 상태와 역할이 다르다. completion notice는 별도 session/viewmodel 상태로 둬서 의미를 섞지 않는다.
- terminal transfer 감지는 polling detail merge와 remote reload 두 경로 모두에서 발생하므로, notice 생성 규칙은 한 helper에 모아 중복/드리프트를 막는다.
- 홈 배너는 기존 `money.nextAction` 계산과 분리해, “다음 행동” 카드와 “비동기 완료 알림”이 서로의 조건을 오염시키지 않게 한다.

## Implementation Steps
1. remittance async completion notice 모델과 소비 마커 저장소를 추가한다.
2. `DemoSessionViewModel`에 create 성공 후 홈 복귀용 one-shot navigation event를 추가한다.
3. polling/detail reload 이후 terminal transfer를 감지해 notice 상태를 갱신하고, 이미 dismiss된 transfer id는 다시 띄우지 않도록 연결한다.
4. `DonDoneNavGraph`에서 remittance create 성공 이벤트를 받아 홈으로 navigate하고 event를 consume한다.
5. `HomeUiModel`/`HomeScreen`에 dismiss 가능한 completion banner를 추가한다.
6. `openTransferFlow()` 재진입 정책을 `SUBMITTED` tracker 기준 대신 “pending async transfer 있음” 기준으로 다시 확인한다.
7. ViewModel/Home 테스트를 추가·갱신한다.

## Test Plan
- `DemoSessionViewModelTest`
  - create 성공 후 홈 복귀 이벤트 발생
  - terminal transfer 감지 시 completion notice 생성
  - dismiss 후 같은 transfer는 재노출되지 않음
  - 앱 재로드 시 dismiss 전 terminal transfer는 복원됨
- `HomeUiModelTest`
  - completion banner가 성공/실패 상태를 올바르게 표시
- 가능하면 `:app:testDebugUnitTest --tests ...`로 범위 실행

## Review Focus
- create 성공 후에만 홈 복귀가 일어나는가
- terminal transfer가 홈 배너로 1회만 노출되고 dismiss 후 재노출되지 않는가
- 앱 초기 로드/재진입 시 dismiss되지 않은 terminal transfer가 다시 복원되는가
- pending transfer가 있을 때 송금 플로우 재진입 정책이 의도대로 유지되는가

## Worktree Split Decision
- `Single lane`

ViewModel, navigation, 홈 UI, 로컬 저장소가 같은 remittance 비동기 상태 전이를 공유하므로 파일 소유권을 깔끔하게 분리하기 어렵다.

## Commit Plan
- `feat: 모바일 송금 비동기 완료 배너 추가`
- `test: 송금 비동기 완료 알림 회귀 추가`

## Open Questions
- 없음. 1차 구현은 모바일-only, 홈 배너, 로컬 소비 마커 기준으로 진행한다.

## Assumptions
- backend 사용자용 remittance API는 최신순 목록/상세만 제공하며 unread terminal event는 계산하지 않는다.
- 동시에 하나의 active transfer만 허용하므로, 모바일은 최신 pending/terminal transfer 한 건 중심으로 notice를 계산해도 된다.
- FCM 없이도 홈 배너 + 앱 내부 상태 복원만으로 현재 UX 문제를 충분히 완화할 수 있다.
