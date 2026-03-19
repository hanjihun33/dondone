# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- PRD:
  - `docs/DonDone_PRD_v1.5.md` 7E, 7F, 10.3, 10.4, 10.5, 10.6, 12.5
- 참고 구현:
  - `apps/dondone-blockchain/src/SafePayRemittance.sol`
  - `apps/dondone-blockchain/backend/src/main/java/com/workproofpay/remittance/**`
- 현재 DonDone 백엔드 기준:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/security/JwtAuthenticationFilter.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/safepay/api/SafePayController.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/**`

# Goal
DonDone 백엔드에 P0 테스트넷 송금 기능을 구현한다. 정책과 상태는 백엔드/DB가 관리하고, Sepolia의 데모 ERC20 전송은 adapter가 수행한다. 범위는 서버 지갑 생성/조회, 잔액 조회, 외부 지갑 기반 수신자 허용목록, precheck, 송금 요청, 송금 목록/상세, 비동기 전송 상태 갱신까지이며, 영수증 PDF는 제외한다.

# In Scope
- `remittance` 모듈 실구현
  - `POST /api/remittance/wallets/me`
  - `GET /api/remittance/wallets/me`
  - `GET /api/remittance/wallets/me/balance`
  - `GET /api/remittance/recipients`
  - `POST /api/remittance/recipients`
  - `PUT /api/remittance/recipients/{recipientId}`
  - `POST /api/remittance/transfers/precheck`
  - `POST /api/remittance/transfers`
  - `GET /api/remittance/transfers`
  - `GET /api/remittance/transfers/{transferId}`
- JWT 사용자 기준 소유권 검증
- 서버 지갑 생성 및 암호화 저장
- treasury가 사용자 지갑 생성 시 초기 gas + demo token 지급
- SafePay 최소 정책
  - 허용목록 외 송금 금지
  - 신규/수정 수신자 soft confirmation 필요 플래그
  - 고액 확인 (`100 dUSDC` 이상)
  - `Idempotency-Key` 중복 방지
  - 자기 자신 송금 금지
  - 주소 형식 검증
- 사용자별 전송 직렬화
- `jobs` 테이블과 스케줄러 기반 비동기 전송 / 영수증 폴링
- JPA 자동 스키마 생성 기준 설정 키와 통합테스트 추가
- transfer 비동기 상태머신 명확화
  - `REQUESTED -> SIGNED -> BROADCASTED -> CONFIRMED`
  - terminal state: `FAILED`, `TIMED_OUT`
  - 잘못된 상태 전이 차단
- 운영/복구 API
  - failed/stuck transfer 조회
  - failed job 조회
  - 관리자 재처리 API
  - wallet funding retry API
- 운영 요약 API
  - transfer/job/wallet 상태별 집계
  - 최근 실패 원인 요약
- 설계 의도를 검증하는 단위/통합 테스트
- 짧은 async remittance 설계 문서
- Swagger에서 바로 검증 가능한 remittance OpenAPI 설명/예시 보강

# Out of Scope
- SafePay 정책의 온체인 상태 머신화
- `SafePayRemittance.sol` 직접 연동
- 영수증 PDF / presigned URL / documents 연동
- 실제 실자금 토큰, 메인넷, yield/vault
- 모바일 화면 구현
- 별도 운영자 콘솔 UI

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/DonDoneBackendApplication.java`
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`

## Mobile
- 직접 구현 범위 없음
- 후속으로 모바일이 사용할 계약 문서 / 응답 shape만 고정

## Docs
- `docs/execplans/active/2026-03-18-remittance-backend-sepolia.md`
- 필요 시 PRD deviation 메모 또는 API 계약 문서 후속 반영

## Shared
- `ApiResponse<T>` envelope 유지
- JWT 인증 사용자 추출 방식 재사용
- `Idempotency-Key` 규칙 재사용

# Contract Changes
- 신규 요청 DTO
  - `UpsertRecipientRequest`
  - `TransferPrecheckRequest`
  - `CreateTransferRequest`
- 신규 응답 DTO
  - `WalletResponse`
  - `WalletBalanceResponse`
  - `RecipientListResponse`
  - `RecipientItemResponse`
  - `TransferPrecheckResponse`
  - `CreateTransferResponse`
  - `TransferListResponse`
  - `TransferDetailResponse`
- 운영 응답 DTO
  - `RemittanceOpsSummaryResponse`
  - `RemittanceOpsTransferListResponse`
  - `RemittanceOpsJobListResponse`
  - `RemittanceAdminActionResponse`
- soft confirmation 계약
  - precheck / create 응답에 `requiresRecentRecipientConfirmation` 또는 동등 의미 필드 포함
  - hard block 대신 안내용 플래그/타임스탬프 제공
- wallet 생성 계약
  - `POST /wallets/me` 는 create-or-return 으로 동작
  - 신규 생성 시 `201`, 기존 지갑 재조회 시 `200`
- 관리자 운영 계약
  - `/api/admin/remittance/**` 는 `ADMIN` role 전용
  - `POST /api/admin/remittance/transfers/{transferId}/retry`
  - `POST /api/admin/remittance/wallets/{userId}/retry-funding`
  - `GET /api/admin/remittance/transfers`
  - `GET /api/admin/remittance/jobs`
  - `GET /api/admin/remittance/summary`
- DB schema 추가
  - `user_wallets`
  - `recipients`
  - `transfers`
  - `jobs`
- error code 추가 후보
  - `RECIPIENT_NOT_FOUND`
  - `RECIPIENT_NOT_ALLOWED`
  - `RECENT_RECIPIENT_CONFIRMATION_REQUIRED`
  - `TRANSFER_NOT_FOUND`
  - `HIGH_AMOUNT_CONFIRMATION_REQUIRED`
  - `SELF_TRANSFER_NOT_ALLOWED`
  - `WALLET_NOT_FOUND`
  - `WALLET_FUNDING_FAILED`
  - `INSUFFICIENT_WALLET_BALANCE`
  - `TRANSFER_ALREADY_IN_PROGRESS`

# Security Notes
- 모든 remittance endpoint는 JWT 필요, `permitAll` 추가 없음
- 운영 endpoint는 JWT + `ADMIN` role 필요
- private key는 AES-GCM 등 대칭키로 암호화 저장, 환경변수 기반 키 필요
- treasury private key / RPC URL / token address / token decimals / gas/top-up 값은 설정으로 주입
- 기본 운영 파라미터
  - user wallet seed: `0.01 Sepolia ETH`, `500 dUSDC`
  - transfer retry: 자동 재시도 `2회` 후 `FAILED`
- 주소 검증과 자기 자신 송금 금지를 controller/service 경계에서 강제
- 타 사용자 수신자/송금 조회는 404 은닉
- idempotency replay 시 payload mismatch 차단
- 초기 funding과 실제 전송 로그에 private key 노출 금지

# Maintainability Notes
- 정책 계산은 `safepay` 별도 컨트롤러로 확장하지 말고 `remittance.service` 내부 정책 서비스 한 곳에서 소유한다.
- 체인 연동은 `adapter` 뒤로 숨기고 서비스 계층이 web3j 타입을 직접 알지 않게 유지한다.
- sample blockchain backend의 DTO/구조를 참고하되 DonDone의 `ApiResponse`, `AuthenticatedUser`, JPA 자동 스키마 생성 기준에 맞게 재구성한다.
- job worker와 transfer service 사이의 상태 전이는 enum과 전용 메서드로만 바꾸고, 문자열 하드코딩을 늘리지 않는다.
- 운영 API는 기존 사용자용 remittance controller와 분리해 `/api/admin/remittance/**` 로 소유권을 분리한다.
- 상태 전이는 `Transfer` 엔티티가 단일 소유권을 갖고 worker/service/controller가 직접 필드를 덮어쓰지 않는다.

# Implementation Steps
1. 실행계획 고정 및 JPA 개발 모드 기준 범위 확정
2. remittance 설정 클래스 / enum / entity / repository 뼈대 추가
3. JPA entity 기준으로 `user_wallets`, `recipients`, `transfers`, `jobs` 스키마 자동 생성 구성
4. wallet crypto, wallet service, Sepolia ERC20 adapter, treasury funding adapter 추가
5. remittance policy service 구현
   - allowlist
   - recent-recipient soft confirm
   - high amount confirm
   - self transfer ban
   - balance check
   - in-flight transfer serialization
6. remittance application service 구현
   - wallet create/get/balance
   - recipient upsert/list
   - precheck
   - create transfer with idempotency
   - transfer list/detail
7. jobs worker 구현
   - `SUBMIT_TRANSFER`
   - `POLL_TRANSFER_RECEIPT`
   - retry / failover
8. transfer 상태머신 세분화
   - `SIGNED`, `BROADCASTED`, `TIMED_OUT`
   - invalid transition guard
9. 운영/복구 서비스 및 관리자 controller 추가
10. controller / DTO / error code / OpenAPI 노출 정리
11. 상태머신/복구/운영 API 테스트 추가
12. async remittance 설계 문서 작성
13. `./gradlew test` 실행 및 결과 정리

# Current Status
- remittance P0 API, wallet/recipient/transfer domain, async worker, demo/sepolia adapter 구현 완료
- `precheck` 는 더 이상 지갑 생성/초기 funding을 유발하지 않음
- submit worker 는 signed tx 를 먼저 저장하고 같은 tx 를 재방송하는 방식으로 중복 전송 위험을 완화함
- 수신자 생성은 `POST`, 수정은 기존 대상 `PUT` 만 허용하도록 정리됨
- 기본 logging 은 민감한 JDBC bind 값을 남기지 않는 수준으로 낮춤
- 지갑 funding 상태(`PENDING/FUNDED/FAILED`)를 wallet 응답과 재시도 플로우에 반영함
- receipt polling 은 timeout 이후 dropped tx 를 `FAILED` 로 정리해 무한 `SUBMITTED` 상태를 피함
- 다음 단계로 explicit transfer 상태머신, 운영 조회, 관리자 재처리 API, async remittance 설계 문서를 추가 예정

# Test Plan
- `cd apps/dondone-backend && ./gradlew test`
- 통합테스트 추가 대상
  - JWT 없이 remittance 접근 시 401
  - 지갑 생성/재조회 idempotent 동작
  - 외부 주소 수신자 등록/목록 조회
  - 허용목록 외 송금 차단
  - 최근 수정 수신자 precheck soft confirmation 플래그
  - 고액 확인 누락 차단
  - 동일 `Idempotency-Key` replay / mismatch
  - 타 사용자 수신자/송금 404 은닉
  - 송금 생성 후 job worker가 상태를 `SUBMITTED`/`CONFIRMED` 또는 `FAILED`로 갱신
  - 상태머신 전이 검증 (`REQUESTED -> SIGNED -> BROADCASTED -> ...`)
  - timeout 후 `TIMED_OUT`
  - 관리자 운영 API 권한/계약
  - 관리자 retry action 이 적절한 job/state 를 생성하는지
  - self transfer / insufficient balance 차단
  - 실패 receipt 시 잔액 불변
  - 사용자별 동시 송금 요청 직렬화
- 범위가 커서 필요한 경우 일부 서비스 단위 테스트를 추가

# Sepolia Smoke Checklist
- treasury private key, wallet encryption key, token address, RPC URL 이 모두 주입된 상태에서 `POST /api/remittance/wallets/me` 실행
- 생성된 지갑의 native/token 잔액이 기대값에 도달하는지 확인
- 소액 송금 1건 생성 후 worker 실행 또는 스케줄 대기
- tx hash 가 기록되고 receipt poll 이후 `CONFIRMED` 로 수렴하는지 확인
- 동일 signed tx 재방송 시 `already known` 류 응답이 정상 허용되는지 확인

# Review Focus
- PRD P0 범위를 넘어서 실자금/메인넷 기능으로 새지 않았는지
- remittance 정책이 controller, service, worker에 분산되지 않았는지
- entity/enum과 실제 JPA 자동 생성 테이블 shape가 의도와 맞는지
- treasury funding, sender transfer, receipt polling에서 비밀키/민감정보 로그가 없는지
- soft confirmation이 hard block으로 남지 않았는지
- `ApiResponse` envelope와 HTTP status가 DonDone 기존 패턴과 일치하는지
- 관리자 운영 endpoint가 일반 사용자 endpoint와 충돌하지 않는지
- 상태 전이 메서드가 invalid transition을 조용히 허용하지 않는지

# Worktree Split Decision
- Single lane

DTO, auth/security, migration, shared response envelope, async state machine이 동시에 움직이는 작업이다. 공통 계약이 아직 고정 중이므로 병렬 lane으로 나누면 충돌 가능성이 높다.

# Commit Plan
- `docs`: remittance backend 실행계획
- `backend`: remittance migration + domain + adapter + jobs + API
- `test`: remittance integration tests 및 설정 보강

# Open Questions
- 없음. 구현 기준은 사용자와 합의됨.

# Assumptions
- 네트워크는 `Sepolia` 고정이며, 데모 토큰은 treasury가 보유한 ERC20이다.
- 최근 수신자 확인은 hard cooldown 대신 안내/확인 플래그로 처리한다.
- 토큰은 USDC 스타일 `6 decimals`를 사용한다.
- 영수증 PDF와 문서 저장은 이번 slice에서 제외한다.
- 모바일 계약 문서는 이번 구현에서 코드와 테스트가 사실상 source of truth 역할을 한다.
