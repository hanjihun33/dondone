## WorkProof Pay - Blockchain (Foundry)

PRD(`pjt/reference.md`)의 P0 범위 중 블록체인 파트를 Foundry 프로젝트로 구성했다.

### 포함된 온체인 기능
- SafePay 정책
- 수신자 허용 목록(allowlist)
- 수신자 변경 후 24시간 쿨다운
- 고액 송금 추가 확인 정책
- 중복 방지 키(idempotency key) 기반 재요청 처리
- 송금 상태 머신: `REQUESTED / SUBMITTED / CONFIRMED / FAILED`
- 송금 영수증 데이터 해시 생성(`getTransferReceiptHash`)
- 문서 무결성 해시 등록/검증(`DocumentHashRegistry`)

### 디렉토리
- `src/SafePayRemittance.sol`: 송금 정책 및 상태 관리 컨트랙트
- `src/DemoStableToken.sol`: 테스트넷 데모용 ERC20 토큰
- `src/DocumentHashRegistry.sol`: Proof Pack/Claim Kit 해시 등록·검증 컨트랙트
- `test/SafePayRemittance.t.sol`: SafePay 단위 테스트
- `test/DocumentHashRegistry.t.sol`: 문서 무결성 단위 테스트
- `script/DeploySafePay.s.sol`: 배포 스크립트
- `pjt/api-for-web-android.md`: 웹/안드로이드 연동 API 문서

### 실행
```bash
forge build
forge test -vv
```

### 로컬 배포 예시
```bash
anvil

export PRIVATE_KEY=<anvil_first_private_key>
export TREASURY=<treasury_wallet_address>
export HIGH_AMOUNT_THRESHOLD=300000000
export MINT_AMOUNT=1000000000000

forge script script/DeploySafePay.s.sol:DeploySafePayScript \
  --rpc-url http://127.0.0.1:8545 \
  --broadcast
```

### 컨트랙트 사용 흐름
1. 사용자 지갑이 `setRecipientAllow`로 송금 대상 등록
2. 24시간 이후 `requestTransfer` 호출(멱등키 포함)
3. 오퍼레이터가 `markSubmitted`
4. 오퍼레이터가 `markConfirmed` 또는 `markFailed`
5. `getTransferReceiptHash`로 영수증 생성용 해시 조회
6. `DocumentHashRegistry.registerProof`로 문서 해시 등록 후 `verifyProof` 검증

### 백엔드 API (신규)
- `backend/`: Spring Boot 기반 Remittance API 서버
- API 범위: 허용목록 조회/수정, precheck, 송금 생성/조회, receipt-link 발급
- 비동기 잡: `SUBMIT_TRANSFER`, `POLL_TRANSFER_RECEIPT`, `RENDER_TRANSFER_RECEIPT`
- 사용자별 Ethereum wallet 생성/조회 API 포함
- private key는 암호화 저장 후, 송금 시 해당 사용자 wallet으로 서명
- 초보자 실행 가이드: `pjt/backend-beginner-guide.md`
