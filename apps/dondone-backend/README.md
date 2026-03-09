# DonDone Backend (Skeleton + Auth)

`docs/DonDone_PRD_v1.5.md` 기준으로 초기 백엔드 스켈레톤과 로그인 기능을 구성했습니다.

## Stack
- Java 17
- Spring Boot 3.2.5
- Spring Security (JWT)
- Spring Data JPA
- Querydsl (JPA + APT)
- PostgreSQL

## Feature-first Skeleton

```text
com.workproofpay.backend
  ├─ auth
  ├─ workproof
  ├─ wage
  ├─ safepay
  ├─ remittance
  ├─ documents
  ├─ jobs
  └─ shared
```

## Run

### 1) 개발용 의존성 실행

전체 개발 스택(PostgreSQL + Redis + MinIO):

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2) 백엔드 실행

```bash
./gradlew bootRun
```

기본 DB 설정은 아래 환경변수를 사용합니다.

- `DB_URL` 또는 `SPRING_DATASOURCE_URL`
- default: `jdbc:postgresql://localhost:5433/dondone`
- 또는 `DB_HOST` / `DB_PORT` / `DB_NAME` 조합으로 지정 가능
- `DB_USERNAME` 또는 `SPRING_DATASOURCE_USERNAME`
- default: `dondone`
- `DB_PASSWORD` 또는 `SPRING_DATASOURCE_PASSWORD`
- default: `dondone`

로컬 머신에 PostgreSQL이 이미 `5432` 포트에서 실행 중인 경우가 많아,
프로젝트 기본 포트는 `5433`을 사용합니다.

개발용 compose 기본 포트:
- PostgreSQL: `localhost:5433`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- MinIO bucket: `dondone-private`

MinIO 기본 계정:
- Access Key: `dondone`
- Secret Key: `dondone123!`

현재 레포에는 `pdf-service` 구현 디렉터리가 없어 `docker-compose.dev.yml`에는 포함하지 않았습니다.

## Test

```bash
./gradlew test
```

기본 `test` 태스크는 Docker가 필요 없는 테스트만 실행합니다.

Docker 기반 통합 테스트는 별도로 실행합니다.

```bash
./gradlew integrationTest
```

통합 테스트는 `PostgreSQL Testcontainers`를 사용합니다.
Docker daemon 접근이 가능해야 실행됩니다.

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Seed Account
- Email: `test@gmail.com`
- Password: `qweqwe123`

## Auth API
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me` (Bearer 토큰 필요)

## Notes
- 현재는 MVP 스켈레톤 단계로, PRD의 나머지 도메인(workproof/wage/...)은 `ping` 엔드포인트만 제공합니다.
- 운영 전환 시 Refresh Token/권한 정책/SafePay 정책을 확장해야 합니다.
