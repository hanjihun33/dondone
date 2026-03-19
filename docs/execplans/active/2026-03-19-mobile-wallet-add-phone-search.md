# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- 사용자 요구:
  - 안드로이드 계좌·지갑 관리 화면에서 `지갑 추가` 동작 복구
  - 지갑 주소 직접 입력만이 아니라 휴대폰 번호 검색 우선 UX 제안 및 구현
- 현재 모바일 구현:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- 현재 백엔드 계약:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`

# Goal
계좌·지갑 관리 화면의 `지갑 추가`를 실제로 동작하게 만들고, 사용성이 낮은 직접 주소 입력만 강제하지 않도록 `휴대폰 번호로 찾기` 우선 UX를 추가한다. 현재 백엔드 계약을 깨지 않으면서도 인증/비인증 상태 모두에서 데모 가능한 등록 흐름을 제공한다.

# In Scope
- `지갑 추가` 헤더 액션을 실제 클릭 가능한 CTA로 전환
- 계좌·지갑 관리 화면에 지갑 추가 바텀시트 추가
- 바텀시트에서 `휴대폰 번호로 찾기` / `지갑 주소 직접 입력` 모드 제공
- 전화번호 검색 결과를 로컬 데모 디렉터리에서 찾고, 선택한 지갑을 수신 지갑으로 등록
- 인증 사용자: 기존 `POST /api/remittance/recipients` 로 등록
- 비인증 사용자: 로컬 `DemoState.remittance.recipients` 에 추가
- 성공/빈 결과/검증 오류 상태를 화면에 명시
- 관련 unit test 추가

# Out of Scope
- 백엔드 전화번호 기반 수신자 검색 API 추가
- 연락처 권한 요청, 주소록 동기화, SMS 인증
- 수신 지갑 수정/삭제 화면
- 송금 화면의 수신자 등록 UX 전면 개편

# Affected Modules
## Backend
- 구현 변경 없음
- 계약 참조만 사용

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageScreen.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionReducer.kt`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/feature/finance/presentation/AccountManageUiModelTest.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionReducerTest.kt`
- 필요 시 `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-19-mobile-wallet-add-phone-search.md`

## Shared
- 기존 remittance action toast/state 재사용

# Contract Changes
- 외부 HTTP API shape 변경은 없다.
- 모바일 내부 계약만 아래처럼 확장한다.
- `AccountManageUiModel` 에 전화번호 검색용 데모 디렉터리/복사용 전체 주소/CTA 상태를 추가한다.
- `Recipient` 모델은 전화번호 표시를 위한 선택 필드를 가질 수 있다.
- `DemoSessionReducer` 에 로컬 수신 지갑 추가 동작을 추가한다.

# Security Notes
- 전화번호 검색은 실제 서버 조회가 아니라 모바일 내 데모 디렉터리 조회로 제한한다.
- 검색 결과에는 전화번호 전체를 노출하지 않고 마스킹 값만 보여준다.
- 인증 사용자의 실제 등록은 기존 access token 기반 `createRecipient` 호출만 사용한다.
- 지갑 주소 복사/표시는 테스트넷 데모 정보 범위로 유지한다.

# Maintainability Notes
- 전화번호 검색용 디렉터리는 화면 내부 임시 하드코딩이 아니라 UI 모델/소유 파일에서 관리해 추후 실제 API 응답으로 교체하기 쉽게 둔다.
- 직접 입력과 전화번호 검색이 같은 등록 액션으로 수렴하도록 해 등록 후처리 로직을 ViewModel 한 곳에 모은다.
- `AccountManageScreen` 안에 복잡한 상태 분기를 퍼뜨리지 말고, 검색 후보 표시용 모델과 검증 helper를 분리한다.

# Implementation Steps
1. 현재 백엔드가 전화번호 검색을 지원하지 않는다는 점을 계획 문서에 고정한다.
2. `AccountManageUiModel` 에 전화번호 검색 후보 모델과 바텀시트 copy/상태를 추가한다.
3. `AccountManageScreen` 에 클릭 가능한 `지갑 추가` 액션과 바텀시트를 구현한다.
4. 전화번호 검색 모드에서 로컬 디렉터리 exact/partial match 검색과 결과 선택 UI를 추가한다.
5. 직접 입력 모드에서 별칭/관계/주소 입력과 검증 UI를 추가한다.
6. `DemoSessionViewModel` 에 인증/비인증 공통 수신 지갑 추가 진입점을 추가한다.
7. `DemoSessionReducer` 에 비인증 로컬 recipient 추가 로직을 추가한다.
8. UI 모델 테스트와 reducer/viewmodel 테스트를 보강하고 범위 검증을 실행한다.

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew --no-daemon :app:testDebugUnitTest --tests com.dondone.mobile.feature.finance.presentation.AccountManageUiModelTest --tests com.dondone.mobile.app.session.DemoSessionReducerTest --tests com.dondone.mobile.app.session.DemoSessionViewModelTest`
- 가능하면 `cd apps/dondone-mobile/android && ./gradlew --no-daemon :app:compileDebugKotlin`
- 수동 확인
  - 계좌·지갑 관리 화면에서 `지갑 추가` 버튼 탭
  - 전화번호 검색 결과 선택 후 등록
  - 검색 결과 없음 시 직접 입력 전환
  - 직접 입력 검증 실패/성공

# Review Focus
- `지갑 추가`가 실제 CTA로 동작하는가
- 전화번호 검색 우선 UX가 주소 직접 입력 fallback과 자연스럽게 이어지는가
- 인증/비인증 상태 모두에서 등록 후 목록 갱신과 선택 상태가 맞는가
- 실제 전화번호 API가 없다는 제약이 UI copy에서 오해 없이 드러나는가
- 지갑 주소/전화번호 표시가 과도하게 노출되지 않는가

# Worktree Split Decision
- Single lane

`AccountManage` 화면, remittance 세션 상태, demo recipient 모델이 함께 바뀐다. 공통 UI 상태와 등록 후처리가 한 흐름으로 연결돼 있어 단일 lane으로 처리하는 편이 안전하다.

# Commit Plan
- `docs: wallet add phone search 실행계획 추가`
- `feat: 계좌 지갑 관리 전화번호 검색 기반 지갑 추가`
- `test: wallet add phone search 회귀 보강`

# Open Questions
- 실제 전화번호 검색 API는 후속 서버 작업으로 남는다.

# Assumptions
- 이번 턴의 `휴대폰 번호 검색`은 실제 사용자 조회가 아니라 데모 디렉터리 검색으로 구현한다.
- 검색 결과에서 선택된 연락처는 이미 지갑 주소가 준비된 DonDone 사용자 시나리오로 가정한다.
- 비인증 상태에서도 동일 UX를 보여 주되, 등록 결과는 로컬 demo recipient 목록에만 반영한다.
