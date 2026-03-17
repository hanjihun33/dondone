# WorkProof Pay 초보자 따라치기 가이드 (Foundry)

이 문서는 블록체인을 처음 만지는 사람도 `복사 -> 붙여넣기`만으로 아래를 끝낼 수 있게 작성했다.

- 로컬 블록체인(Anvil) 실행
- 컨트랙트 배포
- 허용목록 등록
- 송금 요청/상태 전이
- 영수증 해시 조회
- 문서 해시 등록/검증

기준 프로젝트 경로:
- `/home/yigd/crypto-pjt`

---

## 0. 먼저 알아둘 것

- 이 프로젝트는 **데모/테스트용**이다.
- 아래 private key는 **로컬 Anvil에서만** 써야 한다.
- 실제 메인넷/실서비스 키를 절대 넣지 않는다.

---

## 1. 필요한 프로그램 설치 확인

터미널에서 아래 3개가 실행되면 준비 완료다.

```bash
forge --version
anvil --version
cast --version
```

### 1.1 설치가 안 되어 있으면

Foundry 설치:

```bash
curl -L https://foundry.paradigm.xyz | bash
source ~/.bashrc
foundryup
```

다시 확인:

```bash
forge --version
anvil --version
cast --version
```

---

## 2. 프로젝트 이동 + 빌드/테스트

```bash
cd /home/yigd/crypto-pjt
forge build
forge test -vv
```

정상이라면 마지막에 `passed`가 보인다.

---

## 3. 터미널 2개 열기

- 터미널 A: 로컬 체인(anvil) 실행
- 터미널 B: 배포/호출 명령 실행

---

## 4. 터미널 A에서 Anvil 실행

```bash
anvil
```

실행 후 화면에 계정과 private key 목록이 나온다.

예시(Anvil 기본 1번 계정):
- Address: `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266`
- Private Key: `0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80`

이 창은 계속 켜둔다.

---

## 5. 터미널 B에서 환경변수 설정

터미널 B에서 아래를 한 번에 붙여넣는다.

```bash
cd /home/yigd/crypto-pjt

export RPC_URL=http://127.0.0.1:8545
export CHAIN_ID=31337

# anvil 기본 첫 번째 계정
export DEPLOYER_PK=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
export DEPLOYER_ADDR=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266

# 완전 초보자 모드: sender/operator는 deployer와 동일하게 사용
export SENDER_PK=$DEPLOYER_PK
export SENDER_ADDR=$DEPLOYER_ADDR
export OPERATOR_PK=$DEPLOYER_PK
export OPERATOR_ADDR=$DEPLOYER_ADDR

# 수신자 주소(키 불필요): anvil 기본 두 번째 계정 주소 예시
export RECIPIENT_ADDR=0x70997970C51812dc3A010C7d01b50e0d17dc79C8

# SafePay 정책값
export HIGH_AMOUNT_THRESHOLD=300000000
export MINT_AMOUNT=1000000000000
```

참고:
- 처음에는 `deployer = sender = operator`로 진행하는 것이 가장 덜 헷갈린다.
- 익숙해지면 sender/operator를 다른 계정으로 분리하면 된다.

---

## 6. 배포

```bash
export PRIVATE_KEY=$DEPLOYER_PK
export TREASURY=$SENDER_ADDR

forge script script/DeploySafePay.s.sol:DeploySafePayScript \
  --rpc-url $RPC_URL \
  --broadcast
```

출력에서 아래 3개 주소를 찾아 복사한다.
- `DemoStableToken:`
- `SafePayRemittance:`
- `DocumentHashRegistry:`

복사한 값을 환경변수로 넣는다.

```bash
export TOKEN=<복사한_DemoStableToken_주소>
export SAFEPAY=<복사한_SafePayRemittance_주소>
export DOCREG=<복사한_DocumentHashRegistry_주소>
```

정상 연결 확인:

```bash
cast code $TOKEN --rpc-url $RPC_URL
cast code $SAFEPAY --rpc-url $RPC_URL
cast code $DOCREG --rpc-url $RPC_URL
```

결과가 `0x`만 나오지 않고 긴 바이트코드면 정상 배포다.

---

## 7. 송금 플로우 따라하기 (핵심)

아래 순서를 **반드시 순서대로** 실행한다.

### 7.1 오퍼레이터 등록 (deployer가 owner)

```bash
cast send $SAFEPAY \
  "setOperator(address,bool)" $OPERATOR_ADDR true \
  --private-key $DEPLOYER_PK \
  --rpc-url $RPC_URL
```

확인:

```bash
cast call $SAFEPAY "operators(address)(bool)" $OPERATOR_ADDR --rpc-url $RPC_URL
```

`true`가 나오면 성공.

### 7.2 sender 잔액 확인

```bash
cast call $TOKEN "balanceOf(address)(uint256)" $SENDER_ADDR --rpc-url $RPC_URL
```

값이 0보다 크면 된다.

### 7.3 sender가 SafePay에 approve

```bash
cast send $TOKEN \
  "approve(address,uint256)" $SAFEPAY 115792089237316195423570985008687907853269984665640564039457584007913129639935 \
  --private-key $SENDER_PK \
  --rpc-url $RPC_URL
```

allowance 확인:

```bash
cast call $TOKEN "allowance(address,address)(uint256)" $SENDER_ADDR $SAFEPAY --rpc-url $RPC_URL
```

### 7.4 sender가 recipient 허용목록 등록

```bash
cast send $SAFEPAY \
  "setRecipientAllow(address,bool)" $RECIPIENT_ADDR true \
  --private-key $SENDER_PK \
  --rpc-url $RPC_URL
```

정책 조회:

```bash
cast call $SAFEPAY \
  "getPolicy(address,address)((bool,uint64),uint256)" $SENDER_ADDR $RECIPIENT_ADDR \
  --rpc-url $RPC_URL
```

### 7.5 쿨다운 24시간 우회 (로컬 데모용)

실제론 24시간 기다려야 한다. 데모에서는 시간을 앞으로 보낸다.

```bash
cast rpc evm_increaseTime 86401 --rpc-url $RPC_URL
cast rpc evm_mine --rpc-url $RPC_URL
```

### 7.6 sender가 송금 요청 생성

- 금액: `100000000` (6 decimals 기준 100 토큰)
- idempotency key: `idem-001`

```bash
cast send $SAFEPAY \
  "requestTransfer(address,uint256,string,bool)" $RECIPIENT_ADDR 100000000 "idem-001" false \
  --private-key $SENDER_PK \
  --rpc-url $RPC_URL
```

위 트랜잭션 로그에서 `TransferRequested` 이벤트가 찍힌다.

`requestId`를 빠르게 확인하려면(첫 요청이면 보통 1):

```bash
cast call $SAFEPAY "nextRequestId()(uint256)" --rpc-url $RPC_URL
```

현재 다음 ID이므로, 실제 requestId는 `nextRequestId - 1`.

### 7.7 오퍼레이터가 SUBMITTED 처리

예시 txHash 값(32 bytes):
- `0x1111111111111111111111111111111111111111111111111111111111111111`

```bash
cast send $SAFEPAY \
  "markSubmitted(uint256,bytes32)" 1 0x1111111111111111111111111111111111111111111111111111111111111111 \
  --private-key $OPERATOR_PK \
  --rpc-url $RPC_URL
```

### 7.8 오퍼레이터가 CONFIRMED 처리

```bash
cast send $SAFEPAY \
  "markConfirmed(uint256,bytes32)" 1 0x2222222222222222222222222222222222222222222222222222222222222222 \
  --private-key $OPERATOR_PK \
  --rpc-url $RPC_URL
```

### 7.9 상태/잔액 확인

```bash
cast call $SAFEPAY \
  "getTransferRequest(uint256)((uint256,address,address,uint256,bytes32,uint64,uint64,uint8,bytes32,string))" 1 \
  --rpc-url $RPC_URL

cast call $TOKEN "balanceOf(address)(uint256)" $SENDER_ADDR --rpc-url $RPC_URL
cast call $TOKEN "balanceOf(address)(uint256)" $RECIPIENT_ADDR --rpc-url $RPC_URL
```

`status` enum 값:
- `1 = REQUESTED`
- `2 = SUBMITTED`
- `3 = CONFIRMED`
- `4 = FAILED`

### 7.10 영수증용 해시 조회

```bash
cast call $SAFEPAY "getTransferReceiptHash(uint256)(bytes32)" 1 --rpc-url $RPC_URL
```

이 해시를 백엔드에서 PDF 영수증 메타와 연결하면 된다.

---

## 8. 문서 해시 등록/검증 따라하기

`DocumentHashRegistry`는 Proof Pack/Claim Kit 해시 검증용이다.

### 8.1 오퍼레이터 등록 (문서 레지스트리)

```bash
cast send $DOCREG \
  "setOperator(address,bool)" $OPERATOR_ADDR true \
  --private-key $DEPLOYER_PK \
  --rpc-url $RPC_URL
```

### 8.2 proofId/payloadHash 준비

```bash
export PROOF_ID=$(cast keccak "proof-pack-2026-03-09-001")
export PAYLOAD_HASH=$(cast keccak "normalized-json-payload-v1")

echo $PROOF_ID
echo $PAYLOAD_HASH
```

### 8.3 해시 등록

```bash
cast send $DOCREG \
  "registerProof(bytes32,bytes32,string,string,uint32)" \
  $PROOF_ID $PAYLOAD_HASH "PROOF_PACK" "pp_20260309_001" 4 \
  --private-key $OPERATOR_PK \
  --rpc-url $RPC_URL
```

### 8.4 검증 (OK)

```bash
cast call $DOCREG "verifyProof(bytes32,bytes32)(bool)" $PROOF_ID $PAYLOAD_HASH --rpc-url $RPC_URL
```

`true`면 OK.

### 8.5 검증 (FAIL)

```bash
export WRONG_HASH=$(cast keccak "tampered-payload")
cast call $DOCREG "verifyProof(bytes32,bytes32)(bool)" $PROOF_ID $WRONG_HASH --rpc-url $RPC_URL
```

`false`면 FAIL.

### 8.6 저장된 상세 정보 조회

```bash
cast call $DOCREG \
  "getProof(bytes32)((bytes32,uint64,uint32,address,string,string))" $PROOF_ID \
  --rpc-url $RPC_URL
```

---

## 9. 실패 케이스 빠르게 재현하기

### 9.1 허용목록 위반

```bash
cast send $SAFEPAY \
  "requestTransfer(address,uint256,string,bool)" 0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65 1000000 "idem-blocked" false \
  --private-key $SENDER_PK \
  --rpc-url $RPC_URL
```

`PolicyBlocked("RECIPIENT_NOT_ALLOWED")`류로 실패한다.

### 9.2 고액 확인 누락

```bash
cast send $SAFEPAY \
  "requestTransfer(address,uint256,string,bool)" $RECIPIENT_ADDR 400000000 "idem-high" false \
  --private-key $SENDER_PK \
  --rpc-url $RPC_URL
```

`HIGH_AMOUNT_CONFIRM_REQUIRED`로 실패한다.

### 9.3 상태 전이 위반 (SUBMITTED 없이 CONFIRMED)

새 요청을 만든 뒤 바로 `markConfirmed` 하면 실패한다.

---

## 10. 자주 나는 오류와 해결

### 10.1 `connection refused`
원인:
- anvil이 꺼져 있음

해결:
- 터미널 A에서 `anvil` 다시 실행

### 10.2 `NotOperator()`
원인:
- 해당 주소가 operator 등록 안 됨

해결:
- owner 키로 `setOperator(address,true)` 호출

### 10.3 `InvalidStatusTransition`
원인:
- 상태 순서 위반

해결:
- `REQUESTED -> SUBMITTED -> CONFIRMED/FAILED` 순서 준수

### 10.4 `TokenTransferFailed`
원인:
- sender 잔액 부족 또는 allowance 부족

해결:
- `balanceOf`, `allowance` 확인 후 `approve` 재실행

### 10.5 `COOLDOWN_ACTIVE`
원인:
- 허용목록 등록 직후 24시간 미경과

해결:
- 실제 대기 또는 로컬에서 `evm_increaseTime` 사용

---

## 11. 전체를 한 번에 검증하는 최소 체크리스트

1. `forge test -vv`가 전부 통과한다.
2. 로컬 배포 후 `cast code` 3개 주소가 모두 코드 길이 > 0이다.
3. `approve -> allow -> request -> submitted -> confirmed`가 순서대로 성공한다.
4. sender 잔액은 줄고 recipient 잔액은 늘어난다.
5. `getTransferReceiptHash`가 0이 아닌 값을 반환한다.
6. `registerProof` 후 `verifyProof`가 true를 반환한다.
7. 잘못된 payload hash로 `verifyProof` 호출 시 false가 나온다.

---

## 12. 파일 위치 요약

- 컨트랙트
  - `src/DemoStableToken.sol`
  - `src/SafePayRemittance.sol`
  - `src/DocumentHashRegistry.sol`
- 테스트
  - `test/SafePayRemittance.t.sol`
  - `test/DocumentHashRegistry.t.sol`
- 배포
  - `script/DeploySafePay.s.sol`
- 문서
  - `pjt/api-for-web-android.md`
  - `pjt/foundry-usage-guide.md`
  - `pjt/foundry-beginner-guide.md` (이 문서)
