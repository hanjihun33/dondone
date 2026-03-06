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

### 1) PostgreSQL 실행

```bash
docker compose -f docker-compose.postgres.yml up -d
```

### 2) 백엔드 실행

```bash
./gradlew bootRun
```

기본 DB 설정은 아래 환경변수를 사용합니다.

- `DB_URL` 또는 `SPRING_DATASOURCE_URL`
- default: `jdbc:postgresql://localhost:5432/dondone`
- `DB_USERNAME` 또는 `SPRING_DATASOURCE_USERNAME`
- default: `dondone`
- `DB_PASSWORD` 또는 `SPRING_DATASOURCE_PASSWORD`
- default: `dondone`

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
