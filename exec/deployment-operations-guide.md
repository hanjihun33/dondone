# DonDone 운용 메뉴얼

## 1. 프로젝트 개요

### 1.1 서비스 개요
DonDone은 근로자의 출퇴근 및 근무 기록을 기반으로 급여 검증, 근무 기록 문서화, 미리받기, 송금, 예치 기능을 제공하는 핀테크 서비스이다.

### 1.2 주요 기능
- 근무 기록 관리
- 근무 기록 PDF 생성
- 급여 검증
- 미리받기
- 지갑 간 송금
- 예치 기능
- 블록체인 기반 거래 확인

### 1.3 서비스 구성
- 백엔드: `apps/dondone-backend`
- 웹 프론트엔드: `apps/dondone-web`
- 안드로이드 앱: `apps/dondone-mobile/android`
- 블록체인 컨트랙트: `apps/dondone-blockchain`
- 배포/인프라: 루트 `docker-compose.yml`, `Jenkinsfile`, `deploy/`

## 2. 기술 스택

### 2.1 Backend
- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- Querydsl
- PostgreSQL
- Redis
- JWT
- Web3j
- OpenHTMLtoPDF

### 2.2 Web Frontend
- React 18.3.1
- TypeScript 5.6.3
- Vite 5.4.x
- React Router DOM 6.28.1

### 2.3 Mobile
- Android
- Kotlin
- Jetpack Compose
- OkHttp
- Kakao Map SDK

### 2.4 Blockchain
- Solidity 0.8.24
- Foundry
- Sepolia 네트워크 기반 설정

### 2.5 Infra / DevOps
- Docker
- Docker Compose
- Nginx
- Jenkins

## 3. 시스템 구성

### 3.1 운영 구성
운영 환경은 SSAFY 제공 AWS EC2 1대에서 Docker Compose 기반으로 구동한다. 별도의 추가 리버스 프록시 서버나 별도 도메인 서버는 사용하지 않는다.

구성 요소:
- `postgres`
- `redis`
- `api-server`
- `web`
- `nginx`
- `jenkins`

### 3.2 네트워크/프록시 구성
- 외부 서비스 도메인: `dondone.duckdns.org`
- Nginx가 80/443 포트를 수신
- `/api/` 요청은 Spring Boot API 서버로 프록시
- `/jenkins/` 요청은 Jenkins로 프록시
- 일반 웹 요청은 React 정적 웹 서버로 프록시

### 3.3 Health Check
- Health Check URL: `https://dondone.duckdns.org/health`

## 4. 운영 서버 환경

### 4.1 서버 정보
- 서버 제공: SSAFY 제공 AWS EC2
- 인스턴스 타입: `t2.xlarge`
- 운영체제: `Ubuntu 24.04.4 LTS`
- CPU: `4 vCPU`
- 메모리: `15 GiB`
- 디스크: `320 GB`
- 루트 파일시스템 사용 가능 용량: `309 GB`

### 4.2 서버 확인 명령어
운영 서버 환경 확인 시 사용한 명령어는 다음과 같다.

```bash
# 인스턴스 타입
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600" -s)
curl -H "X-aws-ec2-metadata-token: $TOKEN" -s http://169.254.169.254/latest/meta-data/instance-type

# OS 버전
cat /etc/os-release

# CPU 정보
lscpu

# 메모리 정보
free -h

# 디스크 사용량
df -h

# 블록 디바이스 구조
lsblk
```

## 5. SSAFY EC2 규정 반영 사항

SSAFY 제공 규정 및 내부 참고 문서(`Docs/File/ufw 포트 설정하기.txt`) 기준으로 다음 사항을 준수한다.

### 5.1 서버 접근
- SSAFY에서 제공한 `.pem` 키를 이용하여 `ubuntu` 계정으로 SSH 접속한다.
- 예시:

```bash
ssh -i <팀키>.pem ubuntu@<팀>.p.ssafy.io
```

### 5.2 방화벽(UFW)
- UFW는 반드시 활성화(enable) 상태로 유지한다.
- 기본적으로 SSH(22) 포트 접속이 가능해야 한다.
- 서비스 운영에 필요한 포트만 최소한으로 허용한다.
- 포트 허용 및 상태 확인 예시:

```bash
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw status
```

### 5.3 운영 보안 주의사항
- `/home` 및 시스템 디렉터리의 권한을 임의로 변경하지 않는다.
- EC2는 외부에서 접근 가능한 서버이므로 중요 파일, 계정, DB 비밀번호, 키 관리에 주의한다.
- SSH 포트 차단, 공개키 삭제, 퍼미션 임의 변경 등 복구가 어려운 조작은 지양한다.
- Jenkins, DB, 외부 서비스 토큰 등 민감 정보는 Git 저장소에 커밋하지 않는다.

## 6. 저장소 구조

```text
S14P21C202
├─ apps
│  ├─ dondone-backend
│  ├─ dondone-web
│  ├─ dondone-mobile
│  └─ dondone-blockchain
├─ deploy
├─ docs
├─ docker-compose.yml
├─ docker-compose.dev.yml
├─ .env
├─ .env.example
└─ Jenkinsfile
```

## 7. 환경 변수 설정

### 7.1 공통 환경 변수 파일
운영 및 실행 환경 변수는 루트 `.env` 파일을 기준으로 관리한다. 기본 형식은 `.env.example` 에 정의되어 있다.

### 7.2 주요 환경 변수 항목

#### 데이터 저장소
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`

#### 인증/보안
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION_MS`
- `EMPLOYER_SIGNUP_CODE_ENCRYPTION_KEY`

#### Remittance
- `REMITTANCE_CHAIN_MODE`
- `REMITTANCE_CHAIN_RPC_URL`
- `REMITTANCE_CHAIN_ID`
- `REMITTANCE_TOKEN_ADDRESS`
- `REMITTANCE_TREASURY_PRIVATE_KEY`
- `REMITTANCE_WALLET_ENCRYPTION_KEY`
- `REMITTANCE_INITIAL_TOKEN_AMOUNT_ATOMIC`
- `REMITTANCE_INITIAL_NATIVE_AMOUNT_WEI`

#### Vault
- `VAULT_CHAIN_MODE`
- `VAULT_CHAIN_RPC_URL`
- `VAULT_CHAIN_ID`
- `VAULT_TOKEN_ADDRESS`
- `VAULT_ADDRESS`

#### WorkProof 기본 계약
- `WORKPROOF_DEFAULT_CONTRACT_ENABLED`
- `WORKPROOF_DEFAULT_CONTRACT_PAY_UNIT`
- `WORKPROOF_DEFAULT_CONTRACT_BASE_PAY_AMOUNT`
- `WORKPROOF_DEFAULT_CONTRACT_DAILY_WORK_MINUTES`
- `WORKPROOF_DEFAULT_CONTRACT_MONTHLY_WORK_MINUTES`
- `WORKPROOF_DEFAULT_CONTRACT_PAYDAY_DAY`

#### Web Build
- `VITE_API_BASE_URL`
- `VITE_KAKAO_MAP_APP_KEY`

### 7.3 주의사항
- `.env` 실제 값은 제출 문서에 기재하지 않는다.
- 비밀키, 토큰, 비밀번호, private key는 Git에 커밋하지 않는다.
- 운영 환경에서 `.env` 변경 후에는 관련 컨테이너를 재생성해야 반영된다.

예시:

```bash
docker compose up -d --force-recreate api-server
docker compose up -d --force-recreate web
```

## 8. 외부 서비스 및 연동 도구

### 8.1 필수 외부 서비스
1. Kakao Map
- 용도: 지도/위치 기반 기능

2. DuckDNS
- 용도: 서비스 도메인 연결
- 사용 도메인: `dondone.duckdns.org`

3. Let’s Encrypt
- 용도: HTTPS 인증서 발급 및 적용

4. Sepolia Network
- 용도: 블록체인 테스트넷 사용

5. Sepolia RPC Endpoint
- 용도: 백엔드 및 블록체인 연동 통신
- RPC 제공자명: `추후 기입`

### 8.2 운영/관리 도구
1. Jenkins
- 용도: CI/CD 배포 자동화

2. Mattermost
- 용도: 운영/배포 알림

### 8.3 검증/탐색 도구
1. Etherscan
- 용도: Sepolia 네트워크 상 거래 및 토큰 전송 내역 확인

### 8.4 참고 사항
- Jenkins, Mattermost 등 운영 관리 도구의 실제 접근 URL 및 민감 설정값은 본 문서에 직접 공개하지 않는다.
- 서비스 운영에 필요한 실제 외부 서비스 계정 및 API 키는 제출 문서에 포함하지 않는다.

## 9. 로컬 개발 환경 실행 방법

### 9.1 개발용 인프라 실행
```bash
docker compose -f docker-compose.dev.yml up -d
```

개발용 compose에서 제공하는 주요 포트:
- PostgreSQL: `5433`
- Redis: `6379`
- MinIO API: `9000`
- MinIO Console: `9001`
- Prometheus: `9090`
- Grafana: `3000`

### 9.2 백엔드 실행
```bash
cd apps/dondone-backend
./gradlew bootRun
```

Windows:

```powershell
cd apps/dondone-backend
.\gradlew.bat bootRun
```

기본 API 포트:
- `http://localhost:8080`

Swagger:
- `http://localhost:8080/swagger-ui.html`

### 9.3 웹 실행
```bash
cd apps/dondone-web
npm ci
npm run build
```

개발 실행:

```bash
npm run dev
```

### 9.4 모바일 실행
Android Studio 또는 Gradle을 이용하여 실행한다.

- compileSdk: `36`
- minSdk: `26`
- targetSdk: `36`
- Java target: `17`

모바일 API Base URL은 다음 순서로 결정된다.
1. `local.properties` 의 `DONDONE_API_BASE_URL`
2. 환경 변수 `DONDONE_API_BASE_URL`
3. 기본값 `http://10.0.2.2:8080`

실기기 테스트 시에는 다음 중 하나를 사용한다.
- PC의 LAN IP 사용
- `adb reverse tcp:8080 tcp:8080`

### 9.5 블록체인 컨트랙트 실행
```bash
cd apps/dondone-blockchain
forge build
forge test -vv
```

## 10. 운영 배포 방법

### 10.1 운영 서비스 구성
루트 `docker-compose.yml` 을 기준으로 운영 서비스를 구동한다.

주요 서비스:
- `postgres`
- `redis`
- `api-server`
- `web`
- `nginx`
- `jenkins`

### 10.2 배포 디렉터리
- 운영 배포 경로: `/srv/dondone/app`

### 10.3 Jenkins 기반 배포
Jenkinsfile 기준 배포 흐름은 다음과 같다.

1. 저장소 Checkout
2. Backend Test 수행
3. 운영 서버 저장소 동기화
4. Docker Compose 유효성 확인
5. `deploy/sql/*.sql` 마이그레이션 적용
6. 서비스 재배포
7. Health Check 수행

### 10.4 운영 배포 시 주의사항
- `deploy/sql/*.sql` 은 Jenkins가 자동 적용한다.
- `deploy/sql/demo/*` 는 자동 적용되지 않으며, 데모/테스트 데이터 용도로만 수동 실행한다.
- 운영 `.env` 수정 시 컨테이너 재생성이 필요하다.
- 민감한 블록체인 private key는 `.env` 로만 주입하고 문서나 저장소에 기록하지 않는다.

## 11. 데이터베이스 마이그레이션 정책

### 11.1 자동 적용 경로
- `deploy/sql/*.sql`

Jenkins 배포 시 위 경로의 SQL 파일이 자동으로 순차 적용된다.

### 11.2 수동 적용 경로
- `deploy/sql/demo/*`

이 경로는 운영용 자동 마이그레이션 대상이 아니다.  
테스트 계정 데이터 주입 등 필요한 경우에만 수동으로 실행한다.

예시:

```bash
docker exec -i app-postgres-1 psql -U ssafy -d dondone < deploy/sql/demo/<파일명>.sql
```

## 12. 주요 포트 정리

### 12.1 운영 기준
- 22/tcp: SSH
- 80/tcp: HTTP
- 443/tcp: HTTPS

### 12.2 서비스 내부 포트
- API Server: `8080`
- Web: `80`
- Jenkins: `8080`
- PostgreSQL: `5432`
- Redis: `6379`

### 12.3 개발용 추가 포트
- PostgreSQL 외부 노출: `5433`
- MinIO API: `9000`
- MinIO Console: `9001`
- Prometheus: `9090`
- Grafana: `3000`

## 13. 테스트 및 검증 방법

### 13.1 백엔드 테스트
```bash
cd apps/dondone-backend
./gradlew test
./gradlew integrationTest
```

### 13.2 웹 빌드 검증
```bash
cd apps/dondone-web
npm ci
npm run build
```

### 13.3 모바일 빌드 검증
```bash
cd apps/dondone-mobile/android
.\gradlew.bat :app:compileDebugKotlin
```

### 13.4 운영 상태 확인
```bash
docker ps
docker logs --tail 200 app-api-server-1
curl -k https://dondone.duckdns.org/health
```

## 14. 보안 및 운영 주의사항

- `.env`, private key, JWT secret, DB 비밀번호 등 민감 정보는 제출 문서에 포함하지 않는다.
- UFW는 반드시 enable 상태를 유지한다.
- 서비스 운영용 포트만 허용한다.
- 운영 서버의 `/home` 및 시스템 디렉터리 권한을 임의로 변경하지 않는다.
- SSH 접속용 `.pem` 키 파일은 안전하게 보관한다.
- 운영 콘솔 성격의 Jenkins 접근 URL은 제출 문서 본문에 직접 노출하지 않는다.
- 블록체인 네트워크 관련 private key 및 wallet encryption key는 Git 저장소에 포함하지 않는다.

## 15. 작성 시점 기준 미확정 항목

아래 항목은 현재 저장소와 제공 정보만으로는 정확한 값을 확정할 수 없어 추후 기입이 필요하다.

- Sepolia RPC 제공자명
- 필요 시 운영 알림 체계의 세부 연결 방식

## 16. 부록

### 16.1 참고 파일
- `.env.example`
- `docker-compose.yml`
- `docker-compose.dev.yml`
- `Jenkinsfile`
- `apps/dondone-backend/README.md`
- `apps/dondone-blockchain/README.md`
- `deploy/nginx/nginx.conf`
- `Docs/File/ufw 포트 설정하기.txt`

### 16.2 제출 문서 작성 원칙
- 실제 저장소와 운영 환경에서 확인된 내용만 기재한다.
- 추측, 가정, 임의값은 작성하지 않는다.
- 민감 정보는 제거하고 구조와 절차 중심으로 설명한다.
