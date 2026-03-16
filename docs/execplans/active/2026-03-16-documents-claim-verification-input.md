# Source Inputs
- Root guidance:
  - `AGENTS.md`
  - `apps/dondone-backend/AGENTS.md`
- Private context:
  - `.private/kwanwoo/context/CURRENT.md`
  - `.private/kwanwoo/context/NEXT.md`
  - `.private/kwanwoo/context/ROADMAP.md`
  - `.private/kwanwoo/decisions/DECISIONS.md`
- Product docs:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
  - `docs/DonDone_P0_Functional_Spec_v0.md`
- Existing plan/code:
  - `docs/execplans/active/2026-03-16-wage-verification-contract.md`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/WageController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageVerification.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/api/dto/response/WageVerificationDetailResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/DocumentsController.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

# Goal
`codex/documents-claim-verification-input` 브랜치에서 Documents / Claim의 다음 슬라이스를 `wageVerificationId` 중심 입력축으로 고정한다. 이번 작업의 핵심은 `POST /api/documents/proof-packs`, `POST /api/claim/preparations` 계약과 verification snapshot 재사용 경계를 먼저 정리하고, 필요하면 그 계약을 따르는 backend skeleton 또는 최소 구현까지 연다.

# In Scope
- `POST /api/documents/proof-packs` request/response/error/source-of-truth 정리
- `POST /api/claim/preparations` request/response/error/source-of-truth 정리
- Documents / Claim가 `wageVerificationId`를 단일 anchor로 읽는 내부 규칙 정리
- verification snapshot 재사용 범위와 재조회가 필요한 추가 데이터 범위 정리
- legacy `POST /api/wage/deposits`, `GET /api/wage/summary` transition 메모 정리
- 필요 시 최소 backend 구현:
  - documents create endpoint
  - claim preparation create endpoint
  - verification ownership 재사용 서비스
  - contract test 또는 integration test

# Out of Scope
- 실제 PDF 렌더링, 파일 저장, download URL 발급 구현
- `POST /api/documents/claim-kits` 전체 구현
- `GET /api/documents`, `GET /api/documents/{documentId}` 상세 구현
- `GET /api/claim/preparations/{preparationId}` 구현
- employer confirmation workflow
- WorkProof attachments / missing / modifications 후속 lane 구현
- historical contract snapshot 전용 테이블 또는 별도 migration
- mobile UI 연결

# Affected Modules
## Backend
- Existing:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/DocumentsController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/model/WageVerification.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/repo/WageVerificationRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/security/JwtAuthenticationFilter.java`
- New candidates:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/dto/request/CreateProofPackRequest.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/api/dto/response/DocumentGenerationAcceptedResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/service/DocumentsService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/documents/model/DocumentType.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/api/ClaimController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/api/dto/request/CreateClaimPreparationRequest.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/api/dto/response/ClaimPreparationResponse.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/claim/service/ClaimService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageVerificationQueryService.java`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-16-documents-claim-verification-input.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- 필요 시 `docs/DonDone_P0_Functional_Spec_v0.md`

## Shared
- `ApiResponse<T>` envelope 유지
- JWT 보호 규칙 유지
- `Idempotency-Key` 사용 강도:
  - Documents: 필수
  - Claim preparation: 권장 유지

# Contract Changes
- `POST /api/documents/proof-packs`
  - request source-of-truth는 `wageVerificationId` 하나로 고정한다.
  - 기존 문서 초안의 `month`, `workplaceId`는 이번 슬라이스에서 제거한다.
  - 이유: verification snapshot이 이미 `month`, `workplaceId`, contract/pay snapshot, evidence record IDs를 보존하고 있어 중복 입력이 drift를 만든다.
  - response는 `202 Accepted`를 유지한다:
    - `requestId`
    - `documentType`
    - `status`
    - `pollUrl`
  - 주요 에러:
    - `404 WAGE_VERIFICATION_NOT_FOUND`
    - `409 DOCUMENT_DUPLICATE_REQUEST`
- `POST /api/claim/preparations`
  - request는 아래 필드로 유지한다:
    - `wageVerificationId` 필수
    - `claimKitDocumentId` 선택
    - `locale` 필수
    - `tone` 필수 enum (`DEFAULT`, `POLITE`, `SHORT`)
  - `claimKitDocumentId`가 들어오면 같은 사용자 소유이면서 같은 verification 문맥에서 생성된 문서만 허용한다.
  - response는 `201 Created`를 유지한다:
    - `preparationId`
    - `status`
    - `summaryText`
    - `checklist[]`
    - `suggestedRoutes[]`
    - `relatedDocuments[]`
  - 주요 에러:
    - `404 WAGE_VERIFICATION_NOT_FOUND`
    - `404 CLAIM_KIT_NOT_FOUND`
- downstream 입력 원칙:
  - Documents / Claim는 `depositId`, `yearMonth`, `workplaceId`를 직접 받지 않는다.
  - downstream 문맥 식별자는 `wageVerificationId`로 고정한다.

# Security Notes
- 두 endpoint 모두 JWT 보호 대상으로 유지한다.
- `wageVerificationId` 조회는 verification 소유권 검증을 반드시 거친다.
- 타인 verification 또는 타인 claim kit는 `404`로 숨긴다.
- Documents `Idempotency-Key`는 필수다. 같은 verification로 중복 렌더 요청이 와도 최초 요청만 유효해야 한다.
- Claim preparation은 v0에서 `Idempotency-Key`를 권장으로 유지하되, 구현 시 없더라도 worker가 새 요약을 다시 만드는 흐름을 막지 않는다.
- Wage verification 결과는 참고용 추정 + 근거다. Documents / Claim도 최종 법률/재무 판정처럼 보이게 만들지 않는다.

# Maintainability Notes
- `WageVerification`은 Documents / Claim의 읽기 anchor이고, legacy `WageDeposit`와 섞지 않는다.
- documents/claim가 verification 상세 DTO를 HTTP처럼 다시 조립하는 대신, 내부 query service나 repository 기반 read model을 재사용해 source drift를 줄인다.
- `proof-packs` request에서 `month`, `workplaceId`를 계속 받으면 검증 분기와 mismatch 에러 처리만 늘어난다. 이번 슬라이스에서 제거한다.
- verification snapshot에 없는 표시 정보는 "재계산"이 아니라 "보조 조회"로만 채운다.
  - 예: record row detail, workplace 이름
- historical contract snapshot 전용 기능은 이번 슬라이스에서 보류한다.
  - 현재 `WageVerification`이 pay unit / base pay / normalized wage / estimated totals / threshold를 이미 저장하므로 wage explanation 용도에는 충분하다.

# Implementation Steps
1. `proof-packs`와 `claim/preparations`의 request source-of-truth를 `wageVerificationId` 기준으로 정리한다.
2. `docs/DonDone_P0_API_Contract_v0.md`에 아래를 반영한다.
   - proof pack request 단순화
   - claim preparation의 optional claim kit 검증 규칙
   - verification snapshot 재사용 메모
   - legacy transition 메모
3. 필요 시 `WageVerification` 내부 조회를 재사용하는 `WageVerificationQueryService`를 추가한다.
4. documents feature skeleton을 `proof-packs` create endpoint 기준으로 확장한다.
5. claim feature skeleton을 새로 만들고 `preparations` create endpoint를 연다.
6. 최소 구현 시 응답은 계약 shape를 맞추되, 실제 PDF/job/storage는 placeholder status 또는 fake requestId로 시작한다.
7. 테스트를 추가한다.
   - verification 소유권 은닉
   - proof pack request가 verification anchor만 받는지
   - claim preparation이 verification snapshot 기반 문구를 생성하는지
   - legacy summary/deposit가 downstream 입력으로 우회되지 않는지
8. 완료 후 private context / decisions 동기화 여부를 판단한다.

# Test Plan
- 문서화만 수행 시:
  - 문서 diff self-review
- backend 구현 포함 시:
  - `./gradlew.bat test --tests com.workproofpay.backend.documents.*`
  - `./gradlew.bat test --tests com.workproofpay.backend.claim.*`
  - 또는 통합 테스트로 묶일 경우:
    - `./gradlew.bat test --tests com.workproofpay.backend.wage.WageDemoIntegrationTest`
    - `./gradlew.bat test --tests com.workproofpay.backend.documents.DocumentsIntegrationTest`
    - `./gradlew.bat test --tests com.workproofpay.backend.claim.ClaimPreparationIntegrationTest`
- 환경 blocker가 있으면 Java / Testcontainers / Postgres 분리해서 기록한다.

# Review Focus
- `wageVerificationId` 하나로 Documents / Claim 입력축이 실제로 단순해졌는지
- proof pack request에서 제거한 `month`, `workplaceId`가 다른 흐름을 깨지 않는지
- Claim preparation이 verification snapshot만으로 충분한 facts를 얻는지
- verification snapshot 밖의 데이터가 "재계산"이 아니라 "보조 조회"로 제한되는지
- legacy `deposits/summary` 공존 메모가 구현 의도를 왜곡하지 않는지
- JWT 보호와 404 은닉 규칙이 documents/claim에도 그대로 유지되는지

# Worktree Split Decision
- Single lane

이번 슬라이스는 shared API contract, `WageVerification` 재사용 경계, documents/claim feature skeleton, legacy transition 메모가 함께 움직인다. 같은 입력축을 여러 lane에서 동시에 바꾸면 request shape와 downstream 의미가 쉽게 흔들리므로 단일 lane이 맞다.

# Commit Plan
- `docs: add documents claim verification-input execplan`
- `docs: refine proof-pack and claim preparation contracts`
- `feat: add documents and claim verification-anchor endpoints`
- `test: cover documents claim verification-anchor contracts`

# Open Questions
- `POST /api/documents/claim-kits`도 이번 슬라이스에서 `wageVerificationId` 단일 입력으로 함께 줄일지 여부
- Claim preparation `summaryText`를 템플릿 기반으로 바로 고정할지, Copilot facts 조립 계층과 나중에 합칠지 여부
- proof pack 응답의 `pollUrl` 형식을 실제 문서 상세 경로로 고정할지 job polling 경로로 둘지 여부
- verification snapshot에 없는 workplace 표시명/사용자 표시명은 documents layer에서 어디서 읽을지 여부

# Assumptions
- Documents / Claim의 downstream anchor는 `wageVerificationId`다.
- proof pack 생성은 verification snapshot을 기준으로 설명하고, WorkProof row detail은 snapshot의 `recordIds`로 보조 조회한다.
- claim preparation은 wage verification snapshot만으로도 summary/checklist/route 선택의 핵심 facts를 만들 수 있다.
- 현재 `WageVerification`이 저장하는 contract/pay snapshot이면 historical contract snapshot 전용 기능 없이도 v0 Documents / Claim를 시작하기에 충분하다.
- legacy `POST /api/wage/deposits`, `GET /api/wage/summary`는 기존 화면/데모 호환용으로 남기되, 새 Documents / Claim 입력으로는 사용하지 않는다.
