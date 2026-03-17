# Wallet Architecture Guide

이 문서는 현재 백엔드의 wallet 구조를 처음 보는 사람도 바로 이해할 수 있게 설명한다.

## 1. 지금 구조 한 줄 요약

현재 서버는 **사용자별 Ethereum wallet을 자동 생성하고, private key를 암호화 저장한 뒤, 송금 시 해당 사용자 wallet으로 서명**한다.

즉, 구조는 아래와 같다.

1. 사용자가 회원가입 또는 최초 wallet 생성 API 호출
2. 서버가 Ethereum wallet 생성
3. wallet address는 DB 저장
4. private key는 암호화 저장
5. 송금 시 해당 사용자의 private key를 복호화해서 Web3j로 서명

사용자는 wallet과 private key의 존재를 몰라도 된다.

참고:

- 현재 예제 백엔드에서는 `POST /wallets/me` 호출 시 wallet을 만들 수 있다.
- 또한 첫 송금 요청 시 wallet이 없으면 서버가 자동 생성한다.

## 2. 현재 구현 포인트

### 2-1) wallet 생성

- 엔티티: `user_wallets`
- 저장값:
  - `user_id`
  - `wallet_address`
  - `encrypted_private_key`
  - `created_at`

관련 코드:

- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/domain/UserWalletEntity.java`
- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/service/UserWalletService.java`

### 2-2) private key 암호화

- 방식: AES/GCM
- 설정값: `WALLET_ENCRYPTION_KEY`
- 저장 시 암호화, 송금 시 복호화

관련 코드:

- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/service/WalletCryptoService.java`

### 2-3) 송금 서명

- 송금 요청 생성 시 사용자 wallet address를 `senderAddress`로 저장
- 실제 전송 시 사용자 private key를 복호화해서 Web3j로 서명

관련 코드:

- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/service/TransferService.java`
- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/job/TransferJobWorker.java`
- `/home/ssafy/crypto-pjt/backend/src/main/java/com/workproofpay/remittance/gateway/SepoliaErc20Gateway.java`

## 3. API 사용 흐름

### 3-1) wallet 생성

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: user_001'
```

### 3-2) wallet 조회

```bash
curl -s 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: user_001'
```

### 3-3) 송금

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user_001' \
  -H 'Idempotency-Key: idem-001' \
  -d '{
    "recipientId":"rcp_001",
    "amount":1000,
    "asset":"USDC",
    "highAmountConfirmed":false
  }'
```

이 요청이 들어오면 서버는 `user_001`의 wallet private key로 서명한다.

## 4. 운영 시 꼭 필요한 환경변수

### Demo 모드

- `WALLET_ENCRYPTION_KEY` 권장

### Sepolia 모드

- `SEPOLIA_RPC_URL`
- `SEPOLIA_TOKEN_ADDRESS`
- `WALLET_ENCRYPTION_KEY`

`WALLET_ENCRYPTION_KEY`는 Base64 인코딩된 AES 키여야 한다.

예시 형식:

```bash
export WALLET_ENCRYPTION_KEY=<base64_aes_key>
```

## 5. 지금 의미하는 것

현재 구조는 아래 요구사항을 만족한다.

- 사용자가 회원가입하면 서버가 wallet을 생성할 수 있다
- wallet address를 DB에 저장한다
- private key를 평문이 아니라 암호화 저장한다
- 송금 요청 시 해당 사용자 wallet private key로 서명한다
- 사용자는 wallet/private key를 몰라도 된다
- Java + Web3j 기반이다

## 6. 아직 남아 있는 운영 과제

현재 구현은 기능 요구사항은 맞추지만, 운영 품질 관점에서 아래는 후속 과제다.

- HSM/KMS/MPC 연동
- private key 접근 감사 로그
- 출금 한도와 이상거래 탐지
- wallet 백업/복구 정책
- 실제 회원가입 시스템과 wallet 자동 생성 연결

## 7. 발표/문서용 추천 문장

짧은 문장:

> 현재 시스템은 사용자별 Ethereum wallet을 서버가 자동 생성하고 관리하는 custodial wallet 구조이며, 송금 시 각 사용자 wallet private key로 서명한다.

설명형 문장:

> 사용자는 앱에서 일반 송금처럼 요청만 보내고, 서버는 사용자별로 생성된 내부 wallet의 private key를 암호화 저장했다가 송금 시 복호화하여 Web3j로 트랜잭션을 서명한다.
