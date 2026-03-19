# Source Inputs
- 사용자 요청:
  - Slice 5 `Correction request flow` backend 시작
  - 목록/상세/승인/반려 계약 초안 고정
  - `WorkProof` 반영 규칙과 감사로그 최소 뼈대 설계/구현
- 기준 문서:
  - `docs/web/README.md`
  - `docs/web/implementation-slices.md`
  - `docs/web/correction-request-flow.md`
  - `docs/web/shared-entity-validation.md`
  - `docs/web/employer-web-api-map.md`
  - `docs/execplans/active/2026-03-19-web-worker-directory-dashboard-read-model.md`
  - `docs/reviews/active/2026-03-19-web-worker-read-model-followups.md`
- 현재 코드 근거:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofRequestValidator.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProofAuditLog.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerAccessScopeService.java`
  - `apps/dondone-web/src/pages/issues/IssuesQueuePage.tsx`
  - `apps/dondone-web/src/pages/issues/model/issuesQueueData.ts`

# Goal
Slice 5에서 employer web correction queue foundation을 열고, `GET /api/employer/correction-requests`, `GET /api/employer/correction-requests/{requestId}`, `POST /api/employer/correction-requests/{requestId}/approve`, `POST /api/employer/correction-requests/{requestId}/reject`를 membership-equivalent scope와 단일한 `WorkProof` 반영 규칙으로 제공한다.

# In Scope
- employer correction request queue read-model endpoint 추가
  - `GET /api/employer/correction-requests`
  - `GET /api/employer/correction-requests/{requestId}`
- employer correction request command endpoint 추가
  - `POST /api/employer/correction-requests/{requestId}/approve`
  - `POST /api/employer/correction-requests/{requestId}/reject`
- correction request shared entity foundation 추가
  - `CorrectionRequest`
  - `CorrectionDecisionAudit`
- employer scope를 `EmployerProfile.companyId + defaultWorkplaceId` 기준으로 제한
- 승인 시 `CorrectionRequest` 상태 변경, `WorkProof` 시간 반영, audit 기록을 한 transaction으로 묶는 규칙 고정
- backend 테스트 추가
- Slice 5 계약 문서 갱신

# Out of Scope
- 기존 worker direct edit client를 correction request submit flow로 바꾸는 mobile 변경
- 기존 `/api/auth/*`, `/api/wage/*` API 변경
- mobile 변경
- 반경 밖 `check-out` review와 correction request 도메인 통합
- 비동기 재계산 파이프라인
- attachment 파일 업로드 저장소 구현
- multi-workplace switcher

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/api/dto/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/correction/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/correction/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/service/WorkProofRequestValidator.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProofAuditLog.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/employer/**`

## Mobile
- 없음

## Docs
- `docs/web/implementation-slices.md`
- `docs/web/correction-request-flow.md`
- `docs/web/employer-web-api-map.md`
- 본 실행계획 문서

## Shared
- `docs/web/shared-entity-validation.md`
- `docs/reviews/active/`

# Contract Changes
- 신규 correction queue query DTO
  - `query`
  - `statuses`
  - `page`
  - `size`
- 신규 correction request summary response
  - `requestId`
  - `workProofId`
  - `workerId`
  - `workerName`
  - `workerEmail`
  - `role`
  - `workDate`
  - `originalClockInAt`
  - `originalClockOutAt`
  - `requestedClockInAt`
  - `requestedClockOutAt`
  - `reason`
  - `requestedAt`
  - `status`
- 신규 correction request detail response
  - summary foundation 필드
  - `attachmentCount`
  - `attachments[]`
  - `decisionByAccountId`
  - `decisionByName`
  - `decisionAt`
  - `decisionMemo`
  - `rejectReasonCode`
- 신규 approve command request
  - `decisionMemo`
- 신규 reject command request
  - `decisionMemo`
  - `rejectReasonCode`
- `CorrectionRequest.status`
  - `PENDING`
  - `APPROVED`
  - `REJECTED`

# Security Notes
- `/api/employer/**` role 제한은 기존 `SecurityConfig`를 그대로 사용한다.
- `requestId`를 받는 endpoint는 request snapshot의 `companyId/workplaceId`가 employer scope와 일치하는지 검증한다.
- 승인/반려는 `PENDING` 상태에서 한 번만 허용한다.
- 승인 시 `WorkProof` 반영, correction status 변경, decision audit 기록이 하나의 transaction으로 묶여야 한다.
- 기존 worker 수정 endpoint 권한을 넓히지 않는다.
- 반경 밖 `check-out` review는 correction request와 다른 축으로 유지한다.

# Maintainability Notes
- employer correction queue는 `WorkProofService.update(...)`를 억지로 우회 호출하지 말고, correction service에서 `WorkProofRequestValidator`와 audit 기록 로직을 명시적으로 재사용한다.
- correction request entity는 worker/employer 공통으로 확장될 수 있으므로 employer DTO와 분리된 shared model로 둔다.
- request list/detail와 approve/reject에서 같은 scope 해석을 재사용해 ad-hoc 검증을 늘리지 않는다.
- attachment 저장소가 아직 없으므로 metadata/json만 다루고 파일 업로드를 이번 slice에 섞지 않는다.

# Implementation Steps
1. Slice 5 execplan을 추가하고 correction request 계약/가정/리스크를 문서화한다.
2. `CorrectionRequest`, `CorrectionDecisionAudit` model/repo를 추가한다.
3. employer correction queue query/response DTO와 controller/service를 추가한다.
4. correction request list/detail read-model을 employer scope 기준으로 조합한다.
5. approve/reject command DTO와 service를 추가한다.
6. approve에서 `WorkProofRequestValidator`, `WorkProof.updateTimes`, `WorkProofAuditLog`를 재사용해 단일 transaction 처리 규칙을 고정한다.
7. integration test로 scope/authz/list/detail/approve/reject 회귀를 닫는다.
8. Slice 5 상태와 contract 문서를 갱신한다.

# Test Plan
- `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.EmployerCorrectionRequestIntegrationTest --tests com.workproofpay.backend.employer.EmployerWorkerReadModelIntegrationTest --tests com.workproofpay.backend.employer.EmployerAccessScopeServiceTest`
- 필요 시 전체 employer 회귀:
  - `cd apps/dondone-backend && .\\gradlew.bat test --tests com.workproofpay.backend.employer.*`
- 검증 항목:
  - scoped correction request만 queue에 노출
  - pending/approved/rejected 필터와 검색 기본 동작
  - detail에서 decision 정보와 request snapshot 노출
  - approve 시 request status, `WorkProof`, `WorkProofAuditLog`, `CorrectionDecisionAudit`가 함께 반영
  - reject 시 `WorkProof`는 변경되지 않고 request status/audit만 반영
  - out-of-scope request 승인/반려 차단
  - 이미 처리된 request 재처리 차단

# Review Focus
- correction request가 기존 worker 수정 endpoint 권한을 우회하지 않는지
- approve transaction이 부분 성공 없이 묶여 있는지
- `WorkProof` 반영 규칙이 `correction-request-flow.md`와 일치하는지
- request scope가 company/workplace 기준으로 충분히 제한되는지
- 반경 밖 `check-out` review와 correction request 도메인을 섣불리 섞지 않는지

# Worktree Split Decision
Single lane

이번 작업은 shared entity 추가, employer API contract, `WorkProof` 업데이트 규칙, audit 기록, authz scope, 테스트, 문서가 함께 움직인다. correction request는 공통 도메인 영향이 큰 편이라 병렬 레인으로 나누면 merge risk가 높다.

# Commit Plan
1. `docs: scope slice5 correction request flow`
2. `feat: add employer correction request queue foundation`
3. `test: cover employer correction request approval flow`

# Open Questions
- worker-side request 생성 endpoint는 열었고, mobile/client migration을 어느 시점에 어떤 범위로 붙일지
- attachment metadata는 request detail에 `type`, `fileName`까지만 노출하고 storage/download contract를 언제 열지
- 기존 worker direct edit endpoint `PATCH /api/workproof/{id}`를 legacy 유지/deprecated/제거 중 무엇으로 둘지
- 승인 후 wage summary와 dashboard invalidation을 명시적 캐시 무효화 없이 조회 시 재계산만으로 충분히 볼지

# Assumptions
- Slice 5 1차 목표는 employer-side correction queue foundation을 여는 것이지 worker 생성 흐름까지 닫는 것이 아니다.
- worker correction request create backend는 additive surface로만 열고, mobile/client migration은 현재 slice 범위 밖에 둔다.
- correction request는 시간 수정 요청만 대상으로 시작하고 반경 밖 `check-out` review는 별도 축으로 유지한다.
- request snapshot의 `companyId/workplaceId`는 employer scope 검증에 사용할 수 있는 조직 연결 근거로 본다.
- approve 시 `editReason`에는 correction request의 요청 사유를 연결하고, `memo`는 기존 WorkProof 값을 유지한다.
- attachment는 파일 자체가 아니라 count/json metadata 수준으로만 다룬다.
