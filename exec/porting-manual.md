# DonDone 포팅 보강 문서 (런타임/빌드/배포/DB)

본 문서는 `exec/porting-manual.md`를 보강하기 위한 상세 기술 문서다.
실제 값(비밀번호/토큰/키)은 포함하지 않고, 제품 종류/버전/설정 키/주입 위치만 정리한다.

## 1) JVM, 웹서버, WAS 종류/설정값/버전 (IDE 포함)

### 1.1 Backend JVM / WAS
| 구분 | 값 | 근거 파일 |
|---|---|---|
| JVM (빌드) | `eclipse-temurin:17-jdk` | `apps/dondone-backend/Dockerfile` |
| JVM (실행) | `eclipse-temurin:17-jre` | `apps/dondone-backend/Dockerfile` |
| Java 소스 타깃 | `Java 17` (`sourceCompatibility = 17`) | `apps/dondone-backend/build.gradle.kts` |
| WAS | Spring Boot 내장 WAS (`spring-boot-starter-web`) | `apps/dondone-backend/build.gradle.kts` |
| Spring Boot | `3.2.5` | `apps/dondone-backend/build.gradle.kts` |
| 포트 | `server.port=8080` | `apps/dondone-backend/src/main/resources/application.yml` |
| JVM 옵션 | `JAVA_OPTS="-Xms256m -Xmx512m"` | `docker-compose.yml` |

참고:
- 내장 WAS(Tomcat)의 세부 버전은 Spring Boot BOM에 의해 관리된다.
- 문서상 제출 시에는 "Spring Boot 3.2.5 + embedded WAS"로 기재하고, 정확한 Tomcat patch version은 빌드 시점 의존성 결과로 확인하는 방식이 안전하다.

### 1.2 Web 서버 / Reverse Proxy
| 구분 | 값 | 근거 파일 |
|---|---|---|
| 웹 정적 서버 이미지 | `nginx:alpine` | `apps/dondone-web/Dockerfile` |
| 운영 Reverse Proxy 이미지 | `nginx:alpine` | `docker-compose.yml` |
| 리슨 포트 | `80`, `443` | `docker-compose.yml` |
| TLS 프로토콜 | `TLSv1.2`, `TLSv1.3` | `deploy/nginx/nginx.conf` |
| 업스트림 라우팅 | `/api -> api-server:8080`, `/jenkins -> jenkins:8080`, `/ -> web:80` | `deploy/nginx/nginx.conf` |
| 본문 제한 | `client_max_body_size 20m` | `deploy/nginx/nginx.conf` |

### 1.3 Jenkins (배포 엔진)
| 구분 | 값 | 근거 파일 |
|---|---|---|
| Jenkins 이미지 | `jenkins/jenkins:lts-jdk21` | `docker-compose.yml` |
| Jenkins JVM 옵션 | `-Xms512m -Xmx1024m -Duser.timezone=Asia/Seoul` | `docker-compose.yml` |
| Jenkins 경로 prefix | `--prefix=/jenkins` | `docker-compose.yml` |

### 1.4 Mobile 빌드 런타임
| 구분 | 값 | 근거 파일 |
|---|---|---|
| Android Gradle Plugin | `com.android.application 9.1.0` | `apps/dondone-mobile/android/build.gradle.kts` |
| Kotlin Compose Plugin | `2.2.21` | `apps/dondone-mobile/android/build.gradle.kts` |
| Gradle Wrapper | `9.3.1` | `apps/dondone-mobile/android/gradle/wrapper/gradle-wrapper.properties` |
| compileSdk / minSdk / targetSdk | `36 / 26 / 36` | `apps/dondone-mobile/android/app/build.gradle.kts` |
| Java target | `17` | `apps/dondone-mobile/android/app/build.gradle.kts` |

### 1.5 IDE 버전 (저장소 기준)
| 구분 | 확인 결과 | 근거 파일 |
|---|---|---|
| IntelliJ 계열 빌드 식별자 | `IU-253.29346.240` 흔적 확인 | `.idea/workspace.xml`, `.idea/dataSources.local.xml` |
| Android Studio 버전 문자열 | 저장소 내 고정 버전 문자열 없음 (설치 경로만 확인 가능) | `apps/dondone-mobile/android/.idea/workspace.xml` |
| 프로젝트 JDK 표기 | backend: `temurin-17`, mobile: `jbr-21` | `.idea/misc.xml`, `apps/dondone-mobile/android/.idea/misc.xml` |

주의:
- IDE "정식 제품 버전명"은 저장소에서 확정되지 않으므로, 제출 직전 실제 개발자 IDE 정보로 최종 보강한다.

## 2) 빌드 시 사용되는 환경 변수 상세

## 2.1 분류
- Runtime env: backend/infra 컨테이너 실행 시 사용
- Build-time env: web 정적 번들 생성 시 사용 (`VITE_*`)
- Local-only env: 개발용 compose에서만 사용
- Mobile local/env: Android `local.properties` 또는 시스템 환경 변수

## 2.2 핵심 변수 테이블
| 변수 | 필수 여부 | 사용 시점 | 사용 컴포넌트 | 기본값/비고 | 정의/주입 위치 |
|---|---|---|---|---|---|
| `POSTGRES_DB` | 필수 | runtime | postgres, api-server datasource | 예시 `dondone` | `.env(.example)`, `docker-compose.yml`, `application.yml` |
| `POSTGRES_USER` | 필수 | runtime | postgres, api-server datasource | 없음(명시 필요) | 동일 |
| `POSTGRES_PASSWORD` | 필수/Secret | runtime | postgres, api-server datasource | 없음(명시 필요) | 동일 |
| `REDIS_PASSWORD` | 필수/Secret | runtime | redis, api-server redis | 없음(명시 필요) | `.env(.example)`, `docker-compose.yml`, `application.yml` |
| `JWT_SECRET` | 필수/Secret | runtime | backend auth | 없음(명시 필요) | `.env(.example)`, `docker-compose.yml`, `application.yml` |
| `SPRING_PROFILES_ACTIVE` | 필수 | runtime | backend profile | fallback 없음 | `.env(.example)`, `docker-compose.yml` |
| `REMITTANCE_CHAIN_MODE` | 필수 | runtime | remittance chain adapter | 예: `demo`/`sepolia` | `.env(.example)`, `docker-compose.yml`, `application.yml` |
| `REMITTANCE_CHAIN_RPC_URL` | 선택 | runtime | sepolia remittance | 빈값 가능 | 동일 |
| `VAULT_CHAIN_MODE` | 선택 | runtime | vault chain adapter | compose 기본 `demo` | 동일 |
| `VAULT_CHAIN_RPC_URL` | 선택 | runtime | vault chain | `REMITTANCE_CHAIN_RPC_URL` fallback | 동일 |
| `VITE_KAKAO_MAP_APP_KEY` | 선택 | build-time | web build | 빈값 가능(지도 fallback 동작) | `.env(.example)`, `docker-compose.yml(build.args)`, `apps/dondone-web/Dockerfile` |
| `VITE_API_BASE_URL` | 선택 | build-time | web build | 운영에서는 빈값 권장(same-origin `/api`) | 동일 |
| `KAKAO_NATIVE_APP_KEY` | 선택(기능상 필요) | mobile build/runtime init | Android Kakao Map SDK | 빈값이면 지도 미노출 fallback | `apps/dondone-mobile/android/local.properties(.example)`, `app/build.gradle.kts` |
| `DONDONE_API_BASE_URL` | 선택 | mobile build | Android backend base URL | 기본 `http://10.0.2.2:8080` | 동일 |

추가 참고:
- 전체 카탈로그와 운영 원칙은 `docs/infra/ENVIRONMENT_AND_SECRETS.md`에 정리돼 있다.

## 3) 배포 시 특이사항

### 3.1 파이프라인 핵심 동작
1. Jenkins가 `develop` 브랜치를 기준으로 서버 경로(`/srv/dondone/app`)를 동기화한다.
2. backend 테스트(`./gradlew test`) 후 `docker compose config`로 compose 유효성을 검증한다.
3. DB 마이그레이션은 `deploy/sql/*.sql` 파일을 순차 자동 적용한다.
4. `api-server`, `web` 이미지를 재빌드 후 컨테이너 재기동한다.
5. 내부 health + 외부 URL(`health`, `web`) 검증 후 성공 처리한다.

### 3.2 운영 시 유의점
- `deploy/sql/demo/*`는 자동 적용 대상이 아님(수동 실행 전용).
- `.env` 변경값은 재배포/재기동 시점에 반영되므로, 변경 후 `docker compose up` 재실행이 필요하다.
- nginx는 `--force-recreate`로 재생성하여 최신 reverse proxy/TLS 설정을 반영한다.
- Jenkins credential(`gitlab-credentials`, `mattermost-webhook`)은 `.env`가 아니라 Jenkins Credential Store에서 관리한다.

## 4) DB 접속/ERD 활용 주요 계정/프로퍼티 정의 파일 목록

아래 파일들이 DB 접속정보/ERD 기준 스키마/계정정보의 실제 근거다.

### 4.1 접속 정보/계정 변수 정의
1. `.env.example`
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD` 템플릿 정의

2. `docker-compose.yml`
- postgres 컨테이너 계정/DB명 주입
- api-server가 DB_URL/USER/PASSWORD를 환경 변수로 전달받는 경로 정의

3. `docker-compose.dev.yml`
- 로컬 개발용 DB 포트 매핑(`5433:5432`) 및 로컬 계정 주입 경로 정의

4. `apps/dondone-backend/src/main/resources/application.yml`
- datasource/redis/jwt 프로퍼티 바인딩 경로와 fallback 규칙 정의

### 4.2 ERD/스키마 기준 파일
1. `deploy/sql/*.sql`
- 운영/배포 시 자동 적용되는 스키마 마이그레이션 SQL

2. `deploy/sql/demo/*.sql`
- 데모/테스트 데이터 주입 SQL(수동)

3. `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`
- remittance 중심 텍스트 ERD 및 리팩터링 기준

4. `.idea/dataSources.xml` / `.idea/dataSources.local.xml` (선택 참고)
- IDE DB 연결 URL/계정(로컬 사용자 설정) 흔적
- 팀 표준 소스가 아니라 "개인 로컬 툴링 정보"이므로 제출문서에서는 참고용으로만 취급

## 부록: 버전 체크 빠른 명령
```bash
# backend gradle/spring/java
cat apps/dondone-backend/build.gradle.kts
cat apps/dondone-backend/gradle/wrapper/gradle-wrapper.properties

# mobile gradle/agp/sdk
cat apps/dondone-mobile/android/build.gradle.kts
cat apps/dondone-mobile/android/app/build.gradle.kts
cat apps/dondone-mobile/android/gradle/wrapper/gradle-wrapper.properties

# infra/runtime versions
cat docker-compose.yml
cat deploy/nginx/nginx.conf
cat Jenkinsfile
```
