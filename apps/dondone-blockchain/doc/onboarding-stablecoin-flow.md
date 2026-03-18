# Onboarding ETH + Stablecoin Flow

## 목적

새 사용자가 wallet을 만든 직후 바로 Sepolia 송금을 시도할 수 있게,
서비스가 onboarding 단계에서 가스용 ETH와 송금용 stablecoin을 자동 지급한다.

동시에 실제 송금 요청은 임의 코인이 아니라 `설정된 stablecoin`만 허용한다.

## 지금 바뀐 동작

### 1. wallet 생성 직후 자동 지급

`POST /api/v1/remittance/wallets/me`

이 API가 처음 호출되어 새 wallet이 생성되면,
서버는 DB 저장이 끝난 뒤 `AFTER_COMMIT` 이벤트로 onboarding funding을 실행한다.

순서:

1. 사용자 wallet 생성
2. `test.user_wallets` 저장
3. `WalletCreatedEvent` 발행
4. `WalletOnboardingFundingService`가 이벤트 수신
5. treasury 지갑이 새 wallet에
   - Sepolia ETH
   - stablecoin
   을 전송

관련 코드:

- [UserWalletService.java](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/service/UserWalletService.java)
- [WalletCreatedEvent.java](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/service/WalletCreatedEvent.java)
- [WalletOnboardingFundingService.java](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/service/WalletOnboardingFundingService.java)

### 2. 실제 송금은 stablecoin만 허용

이제 송금 `precheck`와 `createTransfer`에서 요청의 `asset` 값이
설정된 stablecoin symbol과 같아야 한다.

예:

- 허용: `dUSDC`
- 거절: `ETH`, `BTC`, `USDT`

다르면 아래 에러가 반환된다.

```json
{
  "error": {
    "code": "UNSUPPORTED_ASSET",
    "message": "Only the configured stablecoin can be transferred",
    "details": {
      "supportedAsset": "dUSDC"
    }
  }
}
```

관련 코드:

- [TransferService.java](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/service/TransferService.java)
- [TransferJobWorker.java](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/job/TransferJobWorker.java)

## 설정값

설정 위치:

- [application.yml](/home/ssafy/S14P21C202/apps/dondone-blockchain/backend/src/main/resources/application.yml)

핵심 값:

- `SEPOLIA_RPC_URL`
- `SEPOLIA_TOKEN_ADDRESS`
- `SEPOLIA_STABLECOIN_SYMBOL`
- `SEPOLIA_FUNDING_PRIVATE_KEY`
- `WALLET_ENCRYPTION_KEY`
- `SEPOLIA_ONBOARDING_ENABLED`
- `SEPOLIA_ONBOARDING_ETH_AMOUNT_WEI`
- `SEPOLIA_ONBOARDING_STABLECOIN_AMOUNT_ATOMIC`

기본 onboarding 지급값:

- ETH: `10000000000000000 wei` = `0.01 ETH`
- stablecoin: `200000000 atomic`

## 실제 전달 방식

### ETH

treasury private key로 일반 ETH transfer 트랜잭션을 보낸다.

즉:

- `to = 새 사용자 wallet`
- `value = onboarding ETH amount`

이 ETH는 이후 사용자가 ERC20 stablecoin 전송 시 가스비로 사용된다.

### stablecoin

같은 treasury 지갑이 stablecoin contract의 `transfer(address,uint256)`를 호출한다.

즉:

- `to = stablecoin contract address`
- `data = transfer(newWallet, stablecoinAmountAtomic)`
- `value = 0`

새 wallet은 이 stablecoin 잔액으로 실제 송금을 시작할 수 있다.

## 왜 둘 다 필요한가

ERC20 송금에는 2가지가 필요하다.

1. **토큰 잔액**
2. **가스비 ETH**

토큰만 있고 ETH가 없으면 송금이 안 된다.
ETH만 있고 토큰이 없으면 보낼 자산이 없다.

그래서 onboarding은 `ETH + stablecoin`을 같이 지급한다.

## 주의할 점

1. onboarding funding은 treasury 잔액이 있어야 한다.
2. wallet 생성 성공과 funding 성공은 완전히 같은 트랜잭션이 아니다.
3. 현재 구현은 funding 실패 시 wallet 생성 자체를 되돌리지는 않는다.
4. 따라서 운영에서는 funding 실패 로그를 모니터링해야 한다.

## 확인 방법

1. `test.users`에 새 사용자 1명 추가
2. `POST /wallets/me`
3. 응답 wallet address 확보
4. `cast balance <wallet>`로 ETH 확인
5. `cast call <token> "balanceOf(address)(uint256)" <wallet>`로 stablecoin 확인

## 이번에 실제 확인한 결과

실제 Sepolia에서 새 user3 wallet 생성 직후 자동으로 아래가 들어왔다.

- ETH: `0.01`
- stablecoin: `200000000`

즉 onboarding 자동 지급은 동작한다.
