# Remittance API 초보자 실행 가이드 (Spring Boot)

이 문서는 `backend/` 모듈을 로컬에서 실행하고, API를 직접 호출해 송금 플로우를 확인하는 방법을 설명한다.

먼저 현재 구조를 한 줄로 정리하면:

- 서버가 사용자별 wallet을 자동 생성한다.
- wallet address는 DB에 저장한다.
- private key는 암호화해서 저장한다.
- 송금 시 해당 사용자 wallet private key로 서명한다.

## 1. 위치
- 백엔드 루트: `/home/ssafy/S14P21C202/apps/dondone-blockchain/backend`

## 2. 선행 설치

### 2.1 Java 17 확인
```bash
java -version
```

### 2.2 Maven 확인
```bash
mvn -version
```

`mvn: command not found`면 설치:
```bash
sudo apt-get update
sudo apt-get install -y maven
```

## 3. 실행

```bash
cd /home/ssafy/S14P21C202/apps/dondone-blockchain/backend
mvn spring-boot:run
```

기본 포트: `8080`

헬스 체크:
```bash
curl -i http://localhost:8080/api/v1/remittance/recipients
```

## 4. 기본 헤더

아래 헤더를 붙여 호출하면 된다.

- `X-User-Id`: 사용자 식별(없으면 `demo-user`)
- `Idempotency-Key`: 송금 생성 API 필수

## 5. API 따라하기

### 5.0 시드 데이터 넣기
```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/demo/seed' | jq
```

설명:

- 정상 데모 케이스 1개를 DB에 넣는다.
- 파일 원본은 `backend/src/main/resources/demo/remittance-seed-normal-case.json` 이다.

### 5.0 내 wallet 생성
```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: 1' | jq
```

설명:

- 이 API를 호출하면 `1` 전용 wallet이 생성된다.
- 호출하지 않아도 첫 송금 시 자동 생성되지만, 가이드에서는 흐름을 눈으로 확인하기 위해 먼저 만든다.

### 5.0-1 내 wallet 조회
```bash
curl -s 'http://localhost:8080/api/v1/remittance/wallets/me' \
  -H 'X-User-Id: 1' | jq
```

### 5.1 수신자 등록(허용목록)
```bash
curl -s -X PUT 'http://localhost:8080/api/v1/remittance/recipients/rcp_001' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -d '{
    "alias":"Mom",
    "walletAddress":"0x7F0000000000000000000000000000000000A1",
    "relation":"FAMILY",
    "allowed":true
  }' | jq
```

### 5.2 수신자 조회
```bash
curl -s 'http://localhost:8080/api/v1/remittance/recipients' \
  -H 'X-User-Id: 1' | jq
```

### 5.3 송금 사전 점검(precheck)
```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers/precheck' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -d '{
    "recipientId":"rcp_001",
    "amount":50000,
    "asset":"dUSDC",
    "highAmountConfirmed":false
  }' | jq
```

### 5.4 송금 생성
```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -H 'Idempotency-Key: idem-001' \
  -d '{
    "recipientId":"rcp_001",
    "amount":50000,
    "asset":"dUSDC",
    "highAmountConfirmed":true,
    "memo":"March support"
  }' | tee /tmp/create-transfer.json | jq
```

`transferId` 추출:
```bash
TRANSFER_ID=$(jq -r '.transferId' /tmp/create-transfer.json)
echo $TRANSFER_ID
```

### 5.5 송금 상태 조회(폴링)
```bash
for i in $(seq 1 8); do
  curl -s "http://localhost:8080/api/v1/remittance/transfers/${TRANSFER_ID}" \
    -H 'X-User-Id: 1' | jq
  sleep 2
done
```

`REQUESTED -> SUBMITTED -> CONFIRMED/FAILED`로 변한다.

주의:

- `senderAddress`는 사용자의 내부 wallet address다.
- 사용자는 이 wallet private key를 몰라도 된다.
- 실제 서명은 서버가 저장한 암호화 private key를 복호화해서 수행한다.

### 5.6 무결성 해시 조회
```bash
curl -s 'http://localhost:8080/api/v1/remittance/transfers/tr_seed_001/integrity-hash' \
  -H 'X-User-Id: 1' | jq
```

설명:

- `normalizedPayload`는 해시 대상 JSON이다.
- `payloadHash`는 `keccak256(normalizedPayload)` 값이다.
- `proofId`는 `keccak256(docType + ":" + sourceRef + ":" + payloadHash)` 값이다.

### 5.7 영수증 링크 발급
```bash
curl -s -X POST "http://localhost:8080/api/v1/remittance/transfers/${TRANSFER_ID}/receipt-link" \
  -H 'X-User-Id: 1' | tee /tmp/receipt-link.json | jq
```

링크 열기:
```bash
URL_PATH=$(jq -r '.downloadUrl' /tmp/receipt-link.json)
curl -s "http://localhost:8080${URL_PATH}" -H 'X-User-Id: 1'
```

## 6. 데모용 실패 케이스

현재 데모 게이트웨이는 `amount % 13 == 0`이면 실패로 수렴한다.

실패 예시:
```bash
curl -s -X POST 'http://localhost:8080/api/v1/remittance/transfers' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -H 'Idempotency-Key: idem-fail-001' \
  -d '{
    "recipientId":"rcp_001",
    "amount":52000,
    "asset":"dUSDC",
    "highAmountConfirmed":true,
    "memo":"fail demo"
  }' | jq
```

(52000은 13으로 나누어떨어짐)

## 7. 구현된 파일

- 앱 시작점: `backend/src/main/java/com/workproofpay/remittance/RemittanceApiApplication.java`
- API 컨트롤러: `backend/src/main/java/com/workproofpay/remittance/controller/RemittanceController.java`
- 정책/송금 서비스: `backend/src/main/java/com/workproofpay/remittance/service/TransferService.java`
- 잡 워커: `backend/src/main/java/com/workproofpay/remittance/job/TransferJobWorker.java`
- 데모 게이트웨이: `backend/src/main/java/com/workproofpay/remittance/gateway/DemoErc20Gateway.java`
- 설정: `backend/src/main/resources/application.yml`

## 8. 현재 한계

- 현재 예제에는 회원가입 모듈이 없어서, `POST /wallets/me`를 회원가입 직후 호출하는 방식으로 wallet 생성 흐름을 대신한다.
- private key는 DB에 암호화 저장하지만, HSM/KMS/MPC 연동은 아직 없다.
- 영수증은 PDF 바이너리 대신 텍스트 렌더링이다.
- 인증(JWT/Spring Security)은 아직 없다. `X-User-Id` 헤더로 사용자 구분만 한다.

다음 단계는 실제 회원가입 시스템과 wallet 자동 생성을 연결하고, HSM/KMS, JWT 인증/권한을 붙이는 것이다.
