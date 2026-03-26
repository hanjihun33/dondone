# DonDone 백엔드 API 명세서 (구현 기준) v1

## 문서 정보
- 작성일: 2026-03-24
- 기준 코드: `apps/dondone-backend/src/main/java/com/workproofpay/backend`
- 기준 보안 설정: `shared/config/SecurityConfig.java`
- 상태: Draft v1 (구현 카탈로그)
- 목적: 현재 코드 기준으로 실제 제공 중인 API endpoint/접근권한/request/response 타입을 한 문서에서 확인할 수 있게 한다.

## 요구사항 확인

| 항목 | 이번 문서 기준 |
| --- | --- |
| expected behavior | PRD/초안 문서가 아니라 **현재 백엔드 구현 코드 기준**으로 API를 조회할 수 있어야 한다. |
| exact scope | `*Controller.java` 기준 endpoint 93개, 공통 인증/응답/에러 규칙, idempotency header 사용 지점 |
| contract changes | 런타임 계약 변경 없음. 문서만 신규 작성 |
| security impact | `SecurityConfig` 기준 public/role endpoint 분류를 명시 |
| non-functional impact | 서버 동작 변경 없음. 문서 drift 최소화를 위해 controller#method/DTO 타입을 함께 기록 |

## 포함/제외 범위

### 포함
- Worker, Employer, Admin API 전체 카탈로그
- `ApiResponse<T>` 응답 envelope 기준
- `ErrorCode` 기반 공통 오류 코드 분류
- `Idempotency-Key` required/optional 엔드포인트

### 제외
- DTO 필드 단위 상세 제약(필수/최소/최대) 전수 표
- 예시 payload(JSON) 전수
- 미구현(PRD 스케치만 존재) API

## 공통 규칙

### Base Path
- Worker/Admin/Employer API: `/api/**`
- Health: `/health`

### 인증/권한 (`SecurityConfig` 기준)
- Public
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `/api/employer-auth/**`
  - `GET /health`
  - Swagger/OpenAPI 경로
- `ROLE_EMPLOYER`
  - `/api/employer/**`
- `ROLE_ADMIN`
  - `/api/admin/**`
- `ROLE_USER` or `ROLE_ADMIN`
  - 그 외 `/api/**`

### 공통 헤더
- `Authorization: Bearer <token>` (보호 API)
- `Idempotency-Key` (일부 생성 API)

### 공통 응답 형식
`ApiResponse<T>`

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {},
  "details": null
}
```

- 생성 성공(`201`) 시 `code=CREATED`, `message="Created"`
- 비동기 수락(`202`) 시 `code=ACCEPTED`

### 공통 에러 형식

```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "Invalid input value",
  "data": null,
  "details": [
    {
      "field": "month",
      "reason": "month must follow YYYY-MM"
    }
  ]
}
```

- 소스: `shared/exception/GlobalExceptionHandler.java`, `shared/exception/ErrorCode.java`

### 공통 에러 코드 분류 (요약)
- 400: `INVALID_INPUT_VALUE`, `INVALID_REQUEST`, 도메인별 validation 오류
- 401: `UNAUTHORIZED`, `INVALID_TOKEN`, `INVALID_CREDENTIALS`
- 403: `FORBIDDEN`, `EMPLOYER_PROFILE_INACTIVE`
- 404: `*_NOT_FOUND`
- 409: 상태 충돌/중복/정책 위반 (`*_ALREADY_*`, `*_NOT_ALLOWED`, `*_REQUIRED` 등)
- 413: `FILE_TOO_LARGE`
- 500: `INTERNAL_ERROR`

## Idempotency-Key 적용 API
- `required`
  - `POST /api/documents/proof-packs`
  - `POST /api/documents/claim-kits`
  - `POST /api/workproof/documents`
- `optional`
  - `POST /api/advance/requests`
  - `POST /api/remittance/transfers`
  - `POST /api/vault/deposits`
  - `POST /api/vault/withdrawals`

## 구현 API 카탈로그
- 총 endpoint 수: **93**
- 표기 기준
  - `Req Body DTO`: `@RequestBody` 타입
  - `Response DTO`: `ApiResponse<T>`의 `T` (또는 raw response)
  - `Mapping 조건`: 동일 path 분기 시 `params` 조건
## admin
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/admin/advance/requests` | ADMIN | `-` | `AdminAdvanceRequestListResponse` | `-` | `-` | `backend/admin/api/AdminAdvanceRequestController.java#getRequests` |
| `POST` | `/api/admin/advance/requests/{requestId}/approve` | ADMIN | `-` | `Void` | `-` | `-` | `backend/admin/api/AdminAdvanceRequestController.java#approve` |
| `POST` | `/api/admin/advance/requests/{requestId}/reject` | ADMIN | `-` | `Void` | `-` | `-` | `backend/admin/api/AdminAdvanceRequestController.java#reject` |
| `GET` | `/api/admin/employers/companies` | ADMIN | `-` | `AdminEmployerCompaniesResponse` | `-` | `-` | `backend/admin/api/AdminEmployerCompanyController.java#getCompanies` |
| `POST` | `/api/admin/employers/companies` | ADMIN | `AdminCreateEmployerCompanyRequest` | `AdminEmployerCompanyCreatedResponse` | `-` | `-` | `backend/admin/api/AdminEmployerCompanyController.java#createCompany` |
| `GET` | `/api/admin/employers/companies/{companyId}/employers` | ADMIN | `-` | `AdminEmployerCompanyEmployersResponse` | `-` | `-` | `backend/admin/api/AdminEmployerCompanyController.java#getCompanyEmployers` |
| `GET` | `/api/admin/employers/companies/{companyId}/signup-code` | ADMIN | `-` | `AdminEmployerSignupCodeResponse` | `-` | `-` | `backend/admin/api/AdminEmployerCompanyController.java#getEmployerSignupCode` |
| `GET` | `/api/admin/remittance/jobs` | ADMIN | `-` | `RemittanceOpsJobListResponse` | `-` | `-` | `backend/remittance/api/RemittanceAdminController.java#getJobs` |
| `GET` | `/api/admin/remittance/summary` | ADMIN | `-` | `RemittanceOpsSummaryResponse` | `-` | `-` | `backend/remittance/api/RemittanceAdminController.java#getSummary` |
| `GET` | `/api/admin/remittance/transfers` | ADMIN | `-` | `RemittanceOpsTransferListResponse` | `-` | `-` | `backend/remittance/api/RemittanceAdminController.java#getTransfers` |
| `POST` | `/api/admin/remittance/transfers/{transferId}/retry` | ADMIN | `-` | `RemittanceAdminActionResponse` | `-` | `-` | `backend/remittance/api/RemittanceAdminController.java#retryTransfer` |
| `POST` | `/api/admin/remittance/wallets/{userId}/retry-funding` | ADMIN | `-` | `WalletResponse` | `-` | `-` | `backend/remittance/api/RemittanceAdminController.java#retryWalletFunding` |

## advance
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/advance/eligibility` | USER or ADMIN | `-` | `AdvanceEligibilityResponse` | `-` | `-` | `backend/advance/api/AdvanceController.java#getEligibility` |
| `GET` | `/api/advance/requests` | USER or ADMIN | `-` | `AdvanceRequestListResponse` | `-` | `-` | `backend/advance/api/AdvanceController.java#getRequests` |
| `POST` | `/api/advance/requests` | USER or ADMIN | `CreateAdvanceRequest` | `AdvanceRequestResponse` | `optional` | `-` | `backend/advance/api/AdvanceController.java#createRequest` |
| `GET` | `/api/advance/requests/{requestId}` | USER or ADMIN | `-` | `AdvanceRequestDetailResponse` | `-` | `-` | `backend/advance/api/AdvanceController.java#getRequest` |

## auth
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Public | `LoginRequest` | `LoginResponse` | `-` | `-` | `backend/auth/api/AuthController.java#login` |
| `GET` | `/api/auth/me` | USER or ADMIN | `-` | `MeResponse` | `-` | `-` | `backend/auth/api/AuthController.java#me` |
| `PUT` | `/api/auth/me` | USER or ADMIN | `UpdateProfileRequest` | `MeResponse` | `-` | `-` | `backend/auth/api/AuthController.java#updateMe` |
| `PUT` | `/api/auth/me/company-code` | USER or ADMIN | `UpdateCompanyCodeRequest` | `MeResponse` | `-` | `-` | `backend/auth/api/AuthController.java#updateCompanyCode` |
| `POST` | `/api/auth/me/worker-registration-code` | USER or ADMIN | `RedeemWorkerRegistrationCodeRequest` | `WorkerCompanyRegistrationResponse` | `-` | `-` | `backend/auth/api/WorkerCompanyRegistrationController.java#redeem` |
| `POST` | `/api/auth/signup` | Public | `SignupRequest` | `MeResponse` | `-` | `-` | `backend/auth/api/AuthController.java#signup` |

## claim
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/claim/preparations` | USER or ADMIN | `CreateClaimPreparationRequest` | `ClaimPreparationResponse` | `-` | `-` | `backend/claim/api/ClaimController.java#createPreparation` |
| `GET` | `/api/claim/preparations/{preparationId}` | USER or ADMIN | `-` | `ClaimPreparationDetailResponse` | `-` | `-` | `backend/claim/api/ClaimController.java#getPreparation` |

## demo
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/demo/state` | USER or ADMIN | `-` | `DemoStateResponse` | `-` | `-` | `backend/demo/api/DemoController.java#getState` |

## documents
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/documents/claim-kits` | USER or ADMIN | `CreateClaimKitRequest` | `DocumentGenerationAcceptedResponse` | `required` | `-` | `backend/documents/api/DocumentsController.java#createClaimKit` |
| `GET` | `/api/documents/ping` | USER or ADMIN | `-` | `Map<String, String>` | `-` | `-` | `backend/documents/api/DocumentsController.java#ping` |
| `POST` | `/api/documents/proof-packs` | USER or ADMIN | `CreateProofPackRequest` | `DocumentGenerationAcceptedResponse` | `required` | `-` | `backend/documents/api/DocumentsController.java#createProofPack` |
| `GET` | `/api/documents/requests/{requestId}` | USER or ADMIN | `-` | `DocumentGenerationRequestStatusResponse` | `-` | `-` | `backend/documents/api/DocumentsController.java#getRequestStatus` |
| `GET` | `/api/documents/{documentId}` | USER or ADMIN | `-` | `DocumentDetailResponse` | `-` | `-` | `backend/documents/api/DocumentsController.java#getDocumentDetail` |
| `GET` | `/api/documents/{documentId}/download` | USER or ADMIN | `-` | `byte[]` | `-` | `-` | `backend/documents/api/DocumentsController.java#downloadDocument` |

## employer-auth
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/employer-auth/invitations/accept` | Public | `EmployerInvitationAcceptRequest` | `EmployerAuthResponse` | `-` | `-` | `backend/employerauth/api/EmployerAuthController.java#acceptInvitation` |
| `POST` | `/api/employer-auth/login` | Public | `EmployerLoginRequest` | `EmployerAuthResponse` | `-` | `-` | `backend/employerauth/api/EmployerAuthController.java#login` |
| `POST` | `/api/employer-auth/signup` | Public | `EmployerSignupRequest` | `EmployerAuthResponse` | `-` | `-` | `backend/employerauth/api/EmployerAuthController.java#signup` |

## employer
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/employer/correction-requests` | EMPLOYER | `-` | `EmployerCorrectionRequestsResponse` | `-` | `-` | `backend/employer/api/EmployerCorrectionRequestController.java#getCorrectionRequests` |
| `GET` | `/api/employer/correction-requests/{requestId}` | EMPLOYER | `-` | `EmployerCorrectionRequestDetailResponse` | `-` | `-` | `backend/employer/api/EmployerCorrectionRequestController.java#getCorrectionRequest` |
| `POST` | `/api/employer/correction-requests/{requestId}/approve` | EMPLOYER | `EmployerApproveCorrectionRequest` | `EmployerCorrectionRequestDetailResponse` | `-` | `-` | `backend/employer/api/EmployerCorrectionRequestController.java#approveCorrectionRequest` |
| `POST` | `/api/employer/correction-requests/{requestId}/reject` | EMPLOYER | `EmployerRejectCorrectionRequest` | `EmployerCorrectionRequestDetailResponse` | `-` | `-` | `backend/employer/api/EmployerCorrectionRequestController.java#rejectCorrectionRequest` |
| `GET` | `/api/employer/dashboard/attendance-board` | EMPLOYER | `-` | `EmployerAttendanceBoardResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerReadModelController.java#getAttendanceBoard` |
| `GET` | `/api/employer/dashboard/summary` | EMPLOYER | `-` | `EmployerDashboardSummaryResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerReadModelController.java#getDashboardSummary` |
| `GET` | `/api/employer/issues` | EMPLOYER | `-` | `EmployerIssuesResponse` | `-` | `-` | `backend/employer/api/EmployerIssueReadModelController.java#getIssues` |
| `GET` | `/api/employer/issues/review-records/{workProofId}` | EMPLOYER | `-` | `EmployerReviewRequiredRecordDetailResponse` | `-` | `-` | `backend/employer/api/EmployerIssueReadModelController.java#getReviewRecord` |
| `POST` | `/api/employer/issues/review-records/{workProofId}/confirm` | EMPLOYER | `-` | `EmployerReviewRecordConfirmResponse` | `-` | `-` | `backend/employer/api/EmployerIssueCommandController.java#confirmReviewRecord` |
| `GET` | `/api/employer/profile` | EMPLOYER | `-` | `EmployerProfileResponse` | `-` | `-` | `backend/employer/api/EmployerProfileController.java#getProfile` |
| `GET` | `/api/employer/worker-registration-codes` | EMPLOYER | `-` | `EmployerWorkerRegistrationCodesResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerRegistrationCodeController.java#getCodes` |
| `POST` | `/api/employer/worker-registration-codes` | EMPLOYER | `-` | `EmployerWorkerRegistrationCodeResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerRegistrationCodeController.java#issue` |
| `POST` | `/api/employer/worker-registration-codes/{codeId}/revoke` | EMPLOYER | `-` | `EmployerWorkerRegistrationCodeResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerRegistrationCodeController.java#revoke` |
| `GET` | `/api/employer/workers` | EMPLOYER | `-` | `EmployerWorkersResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerReadModelController.java#getWorkers` |
| `GET` | `/api/employer/workers/{workerId}` | EMPLOYER | `-` | `EmployerWorkerDetailResponse` | `-` | `-` | `backend/employer/api/EmployerWorkerReadModelController.java#getWorkerDetail` |
| `GET` | `/api/employer/workplace-settings` | EMPLOYER | `-` | `EmployerWorkplaceSettingsResponse` | `-` | `-` | `backend/employer/api/EmployerWorkplaceSettingsController.java#getSettings` |
| `PUT` | `/api/employer/workplace-settings` | EMPLOYER | `UpdateEmployerWorkplaceSettingsRequest` | `EmployerWorkplaceSettingsResponse` | `-` | `-` | `backend/employer/api/EmployerWorkplaceSettingsController.java#updateSettings` |

## jobs
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/jobs/ping` | USER or ADMIN | `-` | `Map<String, String>` | `-` | `-` | `backend/jobs/api/JobsController.java#ping` |

## remittance
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/remittance/recipients` | USER or ADMIN | `-` | `RecipientListResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#getRecipients` |
| `POST` | `/api/remittance/recipients` | USER or ADMIN | `UpsertRecipientRequest` | `RecipientItemResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#createRecipient` |
| `POST` | `/api/remittance/recipients/search` | USER or ADMIN | `RecipientSearchRequest` | `RecipientSearchListResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#searchRecipients` |
| `PUT` | `/api/remittance/recipients/{recipientId}` | USER or ADMIN | `UpsertRecipientRequest` | `RecipientItemResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#updateRecipient` |
| `GET` | `/api/remittance/transfers` | USER or ADMIN | `-` | `TransferListResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#getTransfers` |
| `POST` | `/api/remittance/transfers` | USER or ADMIN | `CreateTransferRequest` | `CreateTransferResponse` | `optional` | `-` | `backend/remittance/api/RemittanceController.java#createTransfer` |
| `POST` | `/api/remittance/transfers/precheck` | USER or ADMIN | `TransferPrecheckRequest` | `TransferPrecheckResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#precheck` |
| `GET` | `/api/remittance/transfers/{transferId}` | USER or ADMIN | `-` | `TransferDetailResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#getTransfer` |
| `GET` | `/api/remittance/wallets/me` | USER or ADMIN | `-` | `WalletResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#getWallet` |
| `POST` | `/api/remittance/wallets/me` | USER or ADMIN | `-` | `WalletResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#createWallet` |
| `GET` | `/api/remittance/wallets/me/balance` | USER or ADMIN | `-` | `WalletBalanceResponse` | `-` | `-` | `backend/remittance/api/RemittanceController.java#getWalletBalance` |

## safepay
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/safepay/ping` | USER or ADMIN | `-` | `Map<String, String>` | `-` | `-` | `backend/safepay/api/SafePayController.java#ping` |

## vault
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/vault/deposits` | USER or ADMIN | `CreateVaultTransactionRequest` | `CreateVaultTransactionResponse` | `optional` | `-` | `backend/vault/api/VaultController.java#createDeposit` |
| `GET` | `/api/vault/summary` | USER or ADMIN | `-` | `VaultSummaryResponse` | `-` | `-` | `backend/vault/api/VaultController.java#getSummary` |
| `GET` | `/api/vault/transactions` | USER or ADMIN | `-` | `VaultTransactionListResponse` | `-` | `-` | `backend/vault/api/VaultController.java#getTransactions` |
| `GET` | `/api/vault/transactions/{vaultTransactionId}` | USER or ADMIN | `-` | `VaultTransactionDetailResponse` | `-` | `-` | `backend/vault/api/VaultController.java#getTransaction` |
| `POST` | `/api/vault/withdrawals` | USER or ADMIN | `CreateVaultTransactionRequest` | `CreateVaultTransactionResponse` | `optional` | `-` | `backend/vault/api/VaultController.java#createWithdrawal` |

## wage
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/wage/deposits` | USER or ADMIN | `CreateWageDepositRequest` | `WageDepositResponse` | `-` | `-` | `backend/wage/api/WageController.java#createDeposit` |
| `GET` | `/api/wage/estimate` | USER or ADMIN | `-` | `WageEstimateResponse` | `-` | `-` | `backend/wage/api/WageController.java#getEstimate` |
| `GET` | `/api/wage/monthly-summary` | USER or ADMIN | `-` | `WageMonthlySummaryResponse` | `-` | `-` | `backend/wage/api/WageController.java#getMonthlySummary` |
| `GET` | `/api/wage/summary` | USER or ADMIN | `-` | `WageSummaryResponse` | `-` | `-` | `backend/wage/api/WageController.java#getSummary` |
| `POST` | `/api/wage/verifications` | USER or ADMIN | `CreateWageVerificationRequest` | `WageVerificationCreatedResponse` | `-` | `-` | `backend/wage/api/WageController.java#createVerification` |
| `GET` | `/api/wage/verifications/{verificationId}` | USER or ADMIN | `-` | `WageVerificationDetailResponse` | `-` | `-` | `backend/wage/api/WageController.java#getVerification` |

## workproof
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/workproof` | USER or ADMIN | `-` | `List<WorkProofResponse>` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getWorkProofs` |
| `POST` | `/api/workproof` | USER or ADMIN | `CreateWorkProofRequest` | `WorkProofResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#create` |
| `POST` | `/api/workproof/contracts` | USER or ADMIN | `CreateContractRequest` | `CurrentContractResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#createContract` |
| `GET` | `/api/workproof/contracts/current` | USER or ADMIN | `-` | `CurrentContractResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getCurrentContract` |
| `POST` | `/api/workproof/documents` | USER or ADMIN | `CreateWorkproofDocumentRequest` | `DocumentGenerationAcceptedResponse` | `required` | `-` | `backend/documents/api/WorkproofDocumentsController.java#create` |
| `GET` | `/api/workproof/documents/preview` | USER or ADMIN | `-` | `WorkproofDocumentPreviewResponse` | `-` | `-` | `backend/documents/api/WorkproofDocumentsController.java#preview` |
| `GET` | `/api/workproof/monthly-summary` | USER or ADMIN | `-` | `WorkProofMonthlySummaryContractResponse` | `-` | `month,workplaceId` | `backend/workproof/api/WorkProofController.java#getLane1MonthlySummary` |
| `GET` | `/api/workproof/monthly-summary` | USER or ADMIN | `-` | `WorkProofMonthlySummaryResponse` | `-` | `yearMonth` | `backend/workproof/api/WorkProofController.java#getMonthlySummary` |
| `GET` | `/api/workproof/records` | USER or ADMIN | `-` | `WorkProofRecordListResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getRecords` |
| `POST` | `/api/workproof/records/check-in` | USER or ADMIN | `CheckInWorkProofRequest` | `WorkProofRecordResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#checkIn` |
| `POST` | `/api/workproof/records/check-out` | USER or ADMIN | `CheckOutWorkProofRequest` | `WorkProofRecordResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#checkOut` |
| `GET` | `/api/workproof/records/{recordId}` | USER or ADMIN | `-` | `WorkProofRecordResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getRecord` |
| `GET` | `/api/workproof/workplaces` | USER or ADMIN | `-` | `WorkplaceListResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getWorkplaces` |
| `POST` | `/api/workproof/workplaces` | USER or ADMIN | `CreateWorkplaceRequest` | `WorkplaceResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#createWorkplace` |
| `GET` | `/api/workproof/{workProofId}` | USER or ADMIN | `-` | `WorkProofResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#getWorkProof` |
| `PATCH` | `/api/workproof/{workProofId}` | USER or ADMIN | `UpdateWorkProofRequest` | `WorkProofResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#update` |
| `POST` | `/api/workproof/{workProofId}/correction-requests` | USER or ADMIN | `CreateWorkProofCorrectionRequest` | `WorkProofCorrectionRequestResponse` | `-` | `-` | `backend/workproof/api/WorkProofController.java#createCorrectionRequest` |

## health
| Method | Path | Access | Req Body DTO | Response DTO | Idempotency-Key | Mapping 조건 | Controller#method |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/health` | Public | `-` | `Map<String, String>` | `-` | `-` | `backend/shared/api/HealthController.java#health` |

## 참고
- 이 문서는 **구현 카탈로그**다. 필드 단위 계약/예시 payload가 필요하면 후속 v2에서 DTO별 상세 표를 추가한다.
- PRD 중심 설계 초안은 `docs/DonDone_P0_API_Contract_v0.md`를 참고한다.
