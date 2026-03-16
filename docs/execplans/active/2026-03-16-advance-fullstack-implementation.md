# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 기준:
  - `docs/CODEX_WORKFLOW.md`
  - `.agents/skills/prd-breakdown/SKILL.md`
  - `.agents/skills/execplan-writer/SKILL.md`
- PRD / 계약 문서:
  - `docs/DonDone_PRD_v1.5.md` 6.3, 7C, 12.2, 13.1~13.4
  - `docs/DonDone_P0_API_Contract_v0.md` Advance / WorkProof / 공통 규칙 섹션
  - `docs/DonDone_P0_Functional_Spec_v0.md` 4. Advance
- 기존 계획 / 구현 흔적:
  - `docs/execplans/active/2026-03-13-workproof-wage-advance-backend-start.md`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/AdvanceCalculator.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/**`
- 탐색 결과 반영:
  - backend: WorkProof lane 1 요약은 Advance upstream 후보지만 `pendingMinutes`, `needsReviewRecordCount`, `workproofRiskFlags`가 placeholder 상태
  - mobile: Android는 아직 auth/network 계층 없이 `DemoState + AdvanceCalculator` 기반이라 실서버 연동보다 상태/계약 분리가 선행되어야 함

# Goal
PRD P0 기준의 `미리받기(Advance)`를 DonDone 백엔드와 Android 앱에서 실제 계약 중심 흐름으로 구현할 수 있게 범위와 순서를 고정한다. 핵심은 `근거 설명 -> 신청 -> 월 이력/상세`를 데모 시뮬레이션 범위에서 일관되게 제공하고, 현재 모바일의 계산기 기반 상태를 서버 응답 기반 상태로 전환 가능한 구조로 바꾸는 것이다.

# In Scope
- 백엔드 Advance P0 API 구현
  - `GET /api/advance/eligibility?workplaceId={id}`
  - `POST /api/advance/requests`
  - `GET /api/advance/requests?month=YYYY-MM`
  - `GET /api/advance/requests/{requestId}`
- WorkProof lane 1 월간 요약과 현재 계약 데이터를 Advance 입력으로 재사용
- Advance 전용 DTO / validation / error code / persistence 추가
- `Idempotency-Key` 기반 신청 중복 방지 처리
- Android 앱에서 Advance 데이터를 계약 중심 UI 상태로 연결
  - loading / empty / error / blocked / success 상태 명시
  - Finance 화면의 미리받기 섹션 우선 반영
  - 필요 시 Home 카드의 미리받기 요약 동기화
  - 이번 slice에서는 `실서버 조회 + 로그인 UI/세션 저장 + 신청(create) + 이력 상세(detail)`까지 포함하되, 화면 범위는 Android 앱에 한정
- Android 로그인 기능
  - 기존 `/api/auth/login` 계약을 쓰는 로그인 화면 추가
  - access token 저장, 앱 시작 시 세션 복구, 로그아웃 처리
  - 로그인된 세션을 Advance 실연동 조회에 재사용
- PRD 필수 디스클레이머와 evidence-first 문구 반영
- 계약 변경에 맞는 테스트 및 문서 동기화

# Out of Scope
- 실제 대출 집행, 실제 회수/상환, 외부 금융 파트너 연동
- 고용주 운영 콘솔 / Verified Worker Summary 구현 확장
- Vault, Remittance, Documents, Claim의 상세 구현
- 관리자 심사 화면 또는 범용 신용평가 모델
- iOS 또는 mockup 전면 리디자인
- Demo Time Travel 전체 재설계

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/advance/**` 신규
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/api/WorkProofController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofLane1Service.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
- `apps/dondone-backend/src/test/java/**/advance/**` 신규
- 필요 시 공통 idempotency 처리 위치:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/**`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/home/presentation/**`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/menu/presentation/**`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/**`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/**`
- 신규 필요 후보:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/advance/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/advance/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/**`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/auth/presentation/**`

## Docs
- `docs/execplans/active/2026-03-16-advance-fullstack-implementation.md`
- 필요 시 `docs/DonDone_P0_API_Contract_v0.md`
- 필요 시 `docs/DonDone_P0_Functional_Spec_v0.md`

## Shared
- `ApiResponse<T>` envelope 유지
- `Idempotency-Key` 규칙
- `X-Demo-AsOf` 적용 여부 결정
- PRD 디스클레이머 문구 재사용

# Contract Changes
- 백엔드 신규 응답 계약
  - eligibility:
    - `availableAmount`
    - `maxCap`
    - `policyRate`
    - `repaymentTier`
    - `reflectedWorkDays`
    - `reflectedWorkMinutes`
    - `verifiedMinutes`
    - `pendingMinutes`
    - `needsReviewRecordCount`
    - `blockReasonCodes[]`
    - `nextTierRemainingMinutes`
    - `estimatedFee`
    - `estimatedRepaymentDate`
    - `disclaimer`
  - request create/list/detail:
    - `requestId`
    - `status`
    - `requestedAmount`
    - `approvedAmount`
    - `feeAmount`
    - `repaymentDueDate`
    - `eligibilitySnapshot`
- 백엔드 신규 요청 계약
  - `POST /api/advance/requests`
    - `workplaceId`
    - `requestedAmount`
    - `requestedAt`
  - header:
    - `Idempotency-Key` 필수
- DB / model 후보
  - `advance_requests`
  - `advance_snapshots`
- 신규 에러 코드 후보
  - `ADVANCE_DUPLICATE_REQUEST`
  - `ADVANCE_NOT_ELIGIBLE`
  - `REQUEST_AMOUNT_EXCEEDS_LIMIT`
  - `ADVANCE_REQUEST_NOT_FOUND`
  - `IDEMPOTENCY_KEY_REQUIRED`
  - `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD`
- 모바일 계약 변경
  - 현재 `AdvanceData` / `AdvanceCalculator` 중심 내부 계산 모델에서
    `AdvanceEligibilityUiState`, `AdvanceRequestUiState`, `AdvanceHistoryItem` 같은 계약 중심 모델로 분리
  - 서버 응답이 없을 때의 명시적 fallback 상태 정의 필요
  - Android 로그인 상태 계약 추가
    - `restoring`
    - `unauthenticated`
    - `submitting`
    - `authenticated`
  - access token은 Android 로컬 저장소에만 보관하고, Advance repository는 저장된 토큰으로만 조회

# Security Notes
- Advance API는 모두 JWT 보호 대상으로 유지한다.
- Android는 저장된 access token만 사용하고 seed 계정 자동 로그인 경로는 제거한다.
- 타인 소유 `workplaceId`, `requestId` 접근은 `404` 은닉 원칙을 따른다.
- `POST /api/advance/requests`는 `Idempotency-Key`를 강제하고 동일 키 재사용 정책을 명확히 검증한다.
- PRD 기준으로 P0는 실제 금융 서비스가 아니라 데모 시뮬레이션이므로, 응답과 UI 모두 실대출/실정산처럼 보이는 표현을 금지한다.
- `X-Demo-AsOf`는 우선 읽기 API(`eligibility`, list/detail 조회)에서만 허용 여부를 검토하고, 신청 생성에는 기본적으로 적용하지 않는다.
- 차단 사유와 회수 가능성 판단은 explanation-first 원칙으로 노출하되, 내부 점수나 과도한 심사 모델 표현은 피한다.

# Maintainability Notes
- 백엔드는 feature-first 구조(`api/service/repo/model/adapter`)를 유지하고, 정책 계산은 controller가 아닌 service/adapter에 격리한다.
- WorkProof 월간 집계와 Advance 적격성 계산이 서로 다른 규칙을 복제하지 않도록 WorkProof upstream 값을 재사용한다.
- 다만 현재 `WorkProofLane1Service` 월간 요약은 일부 placeholder를 반환하므로, Advance 계산 전에 `pendingMinutes`, `needsReviewRecordCount`, `workproofRiskFlags`의 최소 신뢰도를 먼저 보강해야 한다.
- 모바일은 현재 `DemoSessionReducer`와 계산기 로직에 Finance 화면이 직접 결합되어 있어, 서버 응답 모델을 별도 계층으로 분리하지 않으면 Wage/Remittance까지 재작업 범위가 커진다.
- 로그인 상태와 데모 상태를 한 ViewModel에서 함께 들더라도, 인증 세션과 도메인 데모 상태는 별도 state로 유지해 책임을 분리한다.
- Finance 화면과 Home 카드가 같은 Advance 요약을 보여줄 예정이면 formatter와 상태 매핑을 공통 함수로 모아 중복을 줄인다.
- 디스클레이머, 차단 사유 라벨, tier 설명은 하드코딩 산개를 피하고 계약 또는 mapper 레이어에서 관리한다.

# Implementation Steps
1. Advance 계약 고정
   - PRD 7C와 `docs/DonDone_P0_API_Contract_v0.md`를 기준으로 endpoint/DTO/error code를 최종 확인한다.
   - `verifiedMinutes`, `pendingMinutes`, `blockReasonCodes[]` 노출 수준을 이번 slice 기준으로 확정한다.
2. 백엔드 도메인 뼈대 추가
   - `advance/api`, `advance/service`, `advance/repo`, `advance/model`, `advance/adapter` 패키지를 생성한다.
   - request/response DTO에 Bean Validation을 적용한다.
   - 신청/조회용 entity와 snapshot 저장 구조를 추가한다.
3. 백엔드 정책 계산 구현
   - WorkProof lane 1 월간 요약 + 활성 계약을 입력으로 eligibility 계산을 구현한다.
   - 계산 전에 WorkProof 요약의 placeholder 필드를 Advance 설명에 쓸 수 있는 수준으로 보강한다.
   - PRD의 tier / cap / hard stop 규칙을 데모 정책으로 코드화하되, 실제 금융 심사처럼 확장하지 않는다.
   - list/detail/create 응답에 explanation-first snapshot을 포함한다.
4. 백엔드 보안 및 공통 규칙 반영
   - idempotency 처리와 소유권 검증을 추가한다.
   - 필요 시 `ErrorCode`, 예외 처리, OpenAPI 문서를 보강한다.
5. Android 인증/세션 계층 추가
   - `/api/auth/login` 계약을 호출하는 auth repository와 access token 저장소를 추가한다.
   - 앱 시작 시 세션 복구, 로그아웃, 로그인 중 상태를 명시한다.
   - 기존 seed 자동 로그인은 제거하고, 저장된 토큰만 Advance 실연동에 사용한다.
6. Android 데이터 계층 추가
   - Advance contract model / repository interface / response mapper를 도입한다.
   - Advance repository는 현재 인증 세션의 access token을 입력으로 받아 조회한다.
   - 기존 `AdvanceCalculator` 기반 표시값 중 서버가 계산해야 하는 값과 클라이언트 format-only 값을 분리한다.
7. Android UI 상태 전환
   - Finance 미리받기 카드/상세 영역에 loading / empty / error / blocked / success 상태를 명시한다.
   - 신청 CTA는 eligibility 결과가 있을 때만 활성화하고, 차단 사유가 있으면 즉시 설명한다.
   - 신청 직후 목록이 비어 보이지 않도록 월 경계 보정과 detail merge 전략을 함께 둔다.
   - 최근 신청 이력은 클릭 시 detail bottom sheet로 연결하고, eligibility snapshot을 재표시한다.
   - Home 카드가 같은 요약을 쓰는 경우 최소 표시값만 재사용한다.
   - 로그인되지 않은 상태에서는 기존 앱 대신 로그인 화면을 우선 노출한다.
   - 기존 디자인 언어를 유지한 로그인 화면과 메뉴 내 로그아웃 액션을 추가한다.
8. 문서 및 계약 동기화
   - 구현 과정에서 계약이 바뀌면 `docs/DonDone_P0_API_Contract_v0.md`를 함께 수정한다.
   - 디스클레이머와 데모 한계 문구가 PRD와 어긋나지 않는지 확인한다.
9. 검증
   - 백엔드 단위/통합 테스트
   - Android 빌드 및 필요한 UI 상태 확인

# Test Plan
- Backend
  - `cd apps/dondone-backend && ./gradlew test`
  - 추가 대상:
    - eligibility 계산 규칙 테스트
    - hard stop / block reason 테스트
    - idempotency 중복 요청 테스트
    - 타인 workplace/request 404 은닉 테스트
    - validation 실패 테스트
- Mobile
  - `cd apps/dondone-mobile/android && ./gradlew assembleDebug`
  - 추가 대상:
    - auth session restore / login / logout 상태 테스트
    - mapper / reducer / state 변환 테스트
    - create 이후 list/detail merge 테스트
    - detail open 시 cache hit / fetch fallback 테스트
    - loading / blocked / success 상태 스냅샷 또는 UI 단위 테스트
- Manual
  - reflected 근무 증가 시 eligibility 증가 확인
  - block reason 존재 시 신청 CTA 차단 확인
  - 신청 후 월 이력/상세에서 동일 snapshot 재현 확인

# Review Focus
- PRD 7C와 실제 API/화면 계약이 어긋나지 않는지
- WorkProof 요약 재사용이 중복 계산 없이 일관되는지
- idempotency 및 404 은닉이 누락되지 않았는지
- Android에서 demo 계산 상태와 API 응답 상태가 뒤섞이지 않는지
- Android 로그인 세션 저장/복구가 seed 계정 자동 로그인 없이 동작하는지
- 디스클레이머와 evidence-first 메시지가 화면/응답 모두에 남아 있는지

# Worktree Split Decision
- Single lane

Advance는 이번 작업에서 backend DTO, error code, idempotency, WorkProof 재사용 규칙, Android 상태 모델이 함께 움직인다. 공통 계약과 보안 규칙이 고정되기 전에는 lane 분리가 충돌 위험을 키우므로 단일 lane으로 진행한다.

# Commit Plan
- `docs`: advance fullstack 실행 계획 및 계약 보강
- `backend`: advance API / policy / persistence / tests
- `mobile`: advance data layer / state / finance-home integration
- `docs` 또는 `test`: 계약 동기화와 검증 보강

# Open Questions
- eligibility 응답에서 `pendingMinutes`, `blockReasonCodes[]`를 사용자에게 그대로 노출할지, 문구 변환 후 제한 노출할지 최종 확정이 필요하다.
- Android 범위를 `Finance` 화면에 한정할지, `Home`의 미리받기 요약 카드까지 이번 slice에 포함할지 결정이 필요하다.
- Android 로그인 화면에서 seed 계정을 기본 채움할지, 빈 필드 시작으로 둘지 결정이 필요하다.
- `X-Demo-AsOf`를 Advance 조회 API에 이번 구현에서 실제 적용할지, 후속 데모 모드 lane으로 미룰지 결정이 필요하다.
- 신청 직후 status를 항상 `SUBMITTED`로 둘지, 데모 정책상 즉시 `APPROVED/REJECTED`까지 계산할지 결정이 필요하다.
- Android가 이번 slice에서 실백엔드 호출까지 포함할지, 아니면 auth/network 선행 전까지 contract-aligned fake repository로 마무리할지 최종 결정이 필요하다.

# Assumptions
- 이번 요청의 프론트 범위는 `apps/dondone-mobile/android`를 우선하며, mockup은 필요 시 참고만 한다.
- Android 로그인은 기존 백엔드 `/api/auth/login` 계약을 그대로 사용하고, P0 데모 범위상 access/refresh token 분리는 이번 작업 범위 밖으로 둔다.
- Android는 이번 slice에서 Advance의 login/read/create/detail 실서버 호출까지 포함하고, Wage/Documents 등 다른 도메인 실연동은 후속으로 둔다.
- P0 기준으로 Advance는 실제 금융 기능이 아니라 데모 시뮬레이션 응답과 UI만 제공한다.
- WorkProof lane 1 월간 요약은 Advance upstream으로 재사용 가능하되, 부족한 필드는 최소 보강으로 해결한다.
- Employer Support는 이번 slice의 직접 구현 대상이 아니며, 회사 확인 코드/재직 확인은 데모 정책 입력값 수준으로만 다룬다.
