# WorkProof Pay - Web/Android API 연동 가이드 (P0)

## 1) 목적
이 문서는 웹/안드로이드 팀이 서버 API만으로 SafePay 송금 플로우를 구현할 수 있도록 요청/응답 규격을 고정한다.

- 대상 범위: 송금 허용목록, 송금 요청, 상태 조회, 영수증 링크
- 정책 반영: 허용목록, 24시간 쿨다운, 고액 확인, 중복 방지(idempotency)

현재 지갑 구조:

- 서버가 사용자별 wallet을 생성한다.
- wallet address는 DB에 저장한다.
- private key는 암호화 저장한다.
- 송금 시 각 사용자 wallet private key로 서명한다.

## 2) 상태/코드 표준

### 송금 상태
- `REQUESTED`
- `SUBMITTED`
- `CONFIRMED`
- `FAILED`

### 정책 차단 코드 (`policyCode`)
- `RECIPIENT_NOT_ALLOWED`
- `COOLDOWN_ACTIVE`
- `HIGH_AMOUNT_CONFIRM_REQUIRED`

### 실패 코드 (`failureCode`)
- `NETWORK_ERROR`
- `POLICY_BLOCKED`
- `CHAIN_REVERT`
- `UNKNOWN`

## 3) 공통 헤더
- `Authorization: Bearer <JWT>`
- `X-Request-Id: <uuid>` (권장)
- `Idempotency-Key: <uuid or stable-key>` (송금 생성 필수)
- `X-User-Id: <userId>` (현재 샘플 구현에서는 필수에 가까움)

## 4) API 스펙

### 4.1 수신자 허용목록 조회
`GET /api/v1/remittance/recipients`

응답 예시:
```json
{
  "items": [
    {
      "recipientId": "rcp_001",
      "alias": "Mom",
      "walletAddress": "0x7F...A1",
      "relation": "FAMILY",
      "allowed": true,
      "updatedAt": "2026-03-05T10:00:00Z",
      "cooldownEndsAt": "2026-03-06T10:00:00Z"
    }
  ]
}
```

### 4.0 내 wallet 생성
`POST /api/v1/remittance/wallets/me`

응답 예시:
```json
{
  "userId": "user_001",
  "walletAddress": "0xAB...12",
  "createdAt": "2026-03-11T10:00:00Z"
}
```

### 4.0-1 내 wallet 조회
`GET /api/v1/remittance/wallets/me`

참고:

- 첫 송금 요청 시 wallet이 없으면 서버가 자동 생성한다.
- 다만 앱에서는 가입 직후 `POST /wallets/me`를 먼저 호출해 주소를 확보하는 방식을 권장한다.

### 4.0-2 데모 시드 데이터 주입
`POST /api/v1/remittance/demo/seed`

설명:

- 정상 케이스 1개를 DB에 주입한다.
- 발표용/QA용 재현 데이터로 사용한다.

### 4.2 수신자 허용/수정
`PUT /api/v1/remittance/recipients/{recipientId}`

요청 예시:
```json
{
  "alias": "Mom",
  "walletAddress": "0x7F...A1",
  "relation": "FAMILY",
  "allowed": true
}
```

응답 예시:
```json
{
  "recipientId": "rcp_001",
  "allowed": true,
  "updatedAt": "2026-03-05T10:00:00Z",
  "cooldownEndsAt": "2026-03-06T10:00:00Z"
}
```

### 4.3 송금 사전 점검
`POST /api/v1/remittance/transfers/precheck`

요청 예시:
```json
{
  "recipientId": "rcp_001",
  "amount": "120000",
  "asset": "dUSDC",
  "highAmountConfirmed": false
}
```

응답 예시:
```json
{
  "allowed": false,
  "policyCode": "HIGH_AMOUNT_CONFIRM_REQUIRED",
  "waitSeconds": 0,
  "highAmountThreshold": "100000"
}
```

### 4.4 송금 요청 생성
`POST /api/v1/remittance/transfers`

- 필수 헤더: `Idempotency-Key`
- 동일 `Idempotency-Key` 재요청 시 기존 `transferId`를 그대로 반환

요청 예시:
```json
{
  "recipientId": "rcp_001",
  "amount": "50000",
  "asset": "dUSDC",
  "highAmountConfirmed": true,
  "memo": "March support"
}
```

응답 예시:
```json
{
  "transferId": "tr_20260305_0001",
  "status": "REQUESTED",
  "policy": {
    "policyCode": null,
    "cooldownEndsAt": null
  },
  "createdAt": "2026-03-05T10:10:00Z"
}
```

### 4.5 송금 단건 조회
`GET /api/v1/remittance/transfers/{transferId}`

응답 예시:
```json
{
  "transferId": "tr_20260305_0001",
  "status": "CONFIRMED",
  "asset": "dUSDC",
  "amount": "50000",
  "senderAddress": "0xUserWallet...",
  "recipientAddress": "0x7F...A1",
  "txHash": "0xf1...9a",
  "failureCode": null,
  "updatedAt": "2026-03-05T10:10:19Z"
}
```

### 4.6 영수증 다운로드 링크 발급
`POST /api/v1/remittance/transfers/{transferId}/receipt-link`

응답 예시:
```json
{
  "transferId": "tr_20260305_0001",
  "downloadUrl": "https://s3....",
  "expiresAt": "2026-03-05T10:25:00Z"
}
```

### 4.7 무결성 해시 조회
`GET /api/v1/remittance/transfers/{transferId}/integrity-hash`

응답 예시:
```json
{
  "docType": "TRANSFER_RECEIPT",
  "sourceRef": "tr_seed_001",
  "itemCount": 1,
  "normalizedPayload": "{\"schemaVersion\":\"transfer-integrity-v1\"}",
  "payloadHash": "0x1234",
  "proofId": "0xabcd"
}
```

## 5) 앱 구현 규칙 (필수)
- 송금 버튼 클릭 시 `Idempotency-Key`를 먼저 생성 후 요청한다.
- HTTP 타임아웃/네트워크 실패 시 같은 `Idempotency-Key`로 재시도한다.
- 상태는 `REQUESTED -> SUBMITTED -> CONFIRMED/FAILED`를 폴링 또는 SSE로 구독한다.
- `policyCode=COOLDOWN_ACTIVE`면 남은 시간(`waitSeconds`) UI를 표시한다.
- `policyCode=HIGH_AMOUNT_CONFIRM_REQUIRED`면 추가 확인 체크 후 재요청한다.
- 앱은 회원가입 직후 또는 첫 송금 전 `POST /wallets/me`를 호출해 wallet을 생성해야 한다.
- `senderAddress`는 사용자의 내부 wallet address로 이해해야 한다.

## 6) 블록체인 매핑 (백엔드 구현 참고)
- 컨트랙트: `SafePayRemittance`
- 핵심 함수:
  - `setRecipientAllow(address recipient, bool allowed)`
  - `requestTransfer(address recipient, uint256 amount, string idempotencyKey, bool highAmountConfirmed)`
  - `markSubmitted(uint256 requestId, bytes32 txHash)`
  - `markConfirmed(uint256 requestId, bytes32 txHash)`
  - `markFailed(uint256 requestId, string failureCode)`

## 7) 에러 응답 포맷
모든 4xx/5xx는 아래 구조를 사용한다.

```json
{
  "error": {
    "code": "POLICY_BLOCKED",
    "message": "Cooldown is active",
    "requestId": "f1fbe5b0-5f5f-48ed-9b17-3341537f9f74",
    "details": {
      "policyCode": "COOLDOWN_ACTIVE",
      "waitSeconds": 3600
    }
  }
}
```
