# Hash Spec

이 문서는 현재 프로젝트에서 사용하는 **무결성 해시 규격 초안 v1**이다.

현재는 `TRANSFER_RECEIPT` 문서 타입에 대해 실제 코드에 적용되어 있다.

적용 API:

- `GET /api/v1/remittance/transfers/{transferId}/integrity-hash`

## 1. 목적

문서나 영수증의 핵심 필드를 정규화한 뒤 해시를 계산해,
나중에 위변조 여부를 검증할 수 있게 한다.

## 2. 문서 타입

현재 지원:

- `TRANSFER_RECEIPT`

후속 확장 예정:

- `PROOF_PACK`
- `CLAIM_KIT`

## 3. 정규화 payload 규격

현재 `TRANSFER_RECEIPT`는 아래 필드를 이 순서대로 JSON으로 만든다.

```json
{
  "schemaVersion": "transfer-integrity-v1",
  "docType": "TRANSFER_RECEIPT",
  "sourceRef": "<transferId>",
  "itemCount": 1,
  "transferId": "<transferId>",
  "userId": "<userId>",
  "senderAddress": "<senderAddress>",
  "recipientAddress": "<recipientAddress>",
  "asset": "<asset>",
  "amountAtomic": 50000,
  "status": "CONFIRMED",
  "txHash": "<txHash>",
  "failureCode": "",
  "updatedAt": "2026-03-05T10:00:10Z"
}
```

규칙:

- JSON 키 순서는 위 순서를 고정한다.
- 문자열 `null` 값은 빈 문자열 `""`로 정규화한다.
- 날짜는 `ISO-8601` 문자열을 사용한다.
- 인코딩은 UTF-8 기준이다.

## 4. 해시 생성 규칙

### 4-1) payloadHash

```text
payloadHash = keccak256(normalizedPayload)
```

코드에서는 `sha3(normalizedPayloadString)`로 생성한다.

### 4-2) proofId

```text
proofId = keccak256(docType + ":" + sourceRef + ":" + payloadHash)
```

## 5. 메타 구조

문서 무결성 메타는 아래 필드를 가진다.

- `docType`
- `sourceRef`
- `itemCount`
- `normalizedPayload`
- `payloadHash`
- `proofId`

온체인 `DocumentHashRegistry`와 연결할 때는 아래 형태로 매핑한다.

- `proofId`
- `payloadHash`
- `docType`
- `sourceRef`
- `itemCount`

## 6. 실제 예시

예시 source:

- `transferId = tr_seed_001`

예시 호출:

```bash
curl -s 'http://localhost:8080/api/v1/remittance/transfers/tr_seed_001/integrity-hash' \
  -H 'X-User-Id: user_001'
```

예상 응답 형태:

```json
{
  "docType": "TRANSFER_RECEIPT",
  "sourceRef": "tr_seed_001",
  "itemCount": 1,
  "normalizedPayload": "{\"schemaVersion\":\"transfer-integrity-v1\",...}",
  "payloadHash": "0x...",
  "proofId": "0x..."
}
```

## 7. 코드 위치

- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/service/TransferIntegrityService.java`
- `/home/ssafy/crypto-pjt/src/DocumentHashRegistry.sol`
