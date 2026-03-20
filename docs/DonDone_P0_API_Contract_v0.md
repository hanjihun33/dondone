# DonDone PRD P0 API 계약 초안 v0

## 문서 정보
- 작성일: 2026-03-09
- 기준 문서: `docs/DonDone_PRD_v1.5.md`
- 상태: Draft v0
- 목적: PRD P0 전체를 같은 문서 안에서 읽을 수 있게 정리하고, 이미 비교적 안정적인 계약과 아직 최소 스케치 수준인 계약의 차이를 함께 드러낸다.

## 표기 메모
- `배경`: 왜 이 모양의 API로 두었는지 공유용으로 짧게 설명한다.
- `v0 메모`: 현재 초안 기준 기본값, 임시 정책, 구현 중 조정 가능 지점을 적는다.
- `확장 메모`: 후속 분리, 별도 정책 문서화, P1 확장 가능성을 가볍게 남긴다.
- 요청/응답은 공통 객체 참조보다 self-contained 표기를 우선하고, 중첩 객체는 각 API 안에서 최소 필드를 읽을 수 있게 적는다.

## 요구사항 확인

| 항목 | 이번 문서 기준 |
| --- | --- |
| expected behavior | PRD P0 기준 `home`, `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo` API 초안을 같은 문서에서 읽을 수 있게 한다. 덜 굳은 항목은 최소 계약 스케치로 포함한다. |
| exact scope | `home`, `auth`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo time travel`을 다룬다. |
| contract changes | 공통 응답 envelope은 `ApiResponse<T>` 형식의 `data`를 유지한다. 중복 비용이 큰 API는 `Idempotency-Key` 규칙을 포함한다. 이번 정리에는 `home`, `copilot`, `demo` 최소 계약 스케치가 추가된다. |
| security impact | `/api/auth/login`, `/api/auth/signup`, `/health`, Swagger 외 나머지 API는 JWT 보호 대상으로 본다. 타인 리소스는 `404`로 숨긴다. Copilot은 facts-only, Demo Time Travel은 demo account 전용으로 둔다. |
| non-functional impact | 문서 생성과 송금은 비동기 처리로 설계하고 `202 Accepted`를 사용한다. P0는 데모/테스트넷 기준이며 실거래/실정산을 다루지 않는다. Demo 모드는 `X-Demo-AsOf` 기준 재현성을 우선한다. |

## 범위

### 포함
- Money Home
- Auth
- WorkProof
- Advance
- Wage Shield
- Documents
- Instant Claim
- Remittance
- SafePay
- Vault
- Copilot
- Demo Time Travel

### 제외
- P1 범위 전부
- 실거래/실정산/실수익 보장
- Copilot 자유 대화형 확장
- 릴리스 빌드용 Demo Time Travel 노출

공유 초안 메모:
- 이번 문서는 PRD P0 전체를 같은 문서 안에 담는다.
- `Money Home`, `Copilot`, `Demo Time Travel`은 현재 세부 계약이 비교적 얇아도 제외하지 않고 최소 스케치로 유지한다.
- `W7 WorkProof Integrity`는 WorkProof 하위 규칙으로 포함하고, 별도 top-level 도메인으로 분리하지 않는다.
## 계약 안정도 구분

### 1. 확정
- 문서 구조는 `공통 규칙 -> 기능별 상세` 순서를 유지한다.
- 공통 응답 envelope은 `success`, `code`, `message`, `data`, `timestamp`를 사용한다.
- 공통 에러 형식도 같은 envelope을 사용한다.
- `auth`는 현재 백엔드 구현을 기준으로 문서에 반영한다.
- P0 범위는 문서에서 숨기지 않고 모두 같은 문서 안에 유지한다.
- 중복 비용이 큰 신청/전송/문서 생성 API는 `Idempotency-Key`를 사용한다.
- 문서 생성과 송금은 비동기 처리 전제를 둔다.
- 제품 정책은 테스트넷/데모 기준이며, 실거래/실정산을 의미하지 않는다.
- Copilot은 facts-only, 숫자 재계산 금지 원칙을 유지한다.

### 2. v0 가정
- `DAILY`는 미지정 시 `480분`을 기본값으로 두고, `MONTHLY`는 시스템 기본값을 제공한 뒤 조정 가능하게 둔다.
- WorkProof 누락 기록은 기존 record 수정과 분리한 provisional API로 먼저 둔다.
- WorkProof Integrity(W7)는 별도 독립 endpoint보다 record detail / monthly summary / advance eligibility 같은 응답 안에 녹여 노출한다.
- Advance 데모 상한은 `500,000 KRW` 기본값으로 둔다.
- Wage 확인 필요 상태 기본 임계값은 `30,000원 또는 2%`, 공제 미반영 시 `50,000원 또는 3%`로 둔다.
- WorkProof 동일 날짜 다중 근무는 후속 확장 전까지 허용하지 않는 방향으로 시작한다.
- Wage 계산 경계는 연장 `일별 480분 초과`, 야간 `22:00~06:00` 겹침 분으로 둔다.
- SafePay 고액 추가 확인 기준은 절대/상대값 혼합 정책으로 두되, 세부 임계값은 후속 정책 문서에서 고정한다.
- Vault는 순수 시뮬레이션이며 실제 온체인 예치/수익 실현을 하지 않는다.
- Claim 요약 문장은 facts 기반 템플릿/생성 결과를 반환하는 것으로 둔다.
- WorkProof 첨부는 `attachmentId` 참조 방식으로 설계하고, 업로드는 같은 도메인에서 선행 처리한다.
- Money Home은 현재 월 기준 단일 summary 응답을 우선 사용한다.
- Copilot은 하나의 intent 기반 endpoint로 설명 / 제출용 요약 / 번역을 함께 처리하는 방향을 우선 사용한다.
- Demo Time Travel은 `POST /api/demo/seed`, `POST /api/demo/reset`, `GET /api/demo/state`, `X-Demo-AsOf` 조합을 우선 사용한다.

### 3. 후속 확장
- Home 카드 단위 분리 API
- Home 개인화 추천 고도화
- Copilot RAG-lite / FAQ 연동
- Demo Time Travel 장면 스크립트/세부 제어 확장
- P1 범위 전부
- 다중 근무지 동시 활성 계약
- WorkProof 자동 지오펜스
- Wage 명세서 자동 파싱
- Remittance 정기 송금
- Vault 실제 메인넷 연동
## 공통 규칙

### Base Path
- `auth`: `/api/auth`
- `home`: `/api/home`
- `workproof`: `/api/workproof`
- `advance`: `/api/advance`
- `wage`: `/api/wage`
- `documents`: `/api/documents`
- `claim`: `/api/claim`
- `remittance`: `/api/remittance`
- `safepay`: `/api/safepay`
- `vault`: `/api/vault`
- `copilot`: `/api/copilot`
- `demo`: `/api/demo`

### 인증
- 기본값: `Authorization: Bearer {accessToken}` 필요
- 예외: `POST /api/auth/signup`, `POST /api/auth/login`, `/health`, Swagger

### 공통 헤더

| 헤더 | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | 보호 API만 필수 | JWT access token |
| `Idempotency-Key` | 일부 POST 필수, 일부 권장 | 중복 비용이 큰 요청 방지 키 |
| `Accept-Language` | 선택 | 다국어 안내 문구/정책 문구 출력 언어 |
| `X-Demo-AsOf` | demo mode 읽기 API에서 선택 | 데모 모드 기준일 고정 렌더(`YYYY-MM-DD`) |

### 공통 데모 모드 규칙
- `X-Demo-AsOf`는 demo account / demo mode에서만 유효하다.
- 우선 지원 대상은 `home`, `workproof`, `advance`, `wage`, `remittance`, `vault`, `copilot` 같은 읽기 API다.
- mutating API는 별도 명시가 없으면 `X-Demo-AsOf`를 무시한다.
- 일반 계정이나 릴리스 빌드에서는 이 헤더를 무시하거나 `403`으로 차단한다.
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
- 여기서 `status`는 비동기 job 상태를 뜻하고, 실제 문서/송금 리소스 상태는 각 도메인 섹션에서 별도로 정의한다.
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
| `PUT` | `/api/auth/me` | 현재 사용자 이름/전화번호 수정 | 필요 |

### `POST /api/auth/signup`

배경: 현재 백엔드 auth baseline을 그대로 반영해 초기 연동 차이를 줄인다.

Request:
| 필드 | 설명 |
| --- | --- |
| `email` | string |
| `password` | string, 최소 8자 |
| `name` | string, 최대 100자 |
| `phoneNumber` | string, 휴대폰 번호. 서버 저장 시 숫자만 정규화 |

Response `201 Created`:
| 필드 | 설명 |
| --- | --- |
| `userId` | - |
| `email` | - |
| `name` | - |
| `phoneNumber` | 정규화된 숫자 문자열 |
| `role` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `EMAIL_ALREADY_EXISTS` | - |
| `409` | `PHONE_NUMBER_ALREADY_EXISTS` | - |
| `400` | `VALIDATION_ERROR` | - |

### `POST /api/auth/login`

배경: JWT 발급 규칙은 기존 구현을 유지해 보안 설정과 문서가 어긋나지 않게 한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `email` | - |
| `password` | - |

Response `200 OK`:
| 필드 | 설명 |
| --- | --- |
| `accessToken` | - |
| `tokenType` | - |
| `expiresIn` | - |
| `userId` | - |
| `email` | - |
| `name` | - |
| `phoneNumber` | 정규화된 숫자 문자열 |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `401` | `INVALID_CREDENTIALS` | - |
| `400` | `VALIDATION_ERROR` | - |

### `GET /api/auth/me`

배경: 보호 화면 진입 시 현재 사용자 컨텍스트를 가장 단순하게 복구하기 위한 조회다.

Response `200 OK`:
| 필드 | 설명 |
| --- | --- |
| `userId` | - |
| `email` | - |
| `name` | - |
| `phoneNumber` | 정규화된 숫자 문자열 |
| `role` | - |

### `PUT /api/auth/me`

배경: 회원가입 이후 번호 변경, 누락된 번호 등록, 계정 표시 정보 수정 흐름을 지원한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `name` | string, 최대 100자 |
| `phoneNumber` | string, 휴대폰 번호. 서버 저장 시 숫자만 정규화 |

Response `200 OK`:
| 필드 | 설명 |
| --- | --- |
| `userId` | - |
| `email` | - |
| `name` | - |
| `phoneNumber` | 정규화된 숫자 문자열 |
| `role` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `PHONE_NUMBER_ALREADY_EXISTS` | - |
| `400` | `VALIDATION_ERROR` | - |

## 1A. Home

> PRD 7A 기준. 상태: `P0-초안`. 메인 홈 조합 응답의 최소 계약만 정의한다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/home/summary` | 이번 달 내 돈 상태, 다음 행동, 빠른 금융 액션 조회 |

### 1A.1 `GET /api/home/summary`

배경: 홈은 기록 목록보다 `이번 달 내 돈 상태`를 먼저 보여주는 핀테크 메인 화면이어야 한다.
v0 메모: 카드 분리 API 전에 hero/CTA/quick actions/today work를 하나의 조합 응답으로 먼저 묶는다.
확장 메모: 후속에 카드 단위 endpoint 또는 personalization layer로 분리할 수 있다.

Query:
| 필드 | 설명 |
| --- | --- |
| `month` | 선택. 미지정 시 현재 월(`YYYY-MM`) |

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `asOf` | 현재 해석 기준일. demo mode면 `X-Demo-AsOf` 반영 |
| `hero` | object. 하위 필드: estimatedWageAmount, actualIncomeStatus, advanceEligibleAmount, transferableAmount, statusReasonCode |
| `nextAction` | object. 하위 필드: actionKey, title, description, route, emphasis |
| `quickActions[]` | array<object>. 배열 항목: actionKey, title, route, enabled, badge |
| `todayWorkCard` | object 또는 null. 하위 필드: headline, supportText, reflectionStatus, actionLabel, route |
| `disclaimer` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | month 포맷 오류 |

## 2. WorkProof

> PRD 7B 기준. W1, W2, W3, W4, W5, W6를 API 관점에서 풀어 쓰고, W7 Integrity는 WorkProof 내부 상태/요약 필드로 반영한다.

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
| `POST` | `/api/workproof/records/missing` | 누락 근무 기록 생성(초안) |
| `POST` | `/api/workproof/records/{recordId}/modifications` | 기존 근무 기록 수정 |
| `GET` | `/api/workproof/records?month=YYYY-MM&workplaceId={id}` | 월별 기록 목록 |
| `GET` | `/api/workproof/records/{recordId}` | 기록 상세 |
| `GET` | `/api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}` | 월간 요약/반영 상태 |

### 2.1 `POST /api/workproof/workplaces`

배경: WorkProof는 workplace를 기준축으로 잡아 이후 contract와 record 해석이 흔들리지 않게 한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `name` | 1~100자 |
| `address` | 1~255자 |
| `mapLabel` | 선택 |
| `latitude` | 필수 |
| `longitude` | 필수 |

Response:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `name` | - |
| `address` | - |
| `mapLabel` | - |
| `latitude` | - |
| `longitude` | - |
| `allowedRadiusMeters` | 현재 허용 반경. v0 기본값 `1000` |
| `createdAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | - |

### 2.2 `GET /api/workproof/workplaces`

배경: 출근 화면과 월간 화면이 같은 근무지 목록을 재사용할 수 있게 단순 목록으로 둔다.

Response:
| 필드 | 설명 |
| --- | --- |
| `workplaces[]` | array<object>. 배열 항목: workplaceId, name, address, mapLabel, latitude, longitude, allowedRadiusMeters, hasActiveContract |

추가 메모:
- 현재 backend는 사용자의 workplace가 하나도 없으면 `SSAFY (임시)` workplace를 자동으로 1건 보장한다.
- `allowedRadiusMeters`는 현재 `1000` 기본값으로 내려간다.

### 2.3 `POST /api/workproof/contracts`

배경: 급여 입력은 단순 UI로 유지하되, 서버는 환산 시급 기준으로 재사용 가능한 계약을 만든다.
v0 메모: `DAILY`는 `dailyWorkMinutes` 미지정 시 `480`, `MONTHLY`는 `monthlyWorkMinutes` 미지정 시 시스템 기본값을 사용한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `payUnit` | `HOURLY`, `DAILY`, `MONTHLY` |
| `basePayAmount` | - |
| `dailyWorkMinutes` | `DAILY`일 때 선택, 미지정 시 `480` |
| `monthlyWorkMinutes` | `MONTHLY`일 때 선택, 미지정 시 시스템 기본값 사용 |
| `effectiveFrom` | 선택, 미지정 시 오늘 |

Response:
| 필드 | 설명 |
| --- | --- |
| `contractId` | - |
| `workplaceId` | - |
| `payUnit` | - |
| `basePayAmount` | - |
| `dailyWorkMinutes` | - |
| `monthlyWorkMinutes` | - |
| `normalizedHourlyWage` | - |
| `effectiveFrom` | - |
| `isActive` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `WORKPLACE_NOT_FOUND` | - |
| `409` | `ACTIVE_CONTRACT_EXISTS` | - |
| `400` | `VALIDATION_ERROR` | - |

### 2.4 `GET /api/workproof/contracts/current?workplaceId={id}`

배경: 체크인, 월간 집계, Wage 계산이 모두 같은 현재 활성 계약을 공통으로 참조한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `contractId` | - |
| `workplaceId` | - |
| `payUnit` | - |
| `basePayAmount` | - |
| `dailyWorkMinutes` | - |
| `monthlyWorkMinutes` | - |
| `normalizedHourlyWage` | - |
| `effectiveFrom` | - |
| `effectiveTo` | - |
| `isActive` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `ACTIVE_CONTRACT_NOT_FOUND` | - |

### 2.5 `POST /api/workproof/attachments`

배경: 수정 증빙과 문서 묶음에서 같은 첨부를 재사용하기 위해 선행 업로드로 둔다.

Request:
| 필드 | 설명 |
| --- | --- |
| `multipart/form-data` | - |
| `file` | - |
| `kind` | `PHOTO`, `MEMO_IMAGE`, `SCHEDULE_IMAGE`, `OTHER` |

Response:
| 필드 | 설명 |
| --- | --- |
| `attachmentId` | - |
| `fileName` | - |
| `contentType` | - |
| `size` | - |
| `uploadedAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `UNSUPPORTED_FILE_TYPE` | - |
| `413` | `FILE_TOO_LARGE` | - |

### 2.6 `POST /api/workproof/records/check-in`

배경: 원탭 UX를 위해 사용자는 `workplaceId`만 보내고 서버가 활성 계약을 해석한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `deviceAt` | - |
| `latitude` | - |
| `longitude` | - |
| `locationLabel` | - |

추가 메모:
- check-in은 선택한 workplace 중심 좌표와 `allowedRadiusMeters`를 비교해, 반경 밖 좌표면 기록을 생성하지 않는다.

Response:
| 필드 | 설명 |
| --- | --- |
| `recordId` | - |
| `workDate` | - |
| `status` | `CHECKED_IN` |
| `workplace` | object. 하위 필드: workplaceId, name, address, mapLabel, latitude, longitude |
| `contract` | object. 하위 필드: contractId, payUnit, basePayAmount, dailyWorkMinutes, monthlyWorkMinutes, normalizedHourlyWage, effectiveFrom, isActive |
| `checkIn` | object. 하위 필드: deviceAt, serverAt, latitude, longitude, locationLabel |
| `checkOut` | `null` |
| `reflectionStatus` | `PENDING` |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `WORKPLACE_NOT_FOUND` | - |
| `409` | `ACTIVE_CONTRACT_REQUIRED` | - |
| `409` | `ACTIVE_WORKPROOF_EXISTS` | - |
| `409` | `WORK_DATE_ALREADY_EXISTS` | - |
| `409` | `WORKPLACE_RADIUS_EXCEEDED` | 근무지 허용 반경 밖 |

### 2.7 `POST /api/workproof/records/check-out`

배경: 퇴근도 `recordId` 입력 없이 활성 `CHECKED_IN` 1건을 서버가 찾아 마감한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `deviceAt` | - |
| `latitude` | - |
| `longitude` | - |
| `locationLabel` | - |

추가 메모:
- check-out도 active workproof의 workplace 중심 좌표와 `allowedRadiusMeters`를 비교한다.
- 반경 밖 좌표여도 기록은 저장하되 `reflectionStatus=NEEDS_REVIEW`와 risk flag로 남기고, reflected 집계에서는 제외한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `recordId` | - |
| `workDate` | - |
| `status` | `CHECKED_OUT` |
| `checkIn` | object. 하위 필드: deviceAt, serverAt, latitude, longitude, locationLabel |
| `checkOut` | object 또는 null. 하위 필드: deviceAt, serverAt, latitude, longitude, locationLabel |
| `workedMinutes` | - |
| `reflectionStatus` | `REFLECTED` 또는 `NEEDS_REVIEW` |
| `riskFlags[]` | array<string>. 예: `CHECK_OUT_OUTSIDE_RADIUS` |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `ACTIVE_WORKPROOF_NOT_FOUND` | - |
| `409` | `CHECK_OUT_BEFORE_CHECK_IN` | - |

### 2.8 `POST /api/workproof/records/missing`

배경: PRD W4의 `기록이 누락됐어요` 케이스는 기존 `recordId` 기반 수정과 성격이 달라 provisional 생성 API로 분리한다.
v0 메모: 상세 필드와 후속 승인 흐름은 구현 중 조정 가능하다.
확장 메모: 추후 `manual record` 전용 모델이나 승인 상태로 재분리할 수 있다.

Request:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `workDate` | - |
| `checkInDeviceAt` | 선택 |
| `checkOutDeviceAt` | 선택 |
| `reasonCode` | `MISSING_RECORD` |
| `reasonMemo` | - |
| `attachmentIds` | 선택 |

Response:
| 필드 | 설명 |
| --- | --- |
| `recordId` | - |
| `workDate` | - |
| `status` | `NEEDS_REVIEW` |
| `modified` | `true` |
| `source` | `MANUAL_MISSING_RECORD` |
| `createdAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `WORKPLACE_NOT_FOUND` | - |
| `409` | `WORK_DATE_ALREADY_EXISTS` | - |
| `400` | `MISSING_RECORD_REASON_REQUIRED` | - |
| `400` | `INVALID_MISSING_RECORD_TIME` | - |

### 2.9 `POST /api/workproof/records/{recordId}/modifications`

배경: 이 API는 기존에 존재하는 record를 설명 가능한 방식으로 고치는 용도로만 둔다.
확장 메모: 누락 기록 생성은 별도 provisional API에서 먼저 다루고, 후속에 통합 여부를 다시 판단한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `checkInDeviceAt` | 선택 |
| `checkOutDeviceAt` | 선택 |
| `reasonCode` | `LATE_TAP`, `OVERTIME`, `BREAK_CHANGED`, `OTHER` |
| `reasonMemo` | 선택 |
| `attachmentIds` | 선택 |

Response:
| 필드 | 설명 |
| --- | --- |
| `recordId` | - |
| `modificationId` | - |
| `status` | - |
| `modified` | `true` |
| `modifiedAt` | - |
| `reasonCode` | - |
| `reasonMemo` | - |
| `attachmentCount` | - |
| `auditTrail` | object. 하위 필드: before, after, at |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `WORKPROOF_NOT_FOUND` | - |
| `400` | `MODIFICATION_REASON_REQUIRED` | - |
| `400` | `INVALID_MODIFICATION_TIME` | - |

예시:

```json
{
  "checkOutDeviceAt": "2026-03-18T19:10:00+09:00",
  "reasonCode": "OVERTIME",
  "reasonMemo": "잔업 1시간 추가",
  "attachmentIds": ["att_01", "att_02"]
}
```

### 2.10 `GET /api/workproof/records?month=YYYY-MM&workplaceId={id}`

배경: 월간 검토와 금융 연결 상태 확인이 한 화면에서 가능하도록 리스트를 단순하게 유지한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `workplaceId` | - |
| `records[]` | array<object>. 배열 항목: recordId, workDate, status, checkInDeviceAt, checkOutDeviceAt, workedMinutes, modified, reflectionStatus, riskFlags[] |

### 2.11 `GET /api/workproof/records/{recordId}`

배경: 기록 상세는 근무 증거와 수정 이력을 함께 보여주는 evidence-first 조회다.

Response:
| 필드 | 설명 |
| --- | --- |
| `recordId` | - |
| `workDate` | - |
| `status` | - |
| `workplace` | object. 하위 필드: workplaceId, name, address, mapLabel, latitude, longitude |
| `contract` | object. 하위 필드: contractId, payUnit, basePayAmount, dailyWorkMinutes, monthlyWorkMinutes, normalizedHourlyWage, effectiveFrom, isActive |
| `checkIn` | object. 하위 필드: deviceAt, serverAt, latitude, longitude, locationLabel |
| `checkOut` | object 또는 null. 하위 필드: deviceAt, serverAt, latitude, longitude, locationLabel |
| `reflectionStatus` | `PENDING`, `REFLECTED`, `NEEDS_REVIEW` |
| `workedMinutes` | - |
| `riskFlags[]` | array<string>. 예: `CHECK_OUT_OUTSIDE_RADIUS` |
| `modified` | - |
| `modifications[]` | array<object>. 배열 항목: modificationId, status, reasonCode, reasonMemo, modifiedAt, attachmentCount |
| `attachments[]` | array<object>. 배열 항목: attachmentId, fileName, contentType, size, uploadedAt |

### 2.12 `GET /api/workproof/monthly-summary?month=YYYY-MM&workplaceId={id}`

배경: Advance와 Wage가 직접 raw record를 다시 읽지 않도록 WorkProof 쪽에서 금융용 요약을 먼저 만든다.
v0 메모: 월간 요약은 별도 snapshot 저장 없이 조회 시 계산한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `workplaceId` | - |
| `workDayCount` | - |
| `totalWorkMinutes` | - |
| `overtimeMinutes` | - |
| `nightMinutes` | - |
| `modifiedRecordCount` | - |
| `reflection` | object. 하위 필드: reflectedRecordCount, needsReviewRecordCount, excludedRecordCount |
| `integrity` | object. 하위 필드: recordedWorkDays, reflectedWorkDays, verifiedMinutes, pendingMinutes, workproofRiskFlags[] |
| `financeReadiness` | object. 하위 필드: advanceEligibleWorkDays, wageUsableWorkDays |

추가 메모:
- `workproofRiskFlags[]`에는 `MODIFIED_RECORD_PRESENT`, `PENDING_WORKPROOF_PRESENT`, `CHECK_OUT_OUTSIDE_RADIUS_PRESENT`가 포함될 수 있다.

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

배경: 사용자가 `왜 이 금액이 가능한지`를 먼저 이해하도록 근거 설명 API를 분리한다.
v0 메모: `maxCap`는 데모 기본값 `500,000 KRW`를 사용한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `availableAmount` | - |
| `maxCap` | - |
| `policyRate` | - |
| `repaymentTier` | - |
| `reflectedWorkDays` | - |
| `reflectedWorkMinutes` | - |
| `verifiedMinutes` | - |
| `pendingMinutes` | - |
| `needsReviewRecordCount` | - |
| `blockReasonCodes[]` | array<string>. 차단 또는 제한 사유 코드 |
| `nextTierRemainingMinutes` | - |
| `estimatedFee` | - |
| `estimatedRepaymentDate` | - |
| `disclaimer` | - |

### 3.2 `POST /api/advance/requests`

배경: 신청 계열은 중복 호출 시 혼선이 커서 `Idempotency-Key`를 필수로 둔다.
v0 메모: 실제 대출 집행이 아니라 데모 시뮬레이션 승인/거절 결과를 돌려준다.
현재 구현 가정: P0 데모에서는 적격 시 즉시 `APPROVED` 응답을 반환하고, 동일 `Idempotency-Key` + 동일 payload 재시도는 기존 응답을 재생한다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 필수 |

Request:
| 필드 | 설명 |
| --- | --- |
| `workplaceId` | - |
| `requestedAmount` | - |
| `requestedAt` | - |

Response `201 Created`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `status` | `SUBMITTED`, `APPROVED`, `REJECTED`, `NEEDS_REVIEW` |
| `approvedAmount` | - |
| `feeAmount` | - |
| `repaymentDueDate` | - |
| `eligibilitySnapshot` | object. 하위 필드: availableAmount, maxCap, policyRate, reflectedWorkDays, reflectedWorkMinutes, needsReviewRecordCount |

Replay `200 OK`:
- 동일 `Idempotency-Key`와 동일 요청 본문을 다시 보내면 새 신청을 만들지 않고 기존 결과를 재반환할 수 있다.

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `ADVANCE_DUPLICATE_REQUEST` | - |
| `409` | `ADVANCE_NOT_ELIGIBLE` | - |
| `400` | `REQUEST_AMOUNT_EXCEEDS_LIMIT` | - |

### 3.3 `GET /api/advance/requests?month=YYYY-MM`

배경: 이번 달 신청 흐름을 월 단위로 가볍게 되짚어보는 이력 조회다.

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `requests[]` | array<object>. 배열 항목: requestId, workplaceId, requestedAmount, approvedAmount, status, repaymentDueDate, requestedAt |

### 3.4 `GET /api/advance/requests/{requestId}`

배경: 상세 조회는 신청 당시 근거와 결과를 다시 설명하기 위한 용도다.

Response:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `workplaceId` | - |
| `requestedAmount` | - |
| `approvedAmount` | - |
| `feeAmount` | - |
| `status` | - |
| `repaymentDueDate` | - |
| `eligibilitySnapshot` | object. 하위 필드: availableAmount, maxCap, policyRate, reflectedWorkDays, reflectedWorkMinutes, needsReviewRecordCount |
| `createdAt` | - |

## 4. Wage Shield

> PRD 7D 기준. 근로자가 먼저 이번 달 반영 근무와 실제 받은 돈을 확인하고, 필요 시 회사와 1차 확인으로 이어지는 보호 흐름이다. 결과는 최종 판정이 아니라 `확인 필요 상태와 근거`다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}` | 월간 근무/집계 요약 |
| `GET` | `/api/wage/estimate?month=YYYY-MM&workplaceId={id}` | 참고용 예상 급여 |
| `POST` | `/api/wage/verifications` | 실제 지급 결과 확인 기록 + 확인 필요 상태 판단 |
| `GET` | `/api/wage/verifications/{verificationId}` | 급여 확인 상세 |

### 4.1 `GET /api/wage/monthly-summary?month=YYYY-MM&workplaceId={id}`

배경: Wage 화면에서도 WorkProof 기반 집계 근거를 바로 재사용해, 근로자가 `이번 달 반영 근무`를 먼저 확인할 수 있게 별도 요약을 둔다.

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `workplaceId` | - |
| `contractId` | - |
| `payUnit` | - |
| `normalizedHourlyWage` | - |
| `workDayCount` | - |
| `verifiedWorkMinutes` | - |
| `overtimeMinutes` | - |
| `nightMinutes` | - |
| `modifiedRecordCount` | - |
| `includedRecordIds` | - |
| `excludedPendingRecordCount` | - |

### 4.2 `GET /api/wage/estimate?month=YYYY-MM&workplaceId={id}`

배경: 이 API는 최종 급여 확정이 아니라 참고용 예상 금액과 근거 설명을 위한 조회다.
v0 메모: 연장은 일별 `480분` 초과, 야간은 `22:00~06:00` 겹침 분, 휴일 가산은 제외한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `month` | - |
| `workplaceId` | - |
| `contract` | object. 하위 필드: contractId, payUnit, basePayAmount, dailyWorkMinutes, monthlyWorkMinutes, normalizedHourlyWage, effectiveFrom, isActive |
| `summary` | object. 하위 필드: workDayCount, verifiedWorkMinutes, overtimeMinutes, nightMinutes, modifiedRecordCount |
| `estimate` | object. 하위 필드: baseEstimate, overtimePremium, nightPremium, estimatedTotal |
| `disclaimer` | - |
| `ruleVersion` | - |

### 4.3 `POST /api/wage/verifications`

배경: 근로자가 실제 받은 돈을 확인하면, 시스템이 참고용 예상 금액과 비교해 `확인 필요 상태`와 근거를 정리하는 P0 핵심 액션이다.
v0 메모: 확인 필요 상태 기본 임계값은 `30,000원 또는 2%`, 공제 미반영 시 `50,000원 또는 3%`를 사용한다. 연결된 회사가 있더라도 이 API의 1차 주체는 근로자이며, 회사 참여는 후속 확인 단계에서만 다룬다.

Request:
| 필드 | 설명 |
| --- | --- |
| `month` | 확인 대상 월 |
| `workplaceId` | 확인 대상 근무지 |
| `actualDepositAmount` | 근로자가 확인한 실수령 금액 |
| `deductionsKnown` | 근로자가 알고 있는 공제 여부 |
| `memo` | 선택. 확인 메모 또는 회사 문의 전 참고 메모 |

Response `201 Created`:
| 필드 | 설명 |
| --- | --- |
| `verificationId` | - |
| `status` | `MATCHED`, `CHECK_REQUIRED` |
| `resolutionStage` | `SELF_CHECK`, `EMPLOYER_CONFIRMATION_RECOMMENDED` |
| `estimatedTotal` | 참고용 예상 금액 |
| `actualDepositAmount` | 근로자가 확인한 실수령 금액 |
| `differenceAmount` | 참고용 예상 금액 대비 차이 |
| `differenceRate` | 예상 금액 대비 차이 비율 |
| `threshold` | object. 하위 필드: absoluteWon, relativePercent, deductionRelaxed |
| `possibleCauses[]` | array<object>. 배열 항목: code, title, detail |
| `evidence` | object. 하위 필드: overtimeMinutes, nightMinutes, modifiedRecordCount, recordIds |
| `nextActions[]` | array<string>. 예: `VIEW_EVIDENCE`, `REQUEST_EMPLOYER_CONFIRMATION`, `PREPARE_PROOF_PACK` |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `ACTIVE_CONTRACT_REQUIRED` | - |
| `400` | `ACTUAL_DEPOSIT_REQUIRED` | - |

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

배경: 문서 생성과 Claim 준비가 이 결과를 다시 참조할 수 있게 상세 조회를 분리한다. 사용자 전면에서는 `급여 확인 결과`로 보이지만, 계약상 식별자는 `verification`을 유지한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `verificationId` | - |
| `month` | - |
| `workplaceId` | - |
| `status` | - |
| `resolutionStage` | `SELF_CHECK`, `EMPLOYER_CONFIRMATION_RECOMMENDED`, `EXTERNAL_HELP_PREPARATION` |
| `estimated` | object. 하위 필드: baseEstimate, overtimePremium, nightPremium, estimatedTotal |
| `actual` | object. 하위 필드: actualDepositAmount, deductionsKnown, submittedBy (`WORKER`) |
| `difference` | object. 하위 필드: differenceAmount, differenceRate, thresholdApplied |
| `threshold` | object. 하위 필드: absoluteWon, relativePercent, deductionRelaxed |
| `possibleCauses[]` | array<object>. 배열 항목: code, title, detail |
| `evidence` | object. 하위 필드: overtimeMinutes, nightMinutes, modifiedRecordCount, recordIds |
| `employerSupport` | object. 하위 필드: available, recommended, status (`NOT_REQUESTED`, `REQUEST_RECOMMENDED`, `PENDING_RESPONSE`, `RESOLVED`) |
| `relatedActions` | object. 하위 필드: proofPackReady, claimKitReady, instantClaimAvailable, proofPackDocumentId, claimKitDocumentId, preparationId |

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

배경: 문서는 개별 기능 화면 밖에서도 한 번에 다시 찾을 수 있는 공통 inbox 성격을 가진다.

Query:
| 파라미터 | 설명 |
| --- | --- |
| `type` | 선택 |
| `status` | 선택 |
| `month` | 선택 |

Response:
| 필드 | 설명 |
| --- | --- |
| `documents[]` | array<object>. 배열 항목: documentId, type, status (`QUEUED`, `RUNNING`, `READY`, `FAILED`), title, relatedEntityType, relatedEntityId, updatedAt |

### 5.2 `GET /api/documents/{documentId}`

배경: 문서 상세는 요약과 관련 링크를 같이 보여 다음 행동으로 이어지게 한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `documentId` | - |
| `type` | - |
| `status` | - |
| `title` | - |
| `summary` | 문서 요약 텍스트 또는 요약 블록 |
| `relatedLinks[]` | 문서와 함께 노출할 액션 링크 목록 |
| `createdAt` | - |
| `updatedAt` | - |
| `downloadable` | - |

### 5.3 `GET /api/documents/{documentId}/download-url`

배경: 실제 파일 접근은 별도 다운로드 URL 발급으로 분리해 보안과 만료 정책을 단순화한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `documentId` | - |
| `downloadUrl` | - |
| `expiresAt` | - |

### 5.4 `POST /api/documents/proof-packs`

배경: Wage 확인 결과를 설명하거나 회사/외부 기관과 공유할 때 바로 꺼낼 수 있는 증빙 리포트를 만든다.
v0 메모: 월간 요약, WorkProof 상세표, 급여 추정 근거, 수정 이력표, 첨부 목록(있으면)을 기본 포함 대상으로 본다. 입력 anchor는 `wageVerificationId` 하나로 고정하고, `month`와 `workplaceId`는 verification snapshot에서 파생한다. WorkProof 상세표가 필요하면 verification에 저장된 `recordIds`로 보조 조회한다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 필수 |

Request:
| 필드 | 설명 |
| --- | --- |
| `wageVerificationId` | - |

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `documentType` | `PROOF_PACK` |
| `status` | - |
| `pollUrl` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `DOCUMENT_DUPLICATE_REQUEST` | - |
| `404` | `WAGE_VERIFICATION_NOT_FOUND` | - |

추가 메모:
- Proof Pack은 legacy `POST /api/wage/deposits`, `GET /api/wage/summary`를 직접 입력으로 받지 않는다.
- 급여 계산/차액/threshold 설명은 verification 생성 시점 snapshot을 재사용한다.
- 현재 backend는 `pollUrl`을 `/api/documents/requests/{requestId}`로 연결하고, poll 응답에서 `documentId`와 `documentUrl`을 함께 돌려 detail endpoint로 이어지게 한다.

### 5.5 `POST /api/documents/claim-kits`

배경: 신고/상담 준비에서 필요한 자료를 한 번에 공유하기 위한 묶음 생성 요청이다.
v0 메모: Proof Pack + 제출용 요약 + 체크리스트를 기본으로 묶고, 첨부가 많으면 `ZIP`을 허용한다. 이 API도 follow-up 구현에서는 `wageVerificationId`를 anchor로 사용하고 나머지 월/근무지 문맥은 verification snapshot에서 파생하는 방향을 우선한다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 필수 |

Request:
| 필드 | 설명 |
| --- | --- |
| `wageVerificationId` | - |
| `includeAttachments` | boolean |
| `format` | `PDF`, `ZIP` |

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `documentType` | `CLAIM_KIT` |
| `status` | - |
| `pollUrl` | - |

추가 메모:
- Claim Kit도 `month`, `workplaceId`를 직접 입력으로 받지 않고 verification snapshot에서 파생한다.
- `claimKitDocumentId`가 필요한 downstream은 `pollUrl`이 가리키는 endpoint에서 `documentId`를 확인한 뒤 detail / claim preparation에 넘긴다.

### 5.6 `POST /api/documents/transfer-receipts`

배경: 송금 상세 화면에서 바로 다시 열 수 있는 영수증 문서를 생성한다.
v0 메모: Tx Hash와 송금 상태 요약을 문서 재사용 기준으로 포함한다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 필수 |

Request:
| 필드 | 설명 |
| --- | --- |
| `transferId` | - |

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `documentType` | `TRANSFER_RECEIPT` |
| `status` | - |
| `pollUrl` | - |

## 6. Instant Claim

> PRD 7H D3 기준. 자동 제출이 아니라 반자동 지원 흐름이다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/claim/routes?locale=ko-KR` | 신고/상담 경로 안내 |
| `POST` | `/api/claim/preparations` | Instant Claim 준비 데이터 생성 |
| `GET` | `/api/claim/preparations/{preparationId}` | 준비 결과 조회 |

### 6.1 `GET /api/claim/routes?locale=ko-KR`

배경: Instant Claim v0는 자동 제출이 아니라 제출 경로 안내를 우선하는 반자동 지원 흐름이다.

Response:
| 필드 | 설명 |
| --- | --- |
| `locale` | - |
| `routes[]` | array<object>. 배열 항목: channel (`ONLINE`, `PHONE`, `VISIT`), title, description, contact, link |

### 6.2 `POST /api/claim/preparations`

배경: 제출 자체를 대신하지 않고, 사용자가 복사·공유·바로가기를 쉽게 하도록 준비 데이터를 만든다.
v0 메모: `Idempotency-Key`는 현재 권장으로 두고, 구현 중 비용/캐시 전략을 보며 필수 전환 가능성을 연다. summary/checklist/route 추천의 1차 facts는 `wageVerificationId`가 가리키는 verification snapshot에서 읽고, `claimKitDocumentId`는 선택적으로 연결 문서를 덧붙이는 용도다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 권장 |

Request:
| 필드 | 설명 |
| --- | --- |
| `wageVerificationId` | - |
| `claimKitDocumentId` | 선택 |
| `locale` | - |
| `tone` | `DEFAULT`, `POLITE`, `SHORT` |

Response `201 Created`:
| 필드 | 설명 |
| --- | --- |
| `preparationId` | - |
| `status` | `READY` |
| `summaryText` | - |
| `checklist[]` | 제출 전에 확인할 체크리스트 목록 |
| `suggestedRoutes[]` | 추천 신고/상담 경로 목록 |
| `relatedDocuments[]` | 연결된 문서 목록 |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `404` | `WAGE_VERIFICATION_NOT_FOUND` | - |
| `404` | `CLAIM_KIT_NOT_FOUND` | - |

추가 메모:
- `claimKitDocumentId`가 들어오면 같은 사용자 소유이면서 같은 verification 문맥에서 생성된 문서만 허용한다.
- Claim preparation도 legacy `POST /api/wage/deposits`, `GET /api/wage/summary`를 직접 읽지 않는다.
- 현재 `WageVerification` snapshot이면 P0 준비 문구/체크리스트를 시작하기에 충분하다고 보고, 별도 historical contract snapshot 전용 기능은 후속으로 둔다.

### 6.3 `GET /api/claim/preparations/{preparationId}`

배경: 생성된 요약 문구와 체크리스트를 다시 열어보는 재조회 API다.

Response:
| 필드 | 설명 |
| --- | --- |
| `preparationId` | - |
| `status` | - |
| `summaryText` | - |
| `checklist[]` | 제출 전에 확인할 체크리스트 목록 |
| `suggestedRoutes[]` | 추천 신고/상담 경로 목록 |
| `relatedDocuments[]` | 연결된 문서 목록 |
| `createdAt` | - |

## 7. Remittance

> PRD 7E 기준. 테스트넷 전송이며, SafePay 정책과 함께 움직인다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/remittance/recipients` | 허용 목록 조회 |
| `POST` | `/api/remittance/recipients/search` | 전화번호 기반 수신자 검색 |
| `POST` | `/api/remittance/recipients` | 허용 목록 수신자 등록 |
| `GET` | `/api/remittance/transfers` | 송금 목록 조회 |
| `POST` | `/api/remittance/transfers` | 송금 요청 |
| `GET` | `/api/remittance/transfers/{transferId}` | 송금 상세/상태 조회 |

### 7.1 `GET /api/remittance/recipients`

배경: 송금은 허용 목록 기반으로만 움직인다는 SafePay 전제를 화면에서 먼저 확인하게 한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `recipients[]` | array<object>. 배열 항목: recipientId, alias, relation, walletAddress, allowed, recentlyUpdated, updatedAt |

### 7.1A `POST /api/remittance/recipients/search`

배경: 지갑 주소를 직접 입력하지 않고 DonDone 회원 전화번호로 송금 수신 후보를 찾기 위한 검색 단계다.

Request:
| 필드 | 설명 |
| --- | --- |
| `phoneNumber` | 휴대폰 번호. 서버에서 숫자만 정규화 |

Response:
| 필드 | 설명 |
| --- | --- |
| `candidates[]` | array<object>. 배열 항목: candidateUserId, displayName, maskedPhoneNumber, walletAddressMasked, alreadyRegistered |

### 7.2 `POST /api/remittance/recipients`

배경: 수신자 등록은 송금 실행보다 먼저 거치는 allowlist 관리 단계다.

Request:
| 필드 | 설명 |
| --- | --- |
| `alias` | - |
| `relation` | enum (`FAMILY`, `SPOUSE`, `PARENT`, `CHILD`, `SIBLING`, `FRIEND`, `OTHER`) |
| `walletAddress` | 직접 입력 등록 시 사용 |
| `targetUserId` | 전화번호 검색 결과 등록 시 사용 |
| `allowed` | boolean |

Response:
| 필드 | 설명 |
| --- | --- |
| `recipientId` | - |
| `alias` | - |
| `relation` | - |
| `walletAddress` | - |
| `allowed` | - |
| `recentlyUpdated` | - |
| `updatedAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `RECIPIENT_WALLET_ALREADY_EXISTS` | - |
| `400` | `INVALID_WALLET_ADDRESS` | - |

### 7.3 `GET /api/remittance/transfers`

배경: 송금 상세로 다시 들어가기 전, 월별 상태 추적을 빠르게 보는 목록이다.

Query:
| 파라미터 | 설명 |
| --- | --- |
| `month` | - |
| `status` | - |

Response:
| 필드 | 설명 |
| --- | --- |
| `transfers[]` | array<object>. 배열 항목: transferId, recipientId, amount, status (`SUBMITTED`, `CONFIRMED`, `FAILED`, `BLOCKED`), txHash, createdAt |

### 7.4 `POST /api/remittance/transfers`

배경: SafePay 검사와 비동기 전송 상태 추적을 전제로 한 테스트넷 송금 요청이다.
v0 메모: `transferId`는 생성 직후 부여하고, 실제 전송 진행은 `pollUrl`과 상세 조회로 확인한다.
확장 메모: 정기/자동 송금은 P1에서 별도 흐름으로 분리한다.

Headers:
| 헤더 | 규칙 | 설명 |
| --- | --- | --- |
| `Idempotency-Key` | - | 필수 |

Request:
| 필드 | 설명 |
| --- | --- |
| `recipientId` | - |
| `tokenSymbol` | - |
| `amount` | - |
| `memo` | 선택 |

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `transferId` | - |
| `status` | - |
| `safepayDecision` | - |
| `pollUrl` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `409` | `DUPLICATE_TRANSFER_REQUEST` | - |
| `403` | `SAFEPAY_BLOCKED` | - |
| `404` | `RECIPIENT_NOT_FOUND` | - |

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

배경: 송금 상세는 상태, Tx Hash, 영수증 연결까지 한 번에 다시 열 수 있어야 한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `transferId` | - |
| `recipient` | object. 하위 필드: recipientId, name, alias, relationship, walletAddress |
| `amount` | - |
| `status` | - |
| `txHash` | - |
| `failureReason` | - |
| `safepay` | object. 하위 필드: decision, reasonCodes, userMessage, requiresAdditionalConfirm, cooldownUntil |
| `receiptDocumentId` | - |
| `createdAt` | - |
| `updatedAt` | - |

## 8. SafePay

> PRD 7F 기준. Remittance 요청 전/중에 적용되는 보호 정책이다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/safepay/transfer-checks` | 송금 전 정책 검사 |

### 8.1 `POST /api/safepay/transfer-checks`

배경: SafePay는 차단만 하는 모듈이 아니라 `왜 막혔는지`를 사용자 언어로 설명하는 전처리 API다.
v0 메모: 절대/상대 혼합 기준 수치는 후속 정책 문서에서 조정 가능하다.

Request:
| 필드 | 설명 |
| --- | --- |
| `recipientId` | - |
| `amount` | - |
| `tokenSymbol` | - |

Response:
| 필드 | 설명 |
| --- | --- |
| `decision` | `ALLOW`, `WARN`, `BLOCK` |
| `reasonCodes[]` | array<string>. 차단/경고 사유 코드 목록 |
| `userMessage` | - |
| `cooldownUntil` | - |
| `requiresAdditionalConfirm` | - |

주요 이유 코드 예시:
| 코드 | 설명 |
| --- | --- |
| `RECIPIENT_IN_COOLDOWN` | - |
| `AMOUNT_TOO_HIGH` | - |
| `RECIPIENT_NOT_ALLOWLISTED` | - |
| `DUPLICATE_TRANSFER_SUSPECTED` | - |

## 9. Vault

> testnet demo 기준. 비동기 예치/출금 상태를 제공하지만 실제 수익 보장을 의미하지 않는다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/vault/summary` | 보관/이자 요약 조회 |
| `POST` | `/api/vault/deposits` | Vault 예치 요청 생성 |
| `POST` | `/api/vault/withdrawals` | Vault 출금 요청 생성 |
| `GET` | `/api/vault/transactions` | Vault 거래 목록 조회 |
| `GET` | `/api/vault/transactions/{requestId}` | Vault 거래 상세 조회 |

### 9.1 `GET /api/vault/summary`

배경: Vault는 testnet vault position과 wallet 잔액을 함께 보여주는 요약 조회다.
v0 메모: 예상 이자는 예시값이며 수익 보장을 의미하지 않는다.

Response:
| 필드 | 설명 |
| --- | --- |
| `walletAddress` | 사용자 remittance 지갑 주소 |
| `vaultAddress` | Vault 컨트랙트 주소 |
| `network` | 예: `sepolia` |
| `assetSymbol` | 예: `dUSDC` |
| `assetDecimals` | 예: `6` |
| `storedAmountAtomic` | 현재 vault principal |
| `accruedYieldAtomic` | 누적 예상 이자 |
| `walletTokenBalanceAtomic` | 지갑에 남아 있는 토큰 잔액 |
| `availableToStoreAmountAtomic` | 추가 예치 가능 잔액 |
| `shareBalance` | vault share 잔액 |
| `interestPreview` | object. 하위 필드: `dailyEstimatedYieldAtomic`, `monthlyEstimatedYieldAtomic`, `yearlyEstimatedYieldAtomic`, `apyBps` |
| `disclaimer` | - |

### 9.2 `POST /api/vault/deposits`

배경: 사용자 지갑의 testnet 토큰을 Vault로 예치하는 비동기 요청을 생성한다.
v0 메모: `Idempotency-Key` 헤더가 필요하며, 응답은 `REQUESTED -> SIGNED -> BROADCASTED -> CONFIRMED` 흐름을 따른다.

Request:
| 필드 | 설명 |
| --- | --- |
| Header `Idempotency-Key` | 중복 요청 방지 키 |
| `amountAtomic` | - |

Response:
| 필드 | 설명 |
| --- | --- |
| `requestId` | vault transaction 식별자 |
| `txType` | `DEPOSIT` |
| `status` | 최초 `REQUESTED` |
| `detailPath` | `/api/vault/transactions/{requestId}` |
| `createdAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `IDEMPOTENCY_KEY_REQUIRED` | - |
| `400` | `INVALID_VAULT_AMOUNT` | - |
| `409` | `VAULT_INSUFFICIENT_AVAILABLE_BALANCE` | - |
| `409` | `VAULT_TRANSACTION_ALREADY_IN_PROGRESS` | - |
| `409` | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` | - |

### 9.3 `POST /api/vault/withdrawals`

배경: Vault에 보관 중인 금액을 사용자 지갑으로 되돌리는 비동기 요청을 생성한다.
v0 메모: `Idempotency-Key` 헤더가 필요하다.

Request:
| 필드 | 설명 |
| --- | --- |
| Header `Idempotency-Key` | 중복 요청 방지 키 |
| `amountAtomic` | - |

Response:
| 필드 | 설명 |
| --- | --- |
| `requestId` | vault transaction 식별자 |
| `txType` | `WITHDRAW` |
| `status` | 최초 `REQUESTED` |
| `detailPath` | `/api/vault/transactions/{requestId}` |
| `createdAt` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `IDEMPOTENCY_KEY_REQUIRED` | - |
| `400` | `INVALID_VAULT_AMOUNT` | - |
| `409` | `VAULT_INSUFFICIENT_STORED_BALANCE` | - |
| `409` | `VAULT_TRANSACTION_ALREADY_IN_PROGRESS` | - |
| `409` | `IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD` | - |

### 9.4 `GET /api/vault/transactions`

배경: 예치/출금 비동기 상태를 최신순으로 확인한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `transactions[]` | 거래 목록 |
| `transactions[].requestId` | - |
| `transactions[].txType` | `DEPOSIT`, `WITHDRAW` |
| `transactions[].status` | `REQUESTED`, `SIGNED`, `BROADCASTED`, `CONFIRMED`, `FAILED`, `TIMED_OUT` |
| `transactions[].amountAtomic` | - |
| `transactions[].shareDelta` | - |
| `transactions[].txHash` | - |
| `transactions[].failureCode` | 실패 시 코드 |
| `transactions[].updatedAt` | - |

### 9.5 `GET /api/vault/transactions/{requestId}`

배경: 단건 거래의 확정 여부와 txHash를 확인한다.

Response:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `txType` | `DEPOSIT`, `WITHDRAW` |
| `status` | - |
| `walletAddress` | - |
| `vaultAddress` | - |
| `assetSymbol` | - |
| `amountAtomic` | - |
| `shareDelta` | - |
| `txHash` | - |
| `failureCode` | - |
| `createdAt` | - |
| `updatedAt` | - |
| `confirmedAt` | 성공 확정 시각 |

## 10. Copilot

> PRD 8 기준. 상태: `P0-초안`. facts-only 보조 응답의 최소 계약을 정의한다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/copilot/answers` | 화면 facts 기반 설명 / 제출용 요약 / 번역 생성 |

### 10.1 `POST /api/copilot/answers`

배경: Copilot은 자유 대화보다 `현재 화면을 더 쉽게 이해하고 행동하게 돕는 보조 응답`을 우선한다.
v0 메모: 설명 / 제출용 요약 / 번역을 하나의 intent 기반 endpoint로 먼저 묶는다.
확장 메모: explain / claim-summary / translate 분리 endpoint로 재편할 수 있다.

Request:
| 필드 | 설명 |
| --- | --- |
| `intent` | `EXPLAIN_SCREEN`, `DRAFT_CLAIM_SUMMARY`, `TRANSLATE_FACTS` |
| `screenType` | `HOME`, `WAGE_VERIFICATION`, `SAFEPAY`, `CLAIM_PREPARATION` |
| `question` | 선택 |
| `locale` | 선택 |
| `targetLocale` | 번역 시 선택 |
| `tone` | 선택. `PLAIN`, `POLITE`, `SHORT` |
| `facts` | object. 서버 facts 스냅샷 |

Response:
| 필드 | 설명 |
| --- | --- |
| `intent` | - |
| `answerText` | - |
| `evidenceFacts[]` | array<object>. 배열 항목: label, value, sourceKey |
| `suggestedQuestions[]` | array<string> |
| `disclaimer` | `이 안내는 화면에 표시된 사실을 바탕으로 작성된 참고 문장입니다.` |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `FACTS_REQUIRED` | facts 누락 |
| `400` | `UNSUPPORTED_COPILOT_INTENT` | 지원하지 않는 intent |
| `403` | `COPILOT_NOT_AVAILABLE` | 현재 화면/계정에서 비활성 |

## 11. Demo Time Travel

> PRD 9 기준. 상태: `P0-초안`. demo account 전용 재현 장치의 최소 계약을 정의한다.

### 주요 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/demo/seed` | 고정 demo seed 준비 |
| `POST` | `/api/demo/reset` | demo state reset |
| `GET` | `/api/demo/state?asOf=YYYY-MM-DD` | 현재 demo meta/state 조회 |

### 11.1 `POST /api/demo/seed`

배경: 데모 시작 전 고정 시드 데이터를 한 번 주입해 슬라이더 재생 시나리오를 안정화한다.
v0 메모: 소수의 고정 scenario key로 시작한다.

Request:
| 필드 | 설명 |
| --- | --- |
| `scenarioKey` | 선택. 예: `DEFAULT_MONTH_JOURNEY`, `WAGE_GAP_CASE` |

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `seedId` | - |
| `scenarioKey` | - |
| `defaultAsOf` | - |
| `minAsOf` | - |
| `maxAsOf` | - |
| `enabled` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `403` | `DEMO_MODE_DISABLED` | demo mode 비활성 |
| `409` | `DEMO_SEED_ALREADY_ACTIVE` | 활성 seed 존재 |

### 11.2 `POST /api/demo/reset`

배경: 데모 재시작을 빠르게 하기 위해 현재 demo state를 초기화한다.

Response `202 Accepted`:
| 필드 | 설명 |
| --- | --- |
| `requestId` | - |
| `status` | `QUEUED` 또는 `DONE` |
| `pollUrl` | - |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `403` | `DEMO_MODE_DISABLED` | - |

### 11.3 `GET /api/demo/state?asOf=YYYY-MM-DD`

배경: 슬라이더/재생 UI가 현재 demo range와 scene 구간을 읽을 수 있게 하는 메타 조회다.
v0 메모: 개별 도메인 화면은 별도 demo endpoint보다 기존 읽기 API + `X-Demo-AsOf`를 우선 사용한다.

Query:
| 필드 | 설명 |
| --- | --- |
| `asOf` | 선택. 미지정 시 현재 demo 기준일 |

Response:
| 필드 | 설명 |
| --- | --- |
| `enabled` | - |
| `seedId` | - |
| `currentAsOf` | - |
| `minAsOf` | - |
| `maxAsOf` | - |
| `scenes[]` | array<object>. 배열 항목: sceneKey, label, from, to |
| `notes[]` | array<string>. 현재 장면 설명 또는 안내 문구 |

주요 에러:
| HTTP | Code | 설명 |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | asOf 포맷 오류 |
| `403` | `DEMO_MODE_DISABLED` | demo mode 비활성 |

## 공통 구현 메모
- `auth`는 현재 구현을 기준으로 유지한다.
- `home`, `workproof`, `advance`, `wage`, `documents`, `claim`, `remittance`, `safepay`, `vault`, `copilot`, `demo`는 feature-first 구조를 유지한다.
- 문서 생성과 송금은 비동기 job 모델을 따른다.
- Demo Time Travel 공통 해석은 `demo` 또는 shared resolver에서 처리하되, 실제 계산은 각 도메인 서비스가 맡는다.
- Copilot은 raw user prompt만 보지 않고 서버 facts 스냅샷을 입력으로 받는다.
- 외부 연동은 `adapter` 인터페이스 뒤에 둔다.
- 공유용 초안에서는 각 API에 짧은 `배경` / `v0 메모`를 붙여 결정 이유와 조정 가능 지점을 함께 보여준다.
- PRD 전체 P0 기준 문서이므로, 덜 굳은 항목도 최소 스케치 상태로 문서 안에 남긴다.

## 다음 보강 후보
- `summary`, `relatedDocuments[]`, `checklist[]`, `relatedLinks[]` 같은 중첩 object의 최소 의미 정의 보강
- Home / Copilot / Demo Time Travel 예시 JSON 추가
- domain별 DTO field-level validation 상세화
- W7 Integrity 결과 필드의 명확한 노출 위치 정리
