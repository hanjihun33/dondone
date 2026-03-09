# DonDone PRD P0 API 계약 초안 v0

## 문서 정보
- 작성일: 2026-03-09
- 기준 문서: `docs/DonDone_PRD_v1.5.md`
- 상태: Draft v0
- 목적: PRD P0 범위를 기준으로, 모바일/프론트와 백엔드가 같은 계약을 보고 병렬 구현을 시작할 수 있게 한다.

## 요구사항 확인

| 항목 | 이번 문서 기준 |
| --- | --- |
| expected behavior | PRD P0 기준 `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault` API 초안을 정의한다. |
| exact scope | `home`, `copilot`, `time travel`은 이번 문서에서 제외한다. `auth`는 기존 구현을 반영하고, 나머지는 신규 계약 초안으로 정의한다. |
| contract changes | 공통 응답 envelope은 `ApiResponse<T>` 형식의 `data`를 유지한다. 중복 요청 위험이 있는 API는 `Idempotency-Key` 규칙을 포함한다. |
| security impact | `/api/auth/login`, `/api/auth/signup`, `/health`, Swagger 외 나머지 API는 JWT 보호 대상으로 본다. 타인 리소스는 `404`로 숨긴다. |
| non-functional impact | 문서 생성과 송금은 비동기 처리로 설계하고 `202 Accepted`를 사용한다. P0는 데모/테스트넷 기준이며 실거래/실정산을 다루지 않는다. |

## 범위

### 포함
- Auth
- WorkProof
- Advance
- Wage Shield
- Documents
- Instant Claim
- Remittance
- SafePay
- Vault

### 제외
- Home API
- Copilot API
- Demo Time Travel / `X-Demo-AsOf`
- P1 범위 전부

## 계약 안정도 구분

### 1. 확정
- 문서 구조는 `공통 규칙 -> 기능별 상세` 순서를 유지한다.
- 공통 응답 envelope은 `success`, `code`, `message`, `data`, `timestamp`를 사용한다.
- 공통 에러 형식도 같은 envelope을 사용한다.
- `auth`는 현재 백엔드 구현을 기준으로 문서에 반영한다.
- P0 도메인은 `auth -> workproof -> advance -> wage -> documents -> claim -> remittance -> safepay -> vault` 순서로 정리한다.
- 중복 요청 위험이 큰 생성/신청/전송/문서 API는 `Idempotency-Key`를 사용한다.
- 문서 생성과 송금은 비동기 처리 전제를 둔다.
- 제품 정책은 테스트넷/데모 기준이며, 실거래/실정산을 의미하지 않는다.

### 2. v0 가정
- `DAILY`, `MONTHLY` 기준 시간의 기본값 책임은 아직 열어둔다.
- WorkProof 동일 날짜 다중 근무는 후속 확장 전까지 허용하지 않는 방향으로 시작한다.
- Wage 계산 경계는 연장 `일별 480분 초과`, 야간 `22:00~06:00` 겹침 분으로 둔다.
- SafePay 고액 추가 확인 기준은 절대/상대값 혼합 정책으로 두되, 세부 임계값은 후속 정책 문서에서 고정한다.
- Vault는 순수 시뮬레이션이며 실제 온체인 예치/수익 실현을 하지 않는다.
- Claim 요약 문장은 facts 기반 템플릿/생성 결과를 반환하는 것으로 둔다.
- WorkProof 첨부는 `attachmentId` 참조 방식으로 설계하고, 업로드는 같은 도메인에서 선행 처리한다.

### 3. 후속 확장
- Home 조합 API
- Copilot 전용 API
- Demo Time Travel API
- P1 범위 전부
- 다중 근무지 동시 활성 계약
- WorkProof 자동 지오펜스
- Wage 명세서 자동 파싱
- Remittance 정기 송금
- Vault 실제 메인넷 연동

## 공통 규칙

### Base Path
- `auth`: `/api/auth`
- `workproof`: `/api/workproof`
- `advance`: `/api/advance`
- `wage`: `/api/wage`
- `documents`: `/api/documents`
- `claim`: `/api/claim`
- `remittance`: `/api/remittance`
- `safepay`: `/api/safepay`
- `vault`: `/api/vault`

### 인증
- 기본값: `Authorization: Bearer {accessToken}` 필요
- 예외: `POST /api/auth/signup`, `POST /api/auth/login`, `/health`, Swagger

### 공통 헤더

| 헤더 | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | 보호 API만 필수 | JWT access token |
| `Idempotency-Key` | 일부 POST 필수 | 중복 요청 방지 키 |
| `Accept-Language` | 선택 | 다국어 안내 문구/정책 문구 출력 언어 |

### 공통 응답 Envelope

```json
{
  "success": true,
  "code": "OK",
  "message": "Request succeeded",
  "data": {},
  "timestamp": "2026-03-09T05:14:22.123Z"
}
```

### 공통 에러 Envelope

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Month format is invalid",
  "data": null,
  "timestamp": "2026-03-09T05:14:22.123Z"
}
```

### 공통 포맷
- datetime: ISO-8601 offset datetime
  - 예: `2026-03-09T09:02:10+09:00`
- date: `yyyy-MM-dd`
- month: `yyyy-MM`
- 금액: KRW 정수 `long`
- 시간: 분 단위 정수 `int`
- 위도/경도: decimal

### 공통 비동기 규칙
- 문서 생성과 송금 요청은 `202 Accepted`를 기본으로 사용한다.
- 비동기 생성 응답에는 아래 항목을 포함한다.
  - `requestId`
  - `status`
  - `pollUrl`
- 상태 enum 초안
  - `QUEUED`
  - `RUNNING`
  - `DONE`
  - `FAILED`

### 공통 에러 코드

| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | 필수값 누락, 포맷 오류, enum 오류 |
| `401` | `UNAUTHORIZED` | 토큰 누락 또는 만료 |
| `403` | `FORBIDDEN` | 정책상 허용되지 않는 동작 |
| `404` | `RESOURCE_NOT_FOUND` | 리소스를 찾을 수 없음 |
| `409` | `CONFLICT` | 현재 상태와 충돌 |
| `429` | `RATE_LIMITED` | 쿨다운/과도 요청 제한 |
| `500` | `INTERNAL_ERROR` | 예상하지 못한 오류 |

## 1. Auth

> 현재 백엔드 구현을 그대로 반영한다.

### Endpoint 목록

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 | 불필요 |
| `POST` | `/api/auth/login` | 로그인/JWT 발급 | 불필요 |
| `GET` | `/api/auth/me` | 현재 사용자 조회 | 필요 |

### `POST /api/auth/signup`

Request:
- `email`: string
- `password`: string, 최소 8자
- `name`: string, 최대 100자

Response `201 Created`:
- `userId`
- `email`
- `name`
- `role`

주요 에러:
- `409 EMAIL_ALREADY_EXISTS`
- `400 VALIDATION_ERROR`

### `POST /api/auth/login`

Request:
- `email`
- `password`

Response `200 OK`:
- `accessToken`
- `tokenType`
- `expiresIn`
- `userId`
- `email`
- `name`

주요 에러:
- `401 INVALID_CREDENTIALS`
- `400 VALIDATION_ERROR`

### `GET /api/auth/me`

Response `200 OK`:
- `userId`
- `email`
- `name`
- `role`

## 2. WorkProof

> PRD 7B 기준. W1, W2, W3, W4, W5, W6을 API 관점에서 풀어 쓴다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/workproof/workplaces` | 근무지 등록 |
| `GET` | `/api/workproof/workplaces` | 내 근무지 목록 조회 |
| `POST` | `/api/workproof/contracts` | 급여 계약 등록 |
| `GET` | `/api/workproof/contracts/current?workplaceId={id}` | 현재 활성 계약 조회 |
| `POST` | `/api/workproof/attachments` | 수정 첨부 업로드 |
| `POST` | `/api/workproof/records/check-in` | 출근 기록 생성 |
| `POST` | `/api/workproof/records/check-out` | 퇴근 기록 확정 |
| `POST` | `/api/workproof/records/{recordId}/modifications` | 근무 기록 수정 |
| `GET` | `/api/workproof/records?month=YYYY-MM&workplaceId={id}` | 월별 기록 목록 |
| `GET` | `/api/workproof/records/{recordId}` | 기록 상세 |
| `GET` | `/api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}` | 월간 요약/반영 상태 |

### 2.1 `POST /api/workproof/workplaces`

Request:
- `name`: 1~100자
- `address`: 1~255자
- `mapLabel`: 선택
- `latitude`: 필수
- `longitude`: 필수

Response:
- `workplaceId`
- `name`
- `address`
- `mapLabel`
- `latitude`
- `longitude`
- `createdAt`

주요 에러:
- `400 VALIDATION_ERROR`

### 2.2 `GET /api/workproof/workplaces`

Response:
- `workplaces[]`
  - `workplaceId`
  - `name`
  - `address`
  - `mapLabel`
  - `latitude`
  - `longitude`
  - `hasActiveContract`

### 2.3 `POST /api/workproof/contracts`

Request:
- `workplaceId`
- `payUnit`: `HOURLY`, `DAILY`, `MONTHLY`
- `basePayAmount`
- `dailyWorkMinutes`: `DAILY`일 때 필요
- `monthlyWorkMinutes`: `MONTHLY`일 때 필요
- `effectiveFrom`: 선택, 미지정 시 오늘

Response:
- `contractId`
- `workplaceId`
- `payUnit`
- `basePayAmount`
- `dailyWorkMinutes`
- `monthlyWorkMinutes`
- `normalizedHourlyWage`
- `effectiveFrom`
- `isActive`

주요 에러:
- `404 WORKPLACE_NOT_FOUND`
- `409 ACTIVE_CONTRACT_EXISTS`
- `400 VALIDATION_ERROR`

### 2.4 `GET /api/workproof/contracts/current?workplaceId={id}`

Response:
- `contractId`
- `workplaceId`
- `payUnit`
- `basePayAmount`
- `dailyWorkMinutes`
- `monthlyWorkMinutes`
- `normalizedHourlyWage`
- `effectiveFrom`
- `effectiveTo`
- `isActive`

주요 에러:
- `404 ACTIVE_CONTRACT_NOT_FOUND`

### 2.5 `POST /api/workproof/attachments`

Request:
- `multipart/form-data`
- `file`
- `kind`: `PHOTO`, `MEMO_IMAGE`, `SCHEDULE_IMAGE`, `OTHER`

Response:
- `attachmentId`
- `fileName`
- `contentType`
- `size`
- `uploadedAt`

주요 에러:
- `400 UNSUPPORTED_FILE_TYPE`
- `413 FILE_TOO_LARGE`

### 2.6 `POST /api/workproof/records/check-in`

Request:
- `workplaceId`
- `deviceAt`
- `latitude`
- `longitude`
- `locationLabel`

Response:
- `recordId`
- `workDate`
- `status`: `CHECKED_IN`
- `workplace`
- `contract`
- `checkIn`
- `checkOut`: `null`
- `reflectionStatus`: `PENDING`

주요 에러:
- `404 WORKPLACE_NOT_FOUND`
- `409 ACTIVE_CONTRACT_REQUIRED`
- `409 ACTIVE_WORKPROOF_EXISTS`
- `409 WORK_DATE_ALREADY_EXISTS`

### 2.7 `POST /api/workproof/records/check-out`

Request:
- `deviceAt`
- `latitude`
- `longitude`
- `locationLabel`

Response:
- `recordId`
- `workDate`
- `status`: `CHECKED_OUT`
- `checkIn`
- `checkOut`
- `workedMinutes`
- `reflectionStatus`: `REFLECTED` 또는 `NEEDS_REVIEW`

주요 에러:
- `404 ACTIVE_WORKPROOF_NOT_FOUND`
- `409 CHECK_OUT_BEFORE_CHECK_IN`

### 2.8 `POST /api/workproof/records/{recordId}/modifications`

Request:
- `checkInDeviceAt`: 선택
- `checkOutDeviceAt`: 선택
- `reasonCode`: `LATE_TAP`, `OVERTIME`, `BREAK_CHANGED`, `MISSING_RECORD`, `OTHER`
- `reasonMemo`: 선택
- `attachmentIds`: 선택

Response:
- `recordId`
- `modificationId`
- `status`
- `modified`: `true`
- `modifiedAt`
- `reasonCode`
- `reasonMemo`
- `attachmentCount`
- `auditTrail`
  - `before`
  - `after`
  - `at`

주요 에러:
- `404 WORKPROOF_NOT_FOUND`
- `400 MODIFICATION_REASON_REQUIRED`
- `400 INVALID_MODIFICATION_TIME`

예시:

```json
{
  "checkOutDeviceAt": "2026-03-18T19:10:00+09:00",
  "reasonCode": "OVERTIME",
  "reasonMemo": "잔업 1시간 추가",
  "attachmentIds": ["att_01", "att_02"]
}
```

### 2.9 `GET /api/workproof/records?month=YYYY-MM&workplaceId={id}`

Response:
- `month`
- `workplaceId`
- `records[]`
  - `recordId`
  - `workDate`
  - `status`
  - `checkInDeviceAt`
  - `checkOutDeviceAt`
  - `workedMinutes`
  - `modified`
  - `reflectionStatus`

### 2.10 `GET /api/workproof/records/{recordId}`

Response:
- `recordId`
- `workDate`
- `status`
- `workplace`
- `contract`
- `checkIn`
- `checkOut`
- `workedMinutes`
- `modified`
- `modifications[]`
- `attachments[]`

### 2.11 `GET /api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}`

Response:
- `month`
- `workplaceId`
- `workDayCount`
- `totalWorkMinutes`
- `overtimeMinutes`
- `nightMinutes`
- `modifiedRecordCount`
- `reflection`
  - `reflectedRecordCount`
  - `needsReviewRecordCount`
  - `excludedRecordCount`
- `financeReadiness`
  - `advanceEligibleWorkDays`
  - `wageUsableWorkDays`

## 3. Advance

> PRD 7C 기준. 데모 시뮬레이션이며 실제 대출/가불 정책이 아니다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/advance/eligibility?workplaceId={id}` | 현재 가능 금액/근거 조회 |
| `POST` | `/api/advance/requests` | 미리받기 신청 |
| `GET` | `/api/advance/requests?month=YYYY-MM` | 신청 이력 조회 |
| `GET` | `/api/advance/requests/{requestId}` | 신청 상세 조회 |

### 3.1 `GET /api/advance/eligibility?workplaceId={id}`

Response:
- `workplaceId`
- `availableAmount`
- `maxCap`
- `policyRate`
- `reflectedWorkDays`
- `reflectedWorkMinutes`
- `needsReviewRecordCount`
- `nextTierRemainingMinutes`
- `estimatedFee`
- `estimatedRepaymentDate`
- `disclaimer`

### 3.2 `POST /api/advance/requests`

Headers:
- `Idempotency-Key`: 필수

Request:
- `workplaceId`
- `requestedAmount`
- `requestedAt`

Response `201 Created`:
- `requestId`
- `status`: `SUBMITTED`, `APPROVED`, `REJECTED`, `NEEDS_REVIEW`
- `approvedAmount`
- `feeAmount`
- `repaymentDueDate`
- `eligibilitySnapshot`

주요 에러:
- `409 ADVANCE_DUPLICATE_REQUEST`
- `409 ADVANCE_NOT_ELIGIBLE`
- `400 REQUEST_AMOUNT_EXCEEDS_LIMIT`

### 3.3 `GET /api/advance/requests?month=YYYY-MM`

Response:
- `month`
- `requests[]`
  - `requestId`
  - `workplaceId`
  - `requestedAmount`
  - `approvedAmount`
  - `status`
  - `repaymentDueDate`
  - `requestedAt`

### 3.4 `GET /api/advance/requests/{requestId}`

Response:
- `requestId`
- `workplaceId`
- `requestedAmount`
- `approvedAmount`
- `feeAmount`
- `status`
- `repaymentDueDate`
- `eligibilitySnapshot`
- `createdAt`

## 4. Wage Shield

> PRD 7D 기준. 정답 계산기가 아니라 이상 탐지 도구다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}` | 월간 근무/집계 요약 |
| `GET` | `/api/wage/estimate?month=YYYY-MM&workplaceId={id}` | 참고용 예상 급여 |
| `POST` | `/api/wage/verifications` | 실제 입금액 입력 + 차액 판정 |
| `GET` | `/api/wage/verifications/{verificationId}` | 차액 감지 상세 |

### 4.1 `GET /api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}`

Response:
- `month`
- `workplaceId`
- `contractId`
- `payUnit`
- `normalizedHourlyWage`
- `workDayCount`
- `verifiedWorkMinutes`
- `overtimeMinutes`
- `nightMinutes`
- `modifiedRecordCount`
- `includedRecordIds`
- `excludedPendingRecordCount`

### 4.2 `GET /api/wage/estimate?month=YYYY-MM&workplaceId={id}`

Response:
- `month`
- `workplaceId`
- `contract`
- `summary`
- `estimate`
  - `baseEstimate`
  - `overtimePremium`
  - `nightPremium`
  - `estimatedTotal`
- `disclaimer`
- `ruleVersion`

### 4.3 `POST /api/wage/verifications`

Request:
- `month`
- `workplaceId`
- `actualDepositAmount`
- `deductionsKnown`: boolean
- `memo`: 선택

Response `201 Created`:
- `verificationId`
- `status`: `MATCHED`, `REVIEW_REQUIRED`
- `estimatedTotal`
- `actualDepositAmount`
- `differenceAmount`
- `differenceRate`
- `threshold`
  - `absoluteWon`
  - `relativePercent`
  - `deductionRelaxed`
- `possibleCauses[]`
  - `code`
  - `title`
  - `detail`
- `evidence`
  - `overtimeMinutes`
  - `nightMinutes`
  - `modifiedRecordCount`
  - `recordIds`

주요 에러:
- `404 ACTIVE_CONTRACT_REQUIRED`
- `400 ACTUAL_DEPOSIT_REQUIRED`

예시:

```json
{
  "month": "2026-03",
  "workplaceId": 1,
  "actualDepositAmount": 1740000,
  "deductionsKnown": false
}
```

### 4.4 `GET /api/wage/verifications/{verificationId}`

Response:
- `verificationId`
- `month`
- `workplaceId`
- `status`
- `estimated`
- `actual`
- `difference`
- `threshold`
- `possibleCauses[]`
- `evidence`
- `relatedActions`
  - `proofPackReady`
  - `claimKitReady`
  - `instantClaimAvailable`

## 5. Documents

> PRD 7H 기준. 문서 생성은 비동기 처리로 시작한다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/documents` | 문서 목록 조회 |
| `GET` | `/api/documents/{documentId}` | 문서 상세 조회 |
| `GET` | `/api/documents/{documentId}/download-url` | 다운로드 URL 발급 |
| `POST` | `/api/documents/proof-packs` | Proof Pack 생성 요청 |
| `POST` | `/api/documents/claim-kits` | Claim Kit 생성 요청 |
| `POST` | `/api/documents/transfer-receipts` | 송금 영수증 생성 요청 |

### 공통 문서 타입
- `PROOF_PACK`
- `CLAIM_KIT`
- `TRANSFER_RECEIPT`

### 5.1 `GET /api/documents`

Query:
- `type`: 선택
- `status`: 선택
- `month`: 선택

Response:
- `documents[]`
  - `documentId`
  - `type`
  - `status`: `QUEUED`, `RUNNING`, `READY`, `FAILED`
  - `title`
  - `relatedEntityType`
  - `relatedEntityId`
  - `updatedAt`

### 5.2 `GET /api/documents/{documentId}`

Response:
- `documentId`
- `type`
- `status`
- `title`
- `summary`
- `relatedLinks[]`
- `createdAt`
- `updatedAt`
- `downloadable`

### 5.3 `GET /api/documents/{documentId}/download-url`

Response:
- `documentId`
- `downloadUrl`
- `expiresAt`

### 5.4 `POST /api/documents/proof-packs`

Headers:
- `Idempotency-Key`: 필수

Request:
- `month`
- `workplaceId`
- `wageVerificationId`

Response `202 Accepted`:
- `requestId`
- `documentType`: `PROOF_PACK`
- `status`
- `pollUrl`

주요 에러:
- `409 DOCUMENT_DUPLICATE_REQUEST`
- `404 WAGE_VERIFICATION_NOT_FOUND`

### 5.5 `POST /api/documents/claim-kits`

Headers:
- `Idempotency-Key`: 필수

Request:
- `month`
- `workplaceId`
- `wageVerificationId`
- `includeAttachments`: boolean
- `format`: `PDF`, `ZIP`

Response `202 Accepted`:
- `requestId`
- `documentType`: `CLAIM_KIT`
- `status`
- `pollUrl`

### 5.6 `POST /api/documents/transfer-receipts`

Headers:
- `Idempotency-Key`: 필수

Request:
- `transferId`

Response `202 Accepted`:
- `requestId`
- `documentType`: `TRANSFER_RECEIPT`
- `status`
- `pollUrl`

## 6. Instant Claim

> PRD 7H D3 기준. 자동 제출이 아니라 반자동 준비 흐름이다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/claim/routes?locale=ko-KR` | 신고/상담 경로 안내 |
| `POST` | `/api/claim/preparations` | Instant Claim 준비 데이터 생성 |
| `GET` | `/api/claim/preparations/{preparationId}` | 준비 결과 조회 |

### 6.1 `GET /api/claim/routes?locale=ko-KR`

Response:
- `locale`
- `routes[]`
  - `channel`: `ONLINE`, `PHONE`, `VISIT`
  - `title`
  - `description`
  - `contact`
  - `link`

### 6.2 `POST /api/claim/preparations`

Headers:
- `Idempotency-Key`: 권장

Request:
- `wageVerificationId`
- `claimKitDocumentId`: 선택
- `locale`
- `tone`: `DEFAULT`, `POLITE`, `SHORT`

Response `201 Created`:
- `preparationId`
- `status`: `READY`
- `summaryText`
- `checklist[]`
- `suggestedRoutes[]`
- `relatedDocuments[]`

주요 에러:
- `404 WAGE_VERIFICATION_NOT_FOUND`
- `404 CLAIM_KIT_NOT_FOUND`

### 6.3 `GET /api/claim/preparations/{preparationId}`

Response:
- `preparationId`
- `status`
- `summaryText`
- `checklist[]`
- `suggestedRoutes[]`
- `relatedDocuments[]`
- `createdAt`

## 7. Remittance

> PRD 7E 기준. 테스트넷 전송이며, SafePay 정책과 함께 움직인다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/remittance/recipients` | 허용 목록 조회 |
| `POST` | `/api/remittance/recipients` | 허용 목록 수신자 등록 |
| `GET` | `/api/remittance/transfers` | 송금 목록 조회 |
| `POST` | `/api/remittance/transfers` | 송금 요청 |
| `GET` | `/api/remittance/transfers/{transferId}` | 송금 상세/상태 조회 |

### 7.1 `GET /api/remittance/recipients`

Response:
- `recipients[]`
  - `recipientId`
  - `name`
  - `alias`
  - `relationship`
  - `walletAddress`
  - `photoUrl`
  - `isFavorite`
  - `cooldownUntil`

### 7.2 `POST /api/remittance/recipients`

Request:
- `name`
- `alias`
- `relationship`
- `walletAddress`
- `photoUrl`: 선택

Response:
- `recipientId`
- `name`
- `alias`
- `relationship`
- `walletAddress`
- `cooldownUntil`

주요 에러:
- `409 RECIPIENT_ALREADY_EXISTS`
- `400 INVALID_WALLET_ADDRESS`

### 7.3 `GET /api/remittance/transfers`

Query:
- `month`
- `status`

Response:
- `transfers[]`
  - `transferId`
  - `recipientId`
  - `amount`
  - `status`: `SUBMITTED`, `CONFIRMED`, `FAILED`, `BLOCKED`
  - `txHash`
  - `createdAt`

### 7.4 `POST /api/remittance/transfers`

Headers:
- `Idempotency-Key`: 필수

Request:
- `recipientId`
- `tokenSymbol`
- `amount`
- `memo`: 선택

Response `202 Accepted`:
- `transferId`
- `status`
- `safepayDecision`
- `pollUrl`

주요 에러:
- `409 DUPLICATE_TRANSFER_REQUEST`
- `403 SAFEPAY_BLOCKED`
- `404 RECIPIENT_NOT_FOUND`

예시:

```json
{
  "recipientId": "rcp_01",
  "tokenSymbol": "USDC",
  "amount": 120,
  "memo": "Family support"
}
```

### 7.5 `GET /api/remittance/transfers/{transferId}`

Response:
- `transferId`
- `recipient`
- `amount`
- `status`
- `txHash`
- `failureReason`
- `safepay`
- `receiptDocumentId`
- `createdAt`
- `updatedAt`

## 8. SafePay

> PRD 7F 기준. Remittance 요청 전/중에 적용되는 보호 정책이다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/safepay/transfer-checks` | 송금 전 정책 검사 |

### 8.1 `POST /api/safepay/transfer-checks`

Request:
- `recipientId`
- `amount`
- `tokenSymbol`

Response:
- `decision`: `ALLOW`, `WARN`, `BLOCK`
- `reasonCodes[]`
- `userMessage`
- `cooldownUntil`
- `requiresAdditionalConfirm`

주요 이유 코드 예시:
- `RECIPIENT_IN_COOLDOWN`
- `AMOUNT_TOO_HIGH`
- `RECIPIENT_NOT_ALLOWLISTED`
- `DUPLICATE_TRANSFER_SUSPECTED`

## 9. Vault

> PRD 7G 기준. 데모 시뮬레이션이며 수익 보장을 의미하지 않는다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/vault/summary` | 보관/이자 요약 조회 |
| `POST` | `/api/vault/allocations` | 보관 금액 설정 |
| `POST` | `/api/vault/releases` | 보관 금액 해제 |

### 9.1 `GET /api/vault/summary`

Response:
- `storedAmount`
- `availableToStoreAmount`
- `availableToTransferAmount`
- `interestPreview`
  - `daily`
  - `monthly`
  - `apr`
- `disclaimer`

### 9.2 `POST /api/vault/allocations`

Request:
- `amount`

Response:
- `allocationId`
- `storedAmount`
- `availableToStoreAmount`
- `interestPreview`
- `simulatedAt`

주요 에러:
- `400 INVALID_ALLOCATION_AMOUNT`
- `409 INSUFFICIENT_AVAILABLE_BALANCE`

### 9.3 `POST /api/vault/releases`

Request:
- `amount`
- `target`: `SPENDABLE`, `TRANSFERABLE`

Response:
- `releaseId`
- `storedAmount`
- `releasedAmount`
- `availableToTransferAmount`
- `simulatedAt`

## 공통 구현 메모
- `auth`는 현재 구현을 기준으로 유지한다.
- `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`는 feature-first 구조를 유지한다.
- 문서 생성과 송금은 비동기 job 모델을 따른다.
- 외부 연동은 `adapter` 인터페이스 뒤에 둔다.
- PRD 전체 P0 기준 문서지만, `v0 가정` 영역은 후속 조정 가능성을 열어둔다.

## 다음 보강 후보
- `home` 조합 응답 초안
- `copilot` facts API 초안
- `time travel` / `X-Demo-AsOf` 규칙 초안
- domain별 DTO field-level validation 상세화
- domain별 예시 JSON 확장
