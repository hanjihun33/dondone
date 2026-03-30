# DonDone 외부 서비스 가입/활용 정리

본 문서는 DonDone 프로젝트에서 사용하는 외부 서비스를 "가입/키 발급/주입 위치/검증 방법" 중심으로 정리한다.
실제 비밀값(키, 토큰, 비밀번호)은 포함하지 않는다.

## 1) 현재 저장소 기준 외부 서비스 사용 현황

### 1.1 사용 중
- Kakao Developers (웹/모바일 지도 키)
- Ethereum Sepolia RPC Provider (Infura/Alchemy 등, 공급자 선택형)
- DuckDNS (도메인)
- Let's Encrypt (TLS 인증서)
- Docker Hub (기본 이미지 pull)
- npm Registry / Maven Central / Gradle Distribution (코드 컴파일/의존성 다운로드)
- Mattermost Incoming Webhook (배포 알림)

## 2) 서비스별 가입/준비 정보

## 2.1 Kakao Developers (지도)
| 항목 | 내용 |
|---|---|
| 용도 | 웹/모바일 지도 표시 |
| 가입 필요 | 예 (카카오 디벨로퍼스 앱 생성) |
| 발급 항목 | JavaScript 키(웹), Native App Key(모바일) |
| 프로젝트 주입 키 | `VITE_KAKAO_MAP_APP_KEY`, `KAKAO_NATIVE_APP_KEY` |
| 주입 위치 | `.env(.example)`, `apps/dondone-web/Dockerfile`, `apps/dondone-mobile/android/local.properties` |
| 검증 포인트 | 웹 설정 지도 렌더링, 모바일 WorkProof 지도 렌더링 |

등록 체크리스트:
1. 앱 생성 후 플랫폼/도메인(웹)과 패키지/서명키(모바일) 등록
2. 웹: `VITE_KAKAO_MAP_APP_KEY` 설정 후 web 이미지 재빌드
3. 모바일: `local.properties` 또는 환경변수에 `KAKAO_NATIVE_APP_KEY` 설정

## 2.2 Sepolia RPC Provider (블록체인 송금)
| 항목 | 내용 |
|---|---|
| 용도 | `REMITTANCE_CHAIN_MODE=sepolia` 시 실제 테스트넷 트랜잭션 처리 |
| 가입 필요 | 예 (RPC 공급자 서비스 계정) |
| 발급 항목 | HTTPS RPC URL, 프로젝트/앱 키 |
| 프로젝트 주입 키 | `REMITTANCE_CHAIN_RPC_URL` (필수), 필요 시 `VAULT_CHAIN_RPC_URL` |
| 연관 키 | `REMITTANCE_TOKEN_ADDRESS`, `REMITTANCE_TREASURY_PRIVATE_KEY` 등 |
| 주입 위치 | `.env(.example)`, `docker-compose.yml`, `application.yml` |
| 검증 포인트 | backend remittance gateway가 sepolia 호출 성공, tx hash 확인 |

참고:
- 데모 안전모드에서는 `REMITTANCE_CHAIN_MODE=demo`로 운영 가능하며 외부 RPC 호출이 없다.

## 2.3 DuckDNS
| 항목 | 내용 |
|---|---|
| 용도 | 운영/시연 도메인 매핑 (`dondone.duckdns.org`) |
| 가입 필요 | 예 (DuckDNS 계정 및 토큰) |
| 발급 항목 | 서브도메인, update token |
| 프로젝트 반영 위치 | `deploy/nginx/nginx.conf`의 `server_name` |
| 검증 포인트 | `https://dondone.duckdns.org/`, `/health` 접근 성공 |

## 2.4 Let's Encrypt
| 항목 | 내용 |
|---|---|
| 용도 | HTTPS 인증서 발급/갱신 |
| 가입 필요 | 별도 가입 필수 아님(인증서 발급 클라이언트 필요) |
| 준비 항목 | 도메인 소유/연결, 인증서 경로 확보 |
| 프로젝트 반영 위치 | `deploy/nginx/nginx.conf` + host `/etc/letsencrypt` 마운트 (`docker-compose.yml`) |
| 검증 포인트 | TLS 접속 성공, 인증서 만료일 확인 |

## 2.5 Mattermost Webhook
| 항목 | 내용 |
|---|---|
| 용도 | Jenkins 배포 성공/실패 알림 |
| 가입 필요 | 예 (워크스페이스/채널 + Incoming Webhook) |
| 발급 항목 | Webhook URL |
| 프로젝트 주입 위치 | Jenkins Credentials (`mattermost-webhook`) |
| 코드 반영 위치 | `Jenkinsfile`의 `withCredentials` + `curl` 전송 |
| 검증 포인트 | 배포 완료/실패 시 채널 메시지 수신 |

## 2.6 코드 컴파일/빌드 외부 서비스
| 서비스 | 가입 필요 | 용도 | 근거 |
|---|---|---|---|
| Gradle Distribution (`services.gradle.org`) | 보통 불필요 | Gradle wrapper 다운로드 | `gradle-wrapper.properties` |
| Maven Central | 보통 불필요 | Java 의존성 다운로드 | `apps/dondone-backend/build.gradle.kts` |
| npm Registry | 보통 불필요 | Web npm 패키지 설치 (`npm ci`) | `apps/dondone-web/package.json`, Dockerfile |
| Docker Hub | 보통 불필요(기본 pull) | base image pull | `docker-compose.yml`, 각 Dockerfile |

주의:
- 사내망/사설 레지스트리 환경이면 인증이 필요할 수 있다.

## 3) 외부 서비스 정보 관리 원칙
- 실제 Secret 값은 Git 저장소에 저장하지 않는다.
- `.env.example`에는 변수명/형식만 둔다.
- 운영 Secret은 `/srv/dondone/app/.env` 또는 Jenkins Credentials에 저장한다.
- 문서에는 "키 이름, 용도, 주입 위치, 검증 방법"만 기록한다.

## 4) 빠른 점검 체크리스트
1. 지도 기능 필요 시 `VITE_KAKAO_MAP_APP_KEY`, `KAKAO_NATIVE_APP_KEY` 설정 완료
2. sepolia 실연동 필요 시 `REMITTANCE_CHAIN_MODE=sepolia` + `REMITTANCE_CHAIN_RPC_URL` 설정 완료
3. 도메인/TLS 확인: `dondone.duckdns.org` + Let's Encrypt 인증서 정상
4. Jenkins Credentials에 `mattermost-webhook` 등록 완료
5. 빌드 서버에서 Docker Hub / npm / Gradle / Maven Central 접근 가능
