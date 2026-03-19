# Source Inputs
- Root guidance:
  - `AGENTS.md`
- Skill guidance:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- Product/API docs:
  - `docs/DonDone_P0_Functional_Spec_v0.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
- Existing plans:
  - `docs/execplans/active/2026-03-19-mobile-wage-backend-connection.md`
  - `docs/execplans/active/2026-03-17-documents-claim-verification-follow-up.md`
- Mobile current code:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
- Backend current code:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageVerificationRelatedActionsService.java`

# Goal
급여점검 탭의 `문서` 액션이 단순히 메뉴 탭으로만 이동하지 않고, 현재 급여 확인 결과에 맞는 실제 문서 화면(`Proof Pack` 문서 시트 또는 `Claim Kit`/신고 준비 시트)으로 바로 이어지도록 연결한다.

# In Scope
- 급여 탭에서 문서 액션 선택 시 메뉴 탭의 문서 상세/신고 준비 시트 자동 오픈
- 급여 verification 관련 상태를 기준으로 메뉴 진입 타겟 결정
- 메뉴 화면에 외부 진입 요청 소비 로직 추가
- 필요 시 로컬 문서 상태를 wage verification 결과와 최소 동기화

# Out of Scope
- 신규 라우트 추가
- Documents backend API 추가 소비
- Proof Pack / Claim Kit 생성 API 호출 직접 구현
- 웹/백엔드 계약 변경
- 메뉴 문서 UI 전면 리디자인

# Affected Modules
## Backend
- 계약 변경 없음
- 참조만 수행:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageVerificationRelatedActionsService.java`

## Mobile
- 수정:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/navigation/DonDoneNavGraph.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/MenuScreen.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/wage/presentation/WageUiModel.kt`
- 신규 가능:
  - 메뉴 자동 진입 요청을 담는 session/state 파일 1개

## Docs
- `docs/execplans/active/2026-03-19-mobile-wage-document-linking.md`

## Shared
- 없음

# Contract Changes
- 백엔드 API/DTO 변경 없음
- 모바일 내부 계약 추가:
  - 급여 탭에서 메뉴로 넘기는 `문서 진입 타겟` 상태
  - 메뉴 화면이 외부 요청을 1회 소비하고 적절한 시트를 여는 규칙

# Security Notes
- 새로운 인증 경로 추가 없음
- 타 사용자 문서 조회/직접 documentId 접근 같은 우회 경로를 만들지 않는다
- 문서 진입 판단은 현재 로그인 사용자 컨텍스트의 wage verification 결과만 사용한다

# Maintainability Notes
- 급여 탭은 “어디로 갈지 결정”만 하고, 메뉴 화면 상세 렌더링 책임은 메뉴 feature 안에 둔다
- 메뉴 자동 진입 요청은 1회성 소비 상태로 관리해 재구성/재컴포지션 때 반복 오픈되지 않게 한다
- 백엔드 `proofPackReady` / `claimKitReady` 불리언이 실제 문서 존재와 다를 수 있으므로, 모바일 진입 판단은 문서 ID 존재 여부를 우선 사용한다

# Implementation Steps
1. 메뉴 자동 진입용 내부 상태 모델을 추가한다
2. `DemoSessionViewModel` 에 급여-문서 진입 요청 생성/소비 메서드를 추가한다
3. 급여 탭의 `문서` 액션이 메뉴 일반 진입 대신 `request -> navigate` 흐름을 타도록 바꾼다
4. 메뉴 화면에 launch request 파라미터와 `LaunchedEffect` 소비 로직을 추가한다
5. 요청 타겟에 따라 `Proof Pack 문서 시트`, `Claim Kit 문서 시트`, `신고 준비 시트` 중 하나를 연다
6. 필요 시 `DemoState.documents` 를 verification 관련 ID 기준으로 최소 동기화한다
7. Android assemble로 회귀를 확인한다

# Test Plan
- `cd apps/dondone-mobile/android && ./gradlew.bat :app:assembleDebug`
- 수동 확인:
  - 급여 탭에서 `문서` 선택 시 메뉴 리스트만 보이지 않고 관련 문서/시트가 즉시 열린다
  - proof pack 문서가 있을 때 Proof 문서 시트가 열린다
  - claim kit 문서가 있을 때 Claim 문서 시트가 열린다
  - 생성된 문서가 없지만 verification이 있을 때 신고 준비 시트가 열린다
  - 요청 소비 후 메뉴 재진입 시 같은 시트가 반복 자동 오픈되지 않는다

# Review Focus
- 급여 탭이 문서 상세 렌더링 책임까지 가져오지 않았는지
- launch request 소비가 1회성으로 안전한지
- proof/claim 우선순위가 사용자 기대와 맞는지
- 기존 메뉴 수동 진입 동작이 회귀하지 않는지

# Worktree Split Decision
- Single lane

이번 변경은 급여 화면, session 상태, 메뉴 시트 오픈 규칙이 같이 움직인다. 라우트 추가 없이 기존 composable 내부 상태를 외부 요청과 연결해야 하므로, 한 레인에서 일관되게 정리하는 편이 안전하다.

# Commit Plan
- `docs: add mobile wage document linking execplan`
- `feat: connect wage document action to menu document sheets`
- `test: verify wage to menu document launch flow`

# Open Questions
- proof document와 claim document가 모두 준비된 경우 기본 우선순위를 proof로 둘지 claim으로 둘지
- verification은 있지만 문서 리스트가 아직 로컬 seed 상태일 때 어떤 문서 타이틀/상태를 보여줄지

# Assumptions
- 이번 슬라이스의 목표는 “실제 문서 관련 화면으로 바로 진입”이며, 문서 생성 자체를 급여 탭 안에서 수행하는 것은 아니다
- 문서가 실제로 존재하는지는 `proofPackDocumentId`, `claimKitDocumentId` 존재 여부를 우선 신뢰한다
- claim kit 문서가 없더라도 verification이 있으면 신고 준비 시트는 열 수 있다
