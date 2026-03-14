# DonDone Environment And Secrets Guide

> 이 문서는 DonDone 프로젝트의 환경 변수와 secret 관리 기준을 정리한다.
> 실제 secret 값은 이 문서에 기록하지 않는다.
> 이 문서에는 변수명, 용도, 저장 위치, 환경별 차이, 운영 규칙만 기록한다.

---

## 1. 문서 목적

이 문서의 목적은 아래 4가지를 명확히 하는 것이다.

1. 어떤 환경 변수와 secret이 필요한지 한눈에 파악한다.
2. 각 값이 어디에 저장되는지 일관된 규칙을 갖는다.
3. `dev / demo / prod` 환경 차이를 명확히 구분한다.
4. 신규 팀원이 와도 설정 누락 없이 실행과 배포를 할 수 있도록 한다.

---

## 2. 기본 원칙

### 2-1. Git 저장소에 올려도 되는 것

- env 변수명
- 용도 설명
- placeholder 예시값
- 저장 위치
- 환경별 사용 여부
- 운영 체크리스트

### 2-2. Git 저장소에 올리면 안 되는 것

- 실제 비밀번호
- 실제 토큰 값
- 실제 Webhook URL 원문
- 실제 `.env`
- 실제 private key
- Jenkins Credential 실제 값
- GitLab Webhook Secret Token 실제 값

### 2-3. 저장 원칙

- 애플리케이션 실행용 실제 환경 변수: 서버 `.env`
- 로컬 개발용 예시값: `.env.example`
- CI/CD 연동 secret: Jenkins Credentials
- 외부 서비스 webhook secret: 외부 서비스 설정 + Jenkins Credentials
- 운영 규칙/문서: `docs/infra/*.md`

---

## 3. 현재 프로젝트 기준 환경 구분

| 환경 | 실행 방식 | 목적 | 비고 |
|---|---|---|---|
| `dev` | `docker-compose.dev.yml` | 로컬 개발 | Postgres/Redis/MinIO 로컬 구동 |
| `demo` | `docker-compose.yml` + `SPRING_PROFILES_ACTIVE=demo` | 데모/시연 | 현재 레포 기본값 기준 |
| `prod` | 추후 명시 분리 필요 | 운영 | 현재 전용 `application-prod.yml` 없음 |

### 현재 상태 메모

- backend 리소스 파일은 현재 `application.yml` 하나만 존재한다.
- `SPRING_PROFILES_ACTIVE`의 레포 기본값은 `demo`다.
- `prod` 전용 profile 파일은 아직 분리되지 않았다.
- demo 관련 정책은 PRD, API Contract, Functional Spec에 정의되어 있고 실제 환경 분리 기준은 이 문서에서 관리한다.

---

## 4. 저장 위치 규칙

| 구분 | 저장 위치 | Git 커밋 가능 여부 | 비고 |
|---|---|---|---|
| 앱 실행용 예시 env | `.env.example` | 가능 | placeholder만 기록 |
| 서버 배포용 실제 env | `/srv/dondone/app/.env` | 불가 | Compose 실행용 실제 값 |
| 로컬 개발용 실제 env | 로컬 `.env` | 불가 | 개발자 개인 환경 |
| Jenkins Git clone credential | Jenkins Credentials | 불가 | `gitlab-credentials` |
| Jenkins GitLab API token | Jenkins Credentials | 불가 | `gitlab-api-token` |
| Jenkins Mattermost webhook | Jenkins Credentials | 불가 | `mattermost-webhook` |
| GitLab Webhook Secret Token | GitLab + Jenkins Job 설정 | 불가 | 실제 값 문서 기록 금지 |
| 운영 문서 | `docs/infra/*.md` | 가능 | 실제 secret 제외 |

---

## 5. 실제 사용 중인 환경 변수 / Secret 인벤토리

### 5-1. 공통 앱 / Compose 변수

| 키 이름 | 분류 | Secret 여부 | 현재 사용 위치 | 기본값 / 예시값 | 실제 저장 위치 | 사용 환경 | 비고 |
|---|---|---|---|---|---|---|---|
| `POSTGRES_DB` | DB | N | `docker-compose.yml`, `docker-compose.dev.yml` | `dondone` | `.env.example`, 서버 `.env`, 로컬 `.env` | dev/demo/prod | DB 이름 |
| `POSTGRES_USER` | DB | N | `docker-compose.yml`, `docker-compose.dev.yml` | `.env.example=ssafy`, Compose fallback=`dondone` | `.env.example`, 서버 `.env`, 로컬 `.env` | dev/demo/prod | 기본값 불일치 있음, 추후 통일 권장 |
| `POSTGRES_PASSWORD` | DB | Y | `docker-compose.yml`, `docker-compose.dev.yml` | 비움 | 서버 `.env`, 로컬 `.env` | dev/demo/prod | 운영 Compose에서는 필수 |
| `REDIS_PASSWORD` | Cache | Y | `docker-compose.yml`, `application.yml` | 비움 | 서버 `.env`, 로컬 `.env` | demo/prod | 운영 Compose에서는 필수 |
| `JWT_SECRET` | Auth | Y | `docker-compose.yml`, `application.yml` | 비움 | 서버 `.env`, 로컬 `.env` | demo/prod | 운영 Compose에서는 필수 |
| `JWT_ACCESS_EXPIRATION_MS` | Auth | N | `.env.example`, `docker-compose.yml`, `application.yml` | `86400000` | `.env.example`, 서버 `.env`, 로컬 `.env` | dev/demo/prod | access token 만료 시간(ms) |
| `SPRING_PROFILES_ACTIVE` | App Env | N | `.env.example`, `docker-compose.yml` | `demo` | `.env.example`, 서버 `.env`, 로컬 `.env` | dev/demo/prod | 현재 레포 기본 profile |
| `TZ` | Runtime | N | `postgres`, `api-server`, `jenkins` | `Asia/Seoul` | Compose 관리 | demo/prod | 시간대 고정 |
| `JAVA_OPTS` | Runtime | N | `api-server`, `jenkins` | `api=-Xms256m -Xmx512m`, `jenkins=-Xms512m -Xmx1024m -Duser.timezone=Asia/Seoul` | Compose 관리 | demo/prod | 서비스별 값 상이 |
| `JENKINS_OPTS` | Runtime | N | `jenkins` | `--prefix=/jenkins` | Compose 관리 | demo/prod | Jenkins 리버스 프록시용 |

### 5-2. backend 내부 참조 변수

| 키 이름 | 분류 | Secret 여부 | 현재 사용 위치 | 기본값 / 예시값 | 실제 주입 위치 | 사용 환경 | 비고 |
|---|---|---|---|---|---|---|---|
| `DB_URL` | DB | N | `docker-compose.yml`, `application.yml` | `jdbc:postgresql://postgres:5432/${POSTGRES_DB}` | Compose env | demo/prod | 서버 배포 기준 직접 주입 |
| `DB_USERNAME` | DB | N | `docker-compose.yml`, `application.yml` | `${POSTGRES_USER}` | Compose env | demo/prod | backend datasource username |
| `DB_PASSWORD` | DB | Y | `docker-compose.yml`, `application.yml` | `${POSTGRES_PASSWORD}` | Compose env | demo/prod | backend datasource password |
| `REDIS_HOST` | Cache | N | `docker-compose.yml`, `application.yml` | `redis` | Compose env | demo/prod | 컨테이너 네트워크 기준 |
| `REDIS_PORT` | Cache | N | `docker-compose.yml`, `application.yml` | `6379` | Compose env | demo/prod | Redis 포트 |
| `DB_HOST` | DB | N | `application.yml` fallback | `localhost` | 로컬 실행 시 개별 주입 가능 | dev | 직접 실행 fallback |
| `DB_PORT` | DB | N | `application.yml` fallback | `5433` | 로컬 실행 시 개별 주입 가능 | dev | `docker-compose.dev.yml` 포트와 일치 |
| `DB_NAME` | DB | N | `application.yml` fallback | `dondone` | 로컬 실행 시 개별 주입 가능 | dev | 직접 실행 fallback |
| `SPRING_DATASOURCE_URL` | DB | N | `application.yml` fallback | 없음 | 선택적 | dev | Spring 표준 키 fallback |
| `SPRING_DATASOURCE_USERNAME` | DB | N | `application.yml` fallback | 없음 | 선택적 | dev | Spring 표준 키 fallback |
| `SPRING_DATASOURCE_PASSWORD` | DB | Y | `application.yml` fallback | 없음 | 선택적 | dev | Spring 표준 키 fallback |

### 5-3. 로컬 개발 전용 MinIO 변수

| 키 이름 | 분류 | Secret 여부 | 현재 사용 위치 | 기본값 / 예시값 | 실제 저장 위치 | 사용 환경 | 비고 |
|---|---|---|---|---|---|---|---|
| `MINIO_ROOT_USER` | Storage | N | `.env.example`, `docker-compose.dev.yml` | `.env.example=ssafy`, dev fallback=`dondone` | `.env.example`, 로컬 `.env` | dev | dev 전용 |
| `MINIO_ROOT_PASSWORD` | Storage | Y | `.env.example`, `docker-compose.dev.yml` | `.env.example=ssafy`, dev fallback=`dondone123!` | 로컬 `.env` | dev | dev 전용, 추후 정리 필요 |
| `MINIO_BUCKET` | Storage | N | `.env.example`, `docker-compose.dev.yml` | `dondone-private` | `.env.example`, 로컬 `.env` | dev | dev 전용 bucket |

### 5-4. Jenkins 파이프라인 런타임 변수

| 키 이름 | 분류 | Secret 여부 | 현재 사용 위치 | 현재 값 | 저장 위치 | 비고 |
|---|---|---|---|---|---|---|
| `DEPLOY_DIR` | CI/CD | N | `Jenkinsfile` | `/srv/dondone/app` | Jenkinsfile | 서버 배포 디렉토리 |
| `TARGET_BRANCH` | CI/CD | N | `Jenkinsfile` | `develop` | Jenkinsfile | 현재 자동배포 기준 브랜치 |
| `HEALTHCHECK_URL` | CI/CD | N | `Jenkinsfile` | `https://dondone.duckdns.org/health` | Jenkinsfile | 배포 후 검증 URL |
| `LOG_FILE` | CI/CD | N | `Jenkinsfile` | `jenkins-console.log` | Jenkinsfile | 실패 알림 로그 수집용 |
| `GIT_USERNAME` | CI/CD | Y | Jenkins runtime env | Jenkins credential에서 주입 | Jenkins Credentials | `gitlab-credentials`에서 주입 |
| `GIT_PASSWORD` | CI/CD | Y | Jenkins runtime env | Jenkins credential에서 주입 | Jenkins Credentials | `gitlab-credentials`에서 주입 |
| `MM_WEBHOOK` | CI/CD | Y | Jenkins runtime env | Jenkins credential에서 주입 | Jenkins Credentials | `mattermost-webhook`에서 주입 |

### 5-5. Jenkins Credentials 인벤토리

| ID | Kind | Secret 여부 | 용도 | 실제 저장 위치 | 현재 사용 여부 |
|---|---|---|---|---|---|
| `gitlab-api-token` | GitLab API token | Y | Jenkins GitLab Connection | Jenkins Credentials | Jenkins UI 연결용 |
| `gitlab-credentials` | Username with password | Y | Git fetch, pull, deploy sync | Jenkins Credentials | Jenkinsfile에서 사용 중 |
| `mattermost-webhook` | Secret text | Y | Mattermost 배포 알림 | Jenkins Credentials | Jenkinsfile에서 사용 중 |

### 5-6. 외부 서비스 설정

| 항목 | Secret 여부 | 현재 값 / 형식 | 관리 위치 | 비고 |
|---|---|---|---|---|
| GitLab Webhook URL | N | `https://dondone.duckdns.org/jenkins/project/dondone-deploy` | GitLab Project Settings | 현재 적용 완료 |
| GitLab Webhook Secret Token | Y | 실제 값 비공개 | GitLab + Jenkins Job | 실제 값 문서 기록 금지 |
| Mattermost Incoming Webhook URL | Y | 실제 값 비공개 | Mattermost + Jenkins Credentials | 실제 URL 문서 기록 금지 |
| Jenkins Base URL | N | `https://dondone.duckdns.org/jenkins/` | Jenkins System Config | 현재 적용 완료 |

---

## 6. 환경별 차이

| 항목 | dev | demo | prod |
|---|---|---|---|
| Compose 파일 | `docker-compose.dev.yml` | `docker-compose.yml` | `docker-compose.yml` 기반 |
| DB 포트 노출 | `5433:5432` | 내부 expose only | 내부 expose only |
| Redis 포트 노출 | `6379:6379` | 내부 expose only | 내부 expose only |
| Redis 비밀번호 | 없음 | 필수 | 필수 |
| MinIO | 사용 | 미사용 | 미정 |
| `SPRING_PROFILES_ACTIVE` | 선택적 | `demo` 권장 | 추후 `prod` 분리 필요 |
| Jenkins 자동배포 | 보통 미사용 | 사용 가능 | 사용 |
| Mattermost 알림 | 선택 | 사용 가능 | 사용 |
| `X-Demo-AsOf` | 개발 검증용 | 허용 | 무시 또는 차단 대상 |

### 환경 차이 메모

- `dev`는 로컬 개발 편의를 위해 단순화되어 있다.
- `demo`와 `prod`는 현재 같은 Compose 뼈대를 공유하지만 문서상 정책은 다르게 관리해야 한다.
- `prod` 전용 설정 분리는 아직 후속 작업 대상이다.

---

## 7. 현재 적용 상태

### 7-1. 이미 구성된 항목

- `.env.example` 존재
- 운영 Compose에서 `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET` 필수 강제
- Jenkins `/jenkins/` 리버스 프록시 적용 완료
- Jenkins Credentials `gitlab-api-token`, `gitlab-credentials`, `mattermost-webhook` 등록 완료
- GitLab Webhook `/jenkins/project/dondone-deploy` 적용 완료
- Mattermost 성공/실패 알림 연동 완료

### 7-2. 아직 정리 필요한 항목

- `POSTGRES_USER`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` 예시값과 fallback 값 통일
- `prod` 전용 profile 또는 환경 분리
- 서버 실제 `.env`와 `.env.example` 차이 검수
- secret rotation 주기 및 담당자 정리
- `pdf-service`, object storage 정식 도입 시 env 확장

---

## 8. 신규 env / secret 추가 규칙

새 값을 추가할 때는 아래를 반드시 같이 갱신한다.

1. `.env.example`에 placeholder 추가
2. 이 문서 인벤토리 표에 추가
3. 저장 위치 결정
   - 서버 `.env`
   - 로컬 `.env`
   - Jenkins Credentials
   - 외부 서비스 설정
4. 환경별 사용 여부 기입
5. 배포 영향 여부 확인

---

## 9. Secret Rotation 규칙

아래 상황에서는 secret 교체를 검토한다.

- 외부 노출 가능성이 있을 때
- 팀원 변경으로 접근 권한 정리가 필요할 때
- Webhook URL 또는 token이 채팅, 캡처, 문서에 노출되었을 때
- 장기간 교체 없이 유지되었을 때

### Rotation 절차

1. 새 secret 발급
2. Jenkins, GitLab, 서버 `.env` 값 교체
3. 연결 테스트
4. 기존 secret 폐기
5. 이 문서 상태만 갱신
   - 실제 secret 값은 기록하지 않음

---

## 10. 운영 체크리스트

### 배포 전

- [ ] 서버 `/srv/dondone/app/.env`에 필수 값이 모두 존재한다
- [ ] `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`이 비어 있지 않다
- [ ] Jenkins Credentials 3종이 최신 값이다
- [ ] GitLab Webhook URL / Secret Token이 최신 상태다
- [ ] Mattermost Webhook이 정상 동작한다

### 배포 후

- [ ] `docker compose ps`에서 `postgres`, `redis`, `api-server`, `nginx`, `jenkins` 상태가 정상이다
- [ ] `curl -fsS https://dondone.duckdns.org/health` 응답이 정상이다
- [ ] `https://dondone.duckdns.org/jenkins/` 접근이 정상이다
- [ ] GitLab Webhook이 `/jenkins/project/dondone-deploy`로 200 응답한다
- [ ] Mattermost 성공/실패 알림이 정상 발송된다

### 신규 팀원 온보딩 시

- [ ] `.env.example` 기반 필수 키 목록 공유
- [ ] 실제 secret은 안전한 채널로만 전달
- [ ] Jenkins Credentials 수정 권한 범위 확인
- [ ] GitLab Webhook / Mattermost Webhook 관리 위치 안내

---

## 11. 금지 사항

- 실제 `.env` 파일을 저장소에 커밋하지 않는다.
- secret 값을 이슈, PR, 문서 본문에 직접 적지 않는다.
- Jenkins Credential 값을 스크린샷으로 공유하지 않는다.
- GitLab Webhook Secret Token 원문을 문서화하지 않는다.
- dev 예시용 기본 비밀번호를 운영 환경에 그대로 사용하지 않는다.

---

## 12. 참고 파일

- `.env.example`
- `docker-compose.yml`
- `docker-compose.dev.yml`
- `apps/dondone-backend/src/main/resources/application.yml`
- `Jenkinsfile`
- `docs/DonDone_PRD_v1.5.md`
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/DonDone_P0_Functional_Spec_v0.md`

---

## 13. 문서 갱신 이력

| 날짜 | 작성자 | 변경 내용 |
|---|---|---|
| 2026-03-15 | Codex | 현재 레포 기준 1차 완성본 작성 |

---

## 14. 문서 관리 규칙

1. `.env.example`, `docker-compose.yml`, `docker-compose.dev.yml`, `Jenkinsfile`, 외부 연동 설정이 바뀌면 이 문서도 같은 PR에서 함께 수정한다.
2. 실제 secret 값은 절대 문서에 적지 않고, 변수명, 용도, 저장 위치, 사용 환경만 기록한다.
3. 새 env 또는 secret을 추가하면 `.env.example`와 이 문서 인벤토리 표를 동시에 갱신한다.
4. secret 저장 위치가 바뀌면 변경 당일 바로 문서에 반영한다.
5. 배포 전에는 이 문서를 기준으로 필수 env, Jenkins Credentials, webhook 설정 누락 여부를 점검한다.
