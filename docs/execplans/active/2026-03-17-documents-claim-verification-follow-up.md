# Source Inputs
- 작업 가이드:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
  - `.private/kwanwoo/README.md`
- 개인 문맥:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/context/ROADMAP.md`
  - `.private/kwanwoo/decisions/DECISIONS.md`
- PRD/계약:
  - `docs/DonDone_PRD_v1.5.md` 7H Documents / Instant Claim, P0 범위/원칙
  - `docs/DonDone_P0_API_Contract_v0.md` 4.4, 5.2~5.5, 6.2~6.3
  - `docs/DonDone_P0_Functional_Spec_v0.md` Documents / Instant Claim 핵심 흐름
- 기존 계획/구현:
  - `docs/execplans/active/2026-03-16-documents-claim-verification-input.md`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/documents/DocumentsIntegrationTest.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/claim/ClaimPreparationIntegrationTest.java`

# Goal
Documents / Claim verification-anchor 후속 슬라이스를 구현한다. 이번 목표는 `wageVerificationId`를 공통 anchor로 유지하면서 `POST /api/documents/claim-kits`, `GET /api/claim/preparations/{preparationId}`, documents detail/polling, `GET /api/wage/verifications/{verificationId}`의 `relatedActions`를 실제 downstream readiness와 연결하는 것이다.

# In Scope
- `POST /api/documents/claim-kits`를 `wageVerificationId` anchor 기준으로 구현
- documents polling/detail 재조회 경로 구현
- `GET /api/claim/preparations/{preparationId}` 구현
- wage verification detail의 `relatedActions`를 placeholder가 아닌 실제 downstream 상태 기반으로 계산
- 관련 DTO / repo / error contract / 통합 테스트 업데이트
- `docs/DonDone_P0_API_Contract_v0.md`와 필요한 실행 문서 동기화

# Out of Scope
- 실제 PDF 렌더링, job worker, storage, download URL 실구현
- documents 목록 조회 전체 구현
- transfer receipt 관련 후속
- Copilot claim-summary 연동
- historical contract snapshot 전용 저장 구조
- 모바일 UI 연결

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/DocumentsController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/dto/request/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/dto/response/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/model/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/repo/DocumentGenerationRequestRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/service/DocumentsService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/api/ClaimController.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/api/dto/response/*`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/repo/ClaimPreparationRepository.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/service/ClaimService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`

## Mobile
- 없음. 다만 downstream route / readiness 필드 의미가 바뀌므로 계약 추적 필요.

## Docs
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/execplans/active/2026-03-17-documents-claim-verification-follow-up.md`

## Shared
- `ApiResponse<T>` envelope 유지
- JWT 보호 규칙 유지
- `Idempotency-Key` 필수/권장 강도 유지

# Contract Changes
- `POST /api/documents/claim-kits`
  - request에서 `month`, `workplaceId` 제거
  - `wageVerificationId` 필수 유지
  - `includeAttachments`, `format`만 선택 입력으로 유지
  - `202 Accepted` 응답과 `requestId`, `documentType`, `status`, `pollUrl` 유지
- documents polling/detail
  - `pollUrl`이 실제 조회 가능한 endpoint를 가리키도록 구현
  - 문서 detail 응답은 최소 `documentId`, `type`, `status`, `title`, `summary`, `relatedLinks`, `createdAt`, `updatedAt`, `downloadable`를 맞춘다
- `GET /api/claim/preparations/{preparationId}`
  - create 응답과 같은 본문에 `createdAt`을 추가해 재조회 가능하게 한다
- `GET /api/wage/verifications/{verificationId}`
  - `relatedActions.proofPackReady`, `claimKitReady`, `instantClaimAvailable`를 verification status만 보지 않고 downstream 상태와 연결한다

# Security Notes
- 새 endpoint 모두 JWT 보호 대상으로 유지한다
- verification, document, claim preparation 조회는 모두 user ownership 검증을 거친다
- 타인 리소스는 `404`로 숨긴다
- documents create는 `Idempotency-Key` 필수 유지
- Claim는 여전히 반자동 지원이며 최종 법률/재무 판단처럼 보이게 만들지 않는다

# Maintainability Notes
- documents / claim는 verification ownership 조회를 재사용하고 별도 판정 로직을 복제하지 않는다
- claim preparation detail은 저장된 summaryText를 source-of-truth로 두고, checklist/routes/related documents는 deterministic read 조립으로 유지한다
- documents entity를 job/status placeholder로 재사용하되, endpoint naming은 request/detail 역할이 드러나게 분리한다
- `relatedActions` 계산은 WageService 내부 임시 boolean이 아니라 documents/claim read 상태를 묶는 전용 helper로 분리한다

# Implementation Steps
1. follow-up contract 기준으로 `claim-kits`, claim detail, documents polling/detail shape를 고정한다
2. documents feature에 claim kit create, request poll/detail read DTO와 service/repo 메서드를 추가한다
3. claim feature에 preparation detail read를 추가하고 저장 필드 재사용 경계를 정리한다
4. wage verification detail의 `relatedActions`를 documents/claim 실제 상태 기반으로 다시 계산한다
5. documents / claim / wage 통합 테스트를 보강한다
6. API 계약 문서를 구현 상태에 맞게 갱신한다

# Test Plan
- `./gradlew.bat test --tests com.workproofpay.backend.documents.DocumentsIntegrationTest`
- `./gradlew.bat test --tests com.workproofpay.backend.claim.ClaimPreparationIntegrationTest`
- 필요 시 verification detail 회귀를 위해 wage 관련 테스트 1개 추가 후 해당 테스트만 실행
- 여건이 되면 `./gradlew.bat test`까지 확인

# Review Focus
- `POST /api/documents/claim-kits`가 proof pack과 같은 verification anchor 규칙을 따르는지
- documents polling/detail 경로가 실제로 `pollUrl`과 연결되는지
- claim preparation detail이 create와 동일한 의미를 유지하는지
- `relatedActions`가 단순 status 파생이 아니라 downstream readiness를 올바르게 반영하는지
- ownership / 404 은닉이 새 read endpoint에도 동일하게 적용되는지

# Worktree Split Decision
- Single lane

DTO, 문서 엔티티, claim 저장/재조회, wage detail `relatedActions`가 모두 같은 verification anchor 의미를 공유한다. 공통 응답과 readiness semantics가 아직 움직이는 상태라 parallel-safe하지 않다.

# Commit Plan
- `docs: documents claim verification follow-up execplan 추가`
- `feat: documents claim verification-anchor follow-up 구현`
- `test: documents claim follow-up 통합 테스트 보강`
- `docs: P0 documents claim 계약 갱신`

# Open Questions
- documents polling endpoint를 `/api/documents/requests/{requestId}`로 둘지, detail endpoint로 흡수할지
- claim kit detail summary를 proof pack 생성 여부와 얼마나 강하게 연결할지
- wage verification `relatedActions`에서 `READY`와 `QUEUED`를 어떤 수준까지 모두 usable로 볼지

# Assumptions
- poll/read 분리는 유지하되, `pollUrl`은 실제로 호출 가능한 request-status endpoint로 연결한다
- documents detail은 현재 `DocumentGenerationRequest`를 최소 문서 read model로 재사용한다
- claim preparation detail은 저장된 summaryText와 식별자 필드를 source-of-truth로 두고, checklist/routes는 locale + verification snapshot 기준으로 재조립한다
- `relatedActions`는 verification이 `CHECK_REQUIRED`일 때만 활성화하되, 이미 생성된 proof pack / claim kit / preparation 상태를 함께 반영한다
