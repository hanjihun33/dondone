# Profile and Chain Mode Guide

## 목적

`SPRING_PROFILES_ACTIVE`, `REMITTANCE_CHAIN_MODE`, `VAULT_CHAIN_MODE` 가 각각 무엇을 결정하는지 코드 기준으로 정리한다.

## 요약

| 변수 | 현재 가능한 값 | 역할 | 기능 차이 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`, `demo`, `prod` | 애플리케이션 환경 구분 | 로컬 개발, 발표 시연, 실제 운영 같은 서비스 환경 성격을 나눈다. 체인 구현을 직접 선택하지는 않는다. |
| `REMITTANCE_CHAIN_MODE` | `demo`, `sepolia` | 송금(remittance) 체인 구현 선택 | `demo`는 가짜 체인 구현, `sepolia`는 실제 Sepolia RPC/Web3 호출을 사용한다. |
| `VAULT_CHAIN_MODE` | 현재 실질적으로 `demo`만 정상 사용 가능 | 예치(vault) 체인 구현 선택 | `demo`는 데모 예치 구현을 사용한다. `sepolia`는 현재 구현체가 없어 앱이 기동 실패한다. |

## 1. `SPRING_PROFILES_ACTIVE`

### 역할

- 서비스 전체 환경을 구분한다.
- 시연용 데이터/정책, 운영용 설정 성격을 구분하는 값이다.
- 체인 어댑터를 직접 선택하는 값은 아니다.

### 현재 사용하는 값

- `local`
- `demo`
- `prod`

### 의미

- `local`: 개발자 로컬 실행/검증
- `demo`: 발표/시연용 계정, 데이터, 흐름
- `prod`: 실제 운영 서비스

## 2. `REMITTANCE_CHAIN_MODE`

### 역할

- 송금 기능에서 어떤 블록체인 구현을 사용할지 결정한다.

### 현재 사용하는 값

- `demo`
- `sepolia`

### 값별 차이

#### `demo`

- 구현체:
  - [DemoRemittanceBlockchainGateway.java](/C:/Users/SSAFY/Documents/SSAFY/WorkSpace/Project/DunDun/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/DemoRemittanceBlockchainGateway.java)
- 특징:
  - 메모리 기반 데모 송금
  - 실제 RPC 호출 없음
  - 실제 체인 트랜잭션 없음
  - 발표/시연에 안전함

#### `sepolia`

- 구현체:
  - [SepoliaRemittanceBlockchainGateway.java](/C:/Users/SSAFY/Documents/SSAFY/WorkSpace/Project/DunDun/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/SepoliaRemittanceBlockchainGateway.java)
- 특징:
  - 실제 Sepolia 네트워크 사용
  - RPC URL 필요
  - 토큰 주소 필요
  - treasury private key 필요
  - Web3 / 실제 트랜잭션 흐름 사용

## 3. `VAULT_CHAIN_MODE`

### 역할

- 예치(vault) 기능에서 어떤 블록체인 구현을 사용할지 결정한다.

### 현재 사용하는 값

- 코드 기준으로 현재 정상 사용 가능한 값은 `demo` 뿐이다.

### 값별 차이

#### `demo`

- 구현체:
  - [DemoVaultBlockchainGateway.java](/C:/Users/SSAFY/Documents/SSAFY/WorkSpace/Project/DunDun/S14P21C202/apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/adapter/DemoVaultBlockchainGateway.java)
- 특징:
  - 데모 예치/출금 시뮬레이션
  - 테스트넷 실거래가 아니라 앱 내부 데모 동작

#### `sepolia`

- 현재 구현체가 없다.
- 결과:
  - `VaultBlockchainGateway` 빈 생성 불가
  - `VaultJobWorker` 의존성 주입 실패
  - 애플리케이션 기동 실패

## 4. 실무적으로 안전한 조합

```text
SPRING_PROFILES_ACTIVE=demo
REMITTANCE_CHAIN_MODE=demo
VAULT_CHAIN_MODE=demo
```

의미:

- 앱 환경: 시연용
- 송금: 데모 체인
- 예치: 데모 체인

결과:

- 전체가 안전한 데모 모드로 동작한다.

## 5. 현재 가능한 혼합 조합

```text
SPRING_PROFILES_ACTIVE=demo
REMITTANCE_CHAIN_MODE=sepolia
VAULT_CHAIN_MODE=demo
```

의미:

- 앱 환경: 시연용
- 송금: 실제 Sepolia
- 예치: 데모

결과:

- 송금은 실체인 기반으로 동작
- 예치는 데모 방식으로 동작

## 6. 한 줄 정리

- `SPRING_PROFILES_ACTIVE`: 서비스 환경 구분
- `REMITTANCE_CHAIN_MODE`: 송금 체인 구현 선택
- `VAULT_CHAIN_MODE`: 예치 체인 구현 선택
- 현재 vault는 `demo`만 정상 사용 가능하다.
