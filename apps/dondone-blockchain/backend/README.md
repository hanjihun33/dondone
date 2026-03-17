# Remittance API Server

Spring Boot 기반 송금 API 서버입니다.

이 문서는 **처음 보는 사람 기준**으로, 아래 순서대로 따라 하면 서버 실행과 기본 송금 API 검증까지 할 수 있게 작성했습니다.

## 0. 현재 지갑 구조 먼저 이해하기

이 서버는 지금 아래 방식으로 동작합니다.

- Demo 모드
  - 회원별 wallet을 자동 생성해 DB에 저장합니다.
  - 송금 상태는 시뮬레이션하지만, 송신 주소는 사용자 wallet address를 사용합니다.
- Sepolia 모드
  - 회원별 wallet을 자동 생성해 DB에 저장합니다.
  - private key는 암호화해서 저장합니다.
  - 송금 시 해당 사용자 wallet private key를 복호화해서 서명합니다.

정리하면, 현재는 `사용자별 내부 wallet(custodial wallet)을 서버가 관리하는 구조`입니다.

## 1. 준비물

아래 2개가 설치되어 있어야 합니다.

- Java 17
- Maven 3.9+

Sepolia까지 쓸 경우 추가로 필요:

- `SEPOLIA_RPC_URL`
- `SEPOLIA_TOKEN_ADDRESS`
- `WALLET_ENCRYPTION_KEY`

버전 확인:

```bash
java -version
mvn -version
```

## 2. 서버 실행 (Demo 모드, 가장 쉬움)

프로젝트 루트(`/home/ssafy/crypto-pjt`)에서 실행:

```bash
cd /home/ssafy/crypto-pjt/backend
mvn spring-boot:run
```

서버가 뜨면 새 터미널에서 확인:

```bash
curl -i http://localhost:8080/api/v1/remittance/recipients
```

`200 OK`가 나오면 정상입니다.

선택:

운영과 비슷하게 wallet 암호화 키를 직접 넣고 싶으면 아래를 먼저 실행합니다.

```bash
openssl rand -base64 32
export WALLET_ENCRYPTION_KEY=<base64_aes_key>
```

## 3. Demo 모드에서 송금 API 빠르게 확인

### 3-0) 정상 시드 데이터 주입

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/demo/seed'
```

이 API는 `backend/src/main/resources/demo/remittance-seed-normal-case.json` 파일을 읽어서
정상 케이스 1개를 DB에 넣습니다.

### 3-0) 내 wallet 생성

처음 한 번은 wallet을 만들어야 합니다.

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: user_001'
```

응답 예시:

```json
{
    "userId": "user_001",
    "walletAddress": "0x0690f77613ca53787b1dd66f4bff7045d38c6ee5",
    "createdAt": "2026-03-11T02:42:20.618940745Z"
}
```

이 주소가 앞으로 `user_001`의 송신 wallet 주소입니다.

참고:

- `POST /wallets/me`를 미리 호출하지 않아도, 첫 송금 생성 시 서버가 wallet을 자동 생성합니다.
- 다만 처음 보는 사람은 `POST /wallets/me`를 먼저 호출해 주소를 확인하는 편이 더 이해하기 쉽습니다.

### 3-0-1) 내 wallet 조회

```bash
curl -s 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: user_001'
```

### 3-1) 수신자 등록

```bash
curl -s -X PUT 'http://localhost:8080/api/v1/remittance/recipients/rcp_001' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user_001' \
  -d '{
    "alias":"Mina",
    "walletAddress":"0x1111111111111111111111111111111111111111",
    "relation":"family",
    "allowed":true
  }'
```

### 3-2) precheck

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers/precheck' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user_001' \
  -d '{
    "recipientId":"rcp_001",
    "asset":"USDC",
    "amount":1000,
    "highAmountConfirmed":false
  }'
```

### 3-3) 송금 생성

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user_001' \
  -H 'Idempotency-Key: idem-001' \
  -d '{
    "recipientId":"rcp_001",
    "asset":"USDC",
    "amount":1000,
    "highAmountConfirmed":false
  }'
```

응답의 `transferId`를 복사합니다.

### 3-4) 상태 조회

아래에서 `<TRANSFER_ID>`를 방금 값으로 바꿉니다.

```bash
curl -s "http://localhost:8080/api/v1/remittance/transfers/<TRANSFER_ID>" \
  -H 'X-User-Id: user_001'
```

상태는 `REQUESTED -> SUBMITTED -> CONFIRMED/FAILED` 순서로 바뀝니다.

### 3-5) 무결성 해시 조회

```bash
curl -s 'http://localhost:8080/api/v1/remittance/transfers/tr_seed_001/integrity-hash' \
  -H 'X-User-Id: user_001'
```

이 응답에는 아래 값이 들어 있습니다.

- `normalizedPayload`
- `payloadHash`
- `proofId`

## 4. 실제 Sepolia 연동 실행

주의: 아래 모드는 **실제 네트워크 트랜잭션**을 발생시킵니다(가스비 필요).

### 4-1) 필수 값 준비

- `SEPOLIA_RPC_URL`: 예) Alchemy/Infura Sepolia HTTPS URL
- `SEPOLIA_TOKEN_ADDRESS`: Sepolia ERC20 토큰 컨트랙트 주소
- `WALLET_ENCRYPTION_KEY`: 사용자 wallet private key 암복호화용 Base64 AES 키

키 생성 예시:

```bash
openssl rand -base64 32
```

### 4-2) 실행

```bash
cd /home/ssafy/crypto-pjt/backend
export SEPOLIA_RPC_URL=<sepolia_rpc_url>
export SEPOLIA_TOKEN_ADDRESS=<erc20_token_address>
export WALLET_ENCRYPTION_KEY=<base64_aes_key>
mvn spring-boot:run -Dspring-boot.run.arguments="--app.chain.mode=sepolia"
```

`app.chain.mode=sepolia`일 때는 `SepoliaErc20Gateway`가 활성화되어 실제 송금/영수증 조회를 수행합니다.

중요:

- Sepolia 모드에서는 각 사용자 wallet private key로 서명합니다.
- 응답의 `senderAddress`는 해당 사용자의 wallet address입니다.
- 사용자는 private key를 직접 알 필요가 없습니다.

## 5. 자주 막히는 지점

- `Idempotency-Key is required`
  - 송금 생성 API에 `Idempotency-Key` 헤더가 빠졌습니다.
- `POLICY_BLOCKED / COOLDOWN_ACTIVE`
  - 수신자를 방금 수정한 경우 24시간 쿨다운입니다. (테스트 1분 설정)
- `RECIPIENT_NOT_ALLOWED`
  - 수신자 `allowed`가 `true`인지 확인하세요.
- Sepolia 모드에서 시작 직후 에러
  - `SEPOLIA_RPC_URL`, `SEPOLIA_TOKEN_ADDRESS`, `WALLET_ENCRYPTION_KEY` 3개가 비어 있지 않은지 확인하세요.
- `WALLET_NOT_FOUND`
  - 먼저 `POST /api/v1/remittance/wallets/me`로 wallet을 생성하세요.

## 6. 주요 API 목록

- `POST /api/v1/remittance/demo/seed`
- `POST /api/v1/remittance/wallets/me`
- `GET /api/v1/remittance/wallets/me`
- `GET /api/v1/remittance/recipients`
- `PUT /api/v1/remittance/recipients/{recipientId}`
- `POST /api/v1/remittance/transfers/precheck`
- `POST /api/v1/remittance/transfers` (`Idempotency-Key` 헤더 필수)
- `GET /api/v1/remittance/transfers/{transferId}`
- `GET /api/v1/remittance/transfers/{transferId}/integrity-hash`
- `POST /api/v1/remittance/transfers/{transferId}/receipt-link`

추가 상세 가이드는 아래 문서 참고:

- `pjt/backend-beginner-guide.md`
- `pjt/wallet-architecture-guide.md`
- `pjt/hash-spec.md`
