# DonDone Environment And Secrets Guide

> 이 문서는 DonDone 프로젝트의 환경 변수와 secret 관리 기준을 정의한다.
> 실제 secret 값은 저장하지 않는다.
> 이 문서에는 변수명, 용도, 주입 시점, 저장 위치, 운영 규칙만 기록한다.

## 1. 목표

이 문서의 목적은 아래 4가지다.

1. 어떤 환경 변수가 실제로 사용되는지 명확히 한다.
2. 각 값이 어느 시점에 주입되는지 구분한다.
3. 로컬 개발, 발표 시연, 운영 배포의 책임 경계를 분리한다.
4. 신규 팀원이 와도 `.env`, Compose, Jenkins 기준을 혼동하지 않게 한다.

## 2. 핵심 원칙

### 2-1. `.env`는 실제 값의 단일 입력원이다

- `docker-compose.yml`과 `docker-compose.dev.yml`은 값을 소유하지 않는다.
- Compose는 필요한 값을 선언하고 `.env`에서 주입받는다.
- 운영에 영향을 주는 값은 Compose fallback 기본값에 숨기지 않는다.

### 2-2. build-time env와 runtime env를 분리한다

- backend/infra 실행 시 읽는 값은 `runtime env`다.
- `VITE_*` 계열은 웹 정적 번들 생성 시에만 읽는 `build-time env`다.
- `build-time env`를 변경하면 `web` 이미지를 다시 빌드해야 한다.

### 2-3. 로컬 전용 값은 운영 카탈로그와 분리한다

- `MINIO_*`, Grafana 로컬 계정 등은 `docker-compose.dev.yml` 전용이다.
- 운영/발표 배포에서 쓰지 않는 값은 운영 필수값 표에 넣지 않는다.

### 2-4. 미사용 값은 env 인벤토리에 넣지 않는다

- 코드에서 참조하지 않는 값은 `.env.example`에 추가하지 않는다.
- 예: `VAULT_ADDRESS`는 현재 레포에서 사용처가 없으므로 표준 env 목록에서 제외한다.

## 3. Compose 역할

### 3-1. `docker-compose.yml`

배포/런타임 스택용 파일이다.

- `postgres`
- `redis`
- `api-server`
- `web`
- `nginx`
- `jenkins`

현재 Jenkins 파이프라인과 운영 서버 구조가 이 파일을 기준으로 동작하므로, 서비스 이름과 기본 인터페이스는 안정적으로 유지한다.

### 3-2. `docker-compose.dev.yml`

로컬 개발 보조 인프라용 파일이다.

- `postgres`
- `redis`
- `minio`
- `minio-init`
- `prometheus`
- `grafana`

이 파일은 로컬 개발 편의가 목적이다. 운영 배포 파이프라인의 기준이 아니다.

## 4. 실제 값 저장 위치

| 구분 | 저장 위치 | Git 커밋 | 설명 |
|---|---|---|---|
| 예시 템플릿 | `.env.example` | 가능 | 프로젝트 전체 기준 템플릿 |
| 서버 배포용 실제 값 | `/srv/dondone/app/.env` | 불가 | Jenkins와 Compose가 읽는 운영/발표 서버 실제 값 |
| 로컬 개발용 실제 값 | 로컬 `.env` | 불가 | 개발자 개인 환경 |
| Jenkins credential | Jenkins Credentials | 불가 | GitLab credential, Mattermost webhook 등 |
| 외부 서비스 secret | 외부 서비스 설정 | 불가 | GitLab webhook secret, OAuth secret 등 |
| 운영 규칙 문서 | `docs/infra/*.md` | 가능 | 실제 값 없이 정책만 기록 |

## 5. 환경 구분

| 환경 | 목적 | `SPRING_PROFILES_ACTIVE` | Compose 기준 |
|---|---|---|---|
| local | 개발자 로컬 실행/검증 | `local` 또는 팀 합의값 | `docker-compose.dev.yml` |
| demo | 발표 시연용 계정/데이터 포함 환경 | `demo` | `docker-compose.yml` |
| prod | 실제 서비스 운영 | `prod` | `docker-compose.yml` |

### 정책

- `demo` 프로필은 발표 시연용 계정/데이터가 필요하므로 유지 가능하다.
- 단, `demo`는 fallback 기본값이 아니라 `.env`에서 명시적으로 선택해야 한다.
- 타임 트래블 기능 같은 PRD 데모 기능 유지 여부와 `demo` 프로필 유지 여부는 별개다.

## 6. 변수 카탈로그

### 6-1. Shared Runtime

운영/발표 서버의 `api-server`, `postgres`, `redis` 실행에 직접 관여하는 값이다.

| 키 이름 | 필수 | Secret | 주입 시점 | 설명 |
|---|---|---|---|---|
| `POSTGRES_DB` | Y | N | runtime | PostgreSQL DB 이름 |
| `POSTGRES_USER` | Y | N | runtime | PostgreSQL 계정 |
| `POSTGRES_PASSWORD` | Y | Y | runtime | PostgreSQL 비밀번호 |
| `REDIS_PASSWORD` | Y | Y | runtime | Redis 비밀번호 |
| `JWT_SECRET` | Y | Y | runtime | JWT 서명 키 |
| `JWT_ACCESS_EXPIRATION_MS` | N | N | runtime | Access token 만료 시간 |
| `SPRING_PROFILES_ACTIVE` | Y | N | runtime | `demo` 또는 `prod`를 명시적으로 선택 |
| `EMPLOYER_SIGNUP_CODE_ENCRYPTION_KEY` | N | Y | runtime | 미입력 시 `REMITTANCE_WALLET_ENCRYPTION_KEY` fallback 사용 |

### 6-2. Remittance Runtime

송금/정산 기능에 필요한 backend runtime 값이다.

| 키 이름 | 필수 | Secret | 주입 시점 | 설명 |
|---|---|---|---|---|
| `REMITTANCE_CHAIN_MODE` | Y | N | runtime | 예: `demo`, `sepolia` |
| `REMITTANCE_CHAIN_RPC_URL` | N | N | runtime | 체인 RPC URL |
| `REMITTANCE_CHAIN_ID` | N | N | runtime | 체인 ID |
| `REMITTANCE_TOKEN_ADDRESS` | N | N | runtime | 토큰 컨트랙트 주소 |
| `REMITTANCE_TREASURY_PRIVATE_KEY` | N | Y | runtime | 운영 treasury private key |
| `REMITTANCE_WALLET_ENCRYPTION_KEY` | N | Y | runtime | 지갑 암호화 키 |
| `REMITTANCE_WALLET_FUNDING_RECEIPT_TIMEOUT_SECONDS` | N | N | runtime | funding receipt timeout |
| `REMITTANCE_WALLET_FUNDING_PENDING_STALE_SECONDS` | N | N | runtime | funding stale timeout |
| `REMITTANCE_ASSET_SYMBOL` | N | N | runtime | 자산 심볼 |
| `REMITTANCE_ASSET_DECIMALS` | N | N | runtime | 자산 decimal |
| `REMITTANCE_HIGH_AMOUNT_THRESHOLD_ATOMIC` | N | N | runtime | 고액 기준 |
| `REMITTANCE_RECENT_RECIPIENT_WINDOW_SECONDS` | N | N | runtime | 최근 수취인 탐지 윈도우 |
| `REMITTANCE_MAX_AUTO_RETRY_COUNT` | N | N | runtime | 자동 재시도 횟수 |
| `REMITTANCE_DEFAULT_LIST_LIMIT` | N | N | runtime | 목록 기본 limit |
| `REMITTANCE_WORKER_POLL_INTERVAL_MS` | N | N | runtime | worker polling 간격 |
| `REMITTANCE_RECEIPT_POLL_DELAY_SECONDS` | N | N | runtime | receipt polling delay |
| `REMITTANCE_RECEIPT_TIMEOUT_SECONDS` | N | N | runtime | receipt timeout |
| `REMITTANCE_TOKEN_GAS_LIMIT` | N | N | runtime | 토큰 전송 gas limit |
| `REMITTANCE_NATIVE_TRANSFER_GAS_LIMIT` | N | N | runtime | native transfer gas limit |
| `REMITTANCE_INITIAL_TOKEN_AMOUNT_ATOMIC` | N | N | runtime | 초기 토큰 지급량 |
| `REMITTANCE_INITIAL_NATIVE_AMOUNT_WEI` | N | N | runtime | 초기 native 지급량 |

### 6-3. Web Build-Time Variables

웹 정적 파일을 빌드할 때만 사용된다.

| 키 이름 | 필수 | Secret | 주입 시점 | 설명 |
|---|---|---|---|---|
| `VITE_KAKAO_MAP_APP_KEY` | N | N | build-time | 카카오 지도 JS 키 |
| `VITE_API_BASE_URL` | N | N | build-time | 개발/특수 환경용 API base override |

### 6-4. Local Development Only

`docker-compose.dev.yml`에서만 사용하는 값이다.

| 키 이름 | 필수 | Secret | 주입 시점 | 설명 |
|---|---|---|---|---|
| `MINIO_ROOT_USER` | Y | N | runtime-dev | 로컬 MinIO 계정 |
| `MINIO_ROOT_PASSWORD` | Y | Y | runtime-dev | 로컬 MinIO 비밀번호 |
| `MINIO_BUCKET` | N | N | runtime-dev | 로컬 MinIO bucket 이름 |
| `GRAFANA_ADMIN_USER` | N | N | runtime-dev | 로컬 Grafana 계정 |
| `GRAFANA_ADMIN_PASSWORD` | N | N | runtime-dev | 로컬 Grafana 비밀번호 |

### 6-5. CI/CD And External Secrets

이 값들은 `.env`가 아니라 Jenkins나 외부 서비스에 저장한다.

| 항목 | 저장 위치 | 설명 |
|---|---|---|
| `gitlab-credentials` | Jenkins Credentials | Git clone/pull |
| `mattermost-webhook` | Jenkins Credentials | 배포 알림 |
| GitLab Webhook Secret Token | GitLab + Jenkins | webhook 검증 |

## 7. 현재 표준 운영 규칙

### 7-1. `.env.example`

- placeholder와 팀 표준 기본 구조만 둔다.
- 운영 secret, private key, 실제 webhook URL은 넣지 않는다.
- 사용하지 않는 값은 추가하지 않는다.
- 서버 `/srv/dondone/app/.env`도 이 파일을 복사해서 만드는 것을 기준으로 한다.
- `apps/dondone-web/.env.example`는 웹만 단독 실행할 때의 보조 템플릿일 뿐, 프로젝트 정본은 아니다.

### 7-2. `docker-compose.yml`

- 운영/발표에 영향 있는 값은 `.env`에서 명시적으로 받아야 한다.
- `SPRING_PROFILES_ACTIVE` 같은 핵심 값은 fallback 기본값을 두지 않는다.
- backend runtime env는 `api-server`의 명시적 `environment` 매핑으로만 주입한다.
- 웹 빌드용 값은 `build.args`로만 전달한다.

### 7-3. `docker-compose.dev.yml`

- 로컬 개발 인프라만 담당한다.
- 운영 전용 reverse proxy, Jenkins, TLS 설정은 포함하지 않는다.

## 8. 신규 env 추가 규칙

새 env를 추가할 때는 아래를 같이 갱신한다.

1. 실제 코드에서 참조가 생겼는지 확인한다.
2. `.env.example`에 placeholder를 추가한다.
3. 이 문서의 변수 카탈로그에 추가한다.
4. 주입 시점을 결정한다.
   - runtime
   - build-time
   - runtime-dev
   - Jenkins/외부 secret
5. Compose 또는 Jenkins에서 실제 주입 지점을 추가한다.

## 9. 운영 체크리스트

### 배포 전

- [ ] `/srv/dondone/app/.env`가 최신 기준으로 정리되어 있다
- [ ] `SPRING_PROFILES_ACTIVE`가 `demo` 또는 `prod`로 명시되어 있다
- [ ] `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`이 비어 있지 않다
- [ ] 발표 시연에 카카오 지도 기능이 필요하면 `VITE_KAKAO_MAP_APP_KEY`가 설정되어 있다
- [ ] Jenkins Credentials가 최신 상태다

### 배포 후

- [ ] `docker compose ps`에서 `postgres`, `redis`, `api-server`, `web`, `nginx`, `jenkins` 상태가 정상이다
- [ ] `https://dondone.duckdns.org/health`가 200 응답을 반환한다
- [ ] `https://dondone.duckdns.org/`가 200 응답을 반환한다
- [ ] `https://dondone.duckdns.org/jenkins/` 접근이 정상이다

## 10. 비고

- 현재 Jenkins와 운영 서버는 `docker-compose.yml`을 기준으로 동작하므로, 더 큰 구조 분리 예를 들어 `docker-compose.ops.yml` 분리는 후속 작업으로 별도 진행하는 편이 안전하다.
- `VAULT_ADDRESS`처럼 아직 코드에서 참조하지 않는 값은 문서와 예시 env에서 제외한다. 코드 사용처가 생긴 시점에 다시 인벤토리에 추가한다.
