# WorkProof Pay 블록체인(Foundry) 사용/실행 가이드

이 문서는 `/home/yigd/crypto-pjt` 프로젝트에서 Foundry 기반 컨트랙트를 로컬 및 테스트넷에서 실행하는 절차를 정리한다.

## 1. 구성 파일
- `src/DemoStableToken.sol`: 데모용 ERC20 토큰(6 decimals)
- `src/SafePayRemittance.sol`: SafePay 정책/송금 상태 관리 컨트랙트
- `test/SafePayRemittance.t.sol`: 정책/상태 전이 테스트
- `script/DeploySafePay.s.sol`: 배포 스크립트
- `pjt/api-for-web-android.md`: 모바일/웹팀 API 연동 문서

## 2. 사전 준비

### 2.1 Foundry 설치 확인
```bash
forge --version
anvil --version
cast --version
```

### 2.2 프로젝트 루트 이동
```bash
cd /home/yigd/crypto-pjt
```

## 3. 기본 개발 명령

### 3.1 빌드
```bash
forge build
```

### 3.2 테스트
```bash
forge test -vv
```

### 3.3 포맷
```bash
forge fmt
```

## 4. 로컬 체인(Anvil) 실행 및 배포

### 4.1 Anvil 실행
터미널 A:
```bash
anvil
```

### 4.2 환경변수 설정
터미널 B:
```bash
cd /home/yigd/crypto-pjt

export PRIVATE_KEY=<anvil_첫번째_계정_private_key>
export TREASURY=<토큰_민팅받을_지갑주소>
export HIGH_AMOUNT_THRESHOLD=300000000
export MINT_AMOUNT=1000000000000
```

- `HIGH_AMOUNT_THRESHOLD=300000000`는 6 decimals 기준 300 토큰
- `MINT_AMOUNT=1000000000000`는 1,000,000 토큰

### 4.3 배포 실행
```bash
forge script script/DeploySafePay.s.sol:DeploySafePayScript \
  --rpc-url http://127.0.0.1:8545 \
  --broadcast
```

배포 후 콘솔에 아래 주소가 출력된다.
- `DemoStableToken`
- `SafePayRemittance`
- `Treasury`

## 5. Sepolia 테스트넷 배포

### 5.1 환경변수 설정
```bash
cd /home/yigd/crypto-pjt

export PRIVATE_KEY=<sepolia_deployer_private_key>
export TREASURY=<토큰_민팅받을_지갑주소>
export HIGH_AMOUNT_THRESHOLD=300000000
export MINT_AMOUNT=1000000000000
export SEPOLIA_RPC_URL=<rpc_url>
```

```bash
export PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
export TREASURY=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
export HIGH_AMOUNT_THRESHOLD=300000000
export MINT_AMOUNT=1000000000000
export SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/AUCoxKAhWwKp4z9LwiD4P
```

### 5.2 배포
```bash
forge script script/DeploySafePay.s.sol:DeploySafePayScript \
  --rpc-url $SEPOLIA_RPC_URL \
  --broadcast \
  --verify
```

주의:
- `--verify`는 Etherscan API 키 세팅이 되어 있어야 동작한다.
- 검증이 필요 없으면 `--verify`는 제거해도 된다.

## 6. 온체인 사용 플로우

### 6.1 사용자 준비
1. 사용자 지갑에 `DemoStableToken` 잔액 보유
2. 사용자 지갑이 `SafePayRemittance`에 `approve` 수행
3. 사용자 지갑이 `setRecipientAllow(recipient, true)` 수행
4. 24시간 쿨다운 경과 대기

### 6.2 송금 요청
- 사용자 지갑이 `requestTransfer(recipient, amount, idempotencyKey, highAmountConfirmed)` 호출
- 정책 위반 시 `PolicyBlocked(code)` revert

### 6.3 운영자 상태 전이
- 운영자(서버 지갑)가 `markSubmitted(requestId, txHash)`
- 성공 확정 시 `markConfirmed(requestId, txHash)`
- 실패 시 `markFailed(requestId, failureCode)`

## 7. cast 예시

### 7.1 수신자 허용 설정
```bash
cast send <SafePayRemittance주소> \
  "setRecipientAllow(address,bool)" <recipient> true \
  --private-key $PRIVATE_KEY \
  --rpc-url http://127.0.0.1:8545
```

### 7.2 송금 요청
```bash
cast send <SafePayRemittance주소> \
  "requestTransfer(address,uint256,string,bool)" <recipient> 50000000 "idem-001" true \
  --private-key $PRIVATE_KEY \
  --rpc-url http://127.0.0.1:8545
```

### 7.3 요청 상태 조회
```bash
cast call <SafePayRemittance주소> \
  "getTransferRequest(uint256)((uint256,address,address,uint256,bytes32,uint64,uint64,uint8,bytes32,string))" 1 \
  --rpc-url http://127.0.0.1:8545
```

## 8. 트러블슈팅

- `Undeclared identifier console2`:
  - `script/DeploySafePay.s.sol`에 `import { console2 } from "forge-std/console2.sol";` 필요
- `PolicyBlocked("COOLDOWN_ACTIVE")`:
  - 수신자 허용 직후 24시간 제한. 쿨다운 경과 후 재시도
- `PolicyBlocked("HIGH_AMOUNT_CONFIRM_REQUIRED")`:
  - 임계치 초과 금액은 `highAmountConfirmed=true`로 재요청
- `TokenTransferFailed`:
  - 사용자 잔액/allowance 부족 여부 확인

## 9. 백엔드 연동 포인트
- 앱/서버 API 규격은 `pjt/api-for-web-android.md`를 기준으로 사용
- 서버는 `idempotency key`를 DB unique key로 관리
- 서버 오퍼레이터 지갑은 `setOperator`로 등록한 주소를 사용
