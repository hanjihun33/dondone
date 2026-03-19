# Sepolia Live Transfer Test Log

## 목적

사용자 2명을 PostgreSQL `test.users`에 준비한 뒤,
각 사용자 wallet을 생성하고 실제 Sepolia에서 ERC20 전송이 되는지 검증한다.

## 사용한 환경

- App server: Spring Boot remittance backend
- DB: PostgreSQL `localhost:5433/dondone`, schema `test`
- Chain: Ethereum Sepolia
- RPC: `.env`의 `SEPOLIA_RPC_URL`
- Token: `DemoStableToken`
  - address: `0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f`
- Treasury / deployer address:
  - `0xB27a722C0AFeA067d7Cc13DB60F90309cE9A4f16`

## 이번 테스트에서 확인한 것

1. `test.users`에 사용자 1, 2를 넣을 수 있다.
2. `POST /wallets/me`로 사용자별 wallet이 생성된다.
3. 새 wallet은 기본적으로 자금이 없어서 바로 송금은 불가능하다.
4. 이번 실행에서는 faucet이 필요하지 않았다.
5. 이유는 deployer 지갑이 이미 Sepolia ETH와 `DemoStableToken` 잔액을 가지고 있었기 때문이다.
6. deployer가 각 사용자 wallet에 ETH와 토큰을 보내준 뒤 실제 송금이 성공했다.

## 실제 진행 순서

### 1. PostgreSQL에 임시 사용자 2명 생성

- `test.users`
  - `id=1`, `demo1@example.com`
  - `id=2`, `demo2@example.com`

### 2. Sepolia 모드로 서버 실행

필수 환경변수:

- `SEPOLIA_RPC_URL`
- `SEPOLIA_TOKEN_ADDRESS=0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f`
- `WALLET_ENCRYPTION_KEY`

실행 예시:

```bash
set -a
source /home/ssafy/S14P21C202/apps/dondone-blockchain/.env
export SEPOLIA_TOKEN_ADDRESS=0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f
cd /home/ssafy/S14P21C202/apps/dondone-blockchain/backend
mvn spring-boot:run -Dspring-boot.run.arguments="--app.chain.mode=sepolia"
```

### 3. 사용자 wallet 생성

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/wallets/me' -H 'X-User-Id: 1'
curl -s -X POST 'http://localhost:8080/api/v1/remittance/wallets/me' -H 'X-User-Id: 2'
```

생성된 wallet:

- user 1: `0x4abc4054dff41604e755444555c629f74a4e02c6`
- user 2: `0xf5ddbd3f46f208f73ba242b74bf49c29a35a9d26`

### 4. deployer에서 두 wallet로 ETH + 토큰 지급

이번 테스트에서는 faucet 대신 deployer를 사용했다.

```bash
set -a
source /home/ssafy/S14P21C202/apps/dondone-blockchain/.env

cast send 0x4abc4054dff41604e755444555c629f74a4e02c6 \
  --value 0.01ether \
  --private-key "$PRIVATE_KEY" \
  --rpc-url "$SEPOLIA_RPC_URL"

cast send 0xf5ddbd3f46f208f73ba242b74bf49c29a35a9d26 \
  --value 0.01ether \
  --private-key "$PRIVATE_KEY" \
  --rpc-url "$SEPOLIA_RPC_URL"

cast send 0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f \
  "transfer(address,uint256)" \
  0x4abc4054dff41604e755444555c629f74a4e02c6 200000000 \
  --private-key "$PRIVATE_KEY" \
  --rpc-url "$SEPOLIA_RPC_URL"

cast send 0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f \
  "transfer(address,uint256)" \
  0xf5ddbd3f46f208f73ba242b74bf49c29a35a9d26 200000000 \
  --private-key "$PRIVATE_KEY" \
  --rpc-url "$SEPOLIA_RPC_URL"
```

지급 후 잔액:

- user 1 ETH: `0.01`
- user 1 dUSDC: `200000000`
- user 2 ETH: `0.01`
- user 2 dUSDC: `200000000`

### 5. recipient 등록

```bash
curl -s -X PUT 'http://localhost:8080/api/v1/remittance/recipients/rcp_user2' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -d '{"alias":"User2","walletAddress":"0xf5ddbd3f46f208f73ba242b74bf49c29a35a9d26","relation":"friend","allowed":true}'

curl -s -X PUT 'http://localhost:8080/api/v1/remittance/recipients/rcp_user1' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 2' \
  -d '{"alias":"User1","walletAddress":"0x4abc4054dff41604e755444555c629f74a4e02c6","relation":"friend","allowed":true}'
```

### 6. 테스트 중 확인한 정책 이슈

신규 recipient는 즉시 24시간 cooldown에 걸린다.
그래서 바로 송금 테스트를 하려면 `updated_at`을 과거로 돌리거나, 테스트용 설정을 따로 둬야 한다.

이번 테스트에서는 DB에서 `updated_at = now() - interval '2 day'`로 조정했다.

### 7. 실제 송금 실행

#### user 1 -> user 2

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -H 'Idempotency-Key: u1-to-u2-001' \
  -d '{"recipientId":"rcp_user2","asset":"dUSDC","amount":50000,"highAmountConfirmed":false}'
```

결과:

- transferId: `tr_1773801229309_dd1c4d32`
- txHash: `0x75e45587e54100fddd860b406214f9c59923b2f2f2378a3ff530a0f18471121b`
- status: `CONFIRMED`

#### user 2 -> user 1

```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 2' \
  -H 'Idempotency-Key: u2-to-u1-001' \
  -d '{"recipientId":"rcp_user1","asset":"dUSDC","amount":25000,"highAmountConfirmed":false}'
```

결과:

- transferId: `tr_1773801243475_45e4c021`
- txHash: `0xacbb8f97eb633761731bb1800d539f5f91548d14c6e96f8926b3b48a838cdbd4`
- status: `CONFIRMED`

### 8. 최종 잔액

- user 1 dUSDC: `199975000`
- user 2 dUSDC: `200025000`

계산:

- user 1은 `50000` 송금, `25000` 수신 -> `-25000`
- user 2는 `25000` 송금, `50000` 수신 -> `+25000`

## faucet이 필요한가?

### 이번 실행

- 필요 없었음
- 이유: deployer wallet이 이미 ETH와 token을 보유하고 있었음

### 일반적인 경우

- **ETH 가스비**가 없으면 faucet 필요
- 하지만 **토큰 잔액**은 faucet으로 해결되지 않을 수 있음
- 이 프로젝트의 토큰은 `DemoStableToken`이라, 실제 송금 테스트에는 아래 2개가 모두 필요하다.
  - Sepolia ETH
  - `DemoStableToken` 잔액

즉 faucet은 ETH 문제만 해결하고, 토큰은 treasury 또는 token owner가 따로 보내줘야 한다.

## 이번에 실제로 막혔던 구간

1. `.env`를 `source`만 하고 실행하면 `WALLET_ENCRYPTION_KEY`가 export되지 않아 서버가 시작 실패
2. recipient를 방금 등록하면 cooldown 때문에 바로 송금 불가
3. `high-amount-threshold`가 낮아서 큰 금액은 `HIGH_AMOUNT_CONFIRM_REQUIRED`에 걸림

## 결론

- 사용자 2명 wallet 생성: 성공
- deployer에서 ETH + token 지급: 성공
- user1 -> user2 실제 Sepolia ERC20 전송: 성공
- user2 -> user1 실제 Sepolia ERC20 전송: 성공
- faucet은 이번 실행에서는 불필요
- 실제 병목은 faucet보다도 `recipient cooldown`, `token funding`, `.env export`였다
