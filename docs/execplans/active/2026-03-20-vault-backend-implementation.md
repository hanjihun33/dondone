# Source Inputs
- 사용자 요구: `origin/feature/blockchain`의 vault/defi 참고 구현을 바탕으로 현재 `develop`에 DonDone vault 기능 추가
- PRD 근거:
  - `docs/DonDone_PRD_v1.5.md` 3.1 Demo MVP의 `보관/이자(데모 시뮬레이션)`
  - `docs/DonDone_PRD_v1.5.md` 4장 P0/P1 확정표의 `보관/이자`
  - `docs/DonDone_PRD_v1.5.md` 6.7 `보관/이자(데모 시뮬레이션)`
  - `docs/DonDone_PRD_v1.5.md` 7G `보관/이자(Vault, 데모 시뮬레이션)`
  - `docs/DonDone_PRD_v1.5.md` 10.4 비동기 잡 설계
  - `docs/DonDone_PRD_v1.5.md` 12.3 보관/이자 안내 문구
- 현재 백엔드 구조 탐색:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/api/RemittanceController.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/RemittanceJobWorker.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobType.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobReferenceKind.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `apps/dondone-backend/src/main/resources/application.yml`
- 참고 브랜치 탐색:
  - `origin/feature/blockchain:apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/**`
  - `origin/feature/blockchain:apps/dondone-backend/src/test/java/com/workproofpay/backend/vault/VaultIntegrationTest.java`
  - `origin/feature/blockchain:apps/dondone-blockchain/src/StableVault.sol`
  - `origin/feature/blockchain:apps/dondone-blockchain/doc/vault-v1-design.md`
- 모바일 영향 탐색:
  - `apps/dondone-mobile/mockup/mockup.boot.js`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`

# Goal
- DonDone 백엔드에 P0 범위의 `Vault` 기능을 추가한다.
- 사용자는 서버 관리형 remittance 지갑의 테스트 토큰을 `보관(vault deposit)`하고, 다시 `출금(withdraw)`하며, `예상 이자`와 `거래 상태`를 확인할 수 있어야 한다.
- 구현은 testnet/demo 범위에 머물고, 실제 수익 보장이나 실자금 정산처럼 보이지 않게 한다.

# In Scope
- 인증 사용자 대상 vault backend API 추가
  - 요약 조회
  - 예치 요청 생성
  - 출금 요청 생성
  - 거래 목록 조회
  - 거래 상세 조회
- `vault` feature-first 패키지 추가
  - `api/service/repo/model/adapter/config`
- vault 전용 엔티티/저장소 추가
  - 포지션
  - 거래
  - 수익 로그
- vault chain adapter 추가
  - demo 모드 필수
  - 현재 remittance 구조와 호환되는 sepolia/testnet 확장 지점 유지
- vault 비동기 잡 처리 추가
  - submit
  - receipt poll
- 공통 jobs enum/reference kind 확장
- vault 설정값 및 테스트 설정 추가
- 최소 통합 테스트 추가
  - 지갑 생성 후 예치/출금/조회 흐름
  - 지갑 없음/잔액 부족/중복 키 재호출 검증

# Out of Scope
- 모바일 Android/mockup 화면 구현 또는 API 연결
- 메인넷 연동
- 실제 외부 DeFi 전략 운용
- 실수익 계산, 법적/금융적 수익 보장 표현
- 현금 입금 후 자동 스왑/온램프 연계
- Proof Pack, Claim Kit, Wage, Advance 로직 확장
- `apps/dondone-blockchain/backend` 복구/이식

# Affected Modules
## Backend
- 신규:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/**`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/vault/**`
- 수정:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/DonDoneBackendApplication.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobType.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobReferenceKind.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/repo/JobRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
  - `apps/dondone-backend/src/main/resources/application.yml`
  - `apps/dondone-backend/src/test/resources/application-test.yml`
  - 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/DemoRemittanceBlockchainGateway.java`
  - 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`

## Mobile
- 즉시 변경 없음
- 후속 backend 연결 후보:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeUiModel.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/feature/finance/presentation/FinanceHomeScreen.kt`
  - `apps/dondone-mobile/mockup/mockup.boot.js`
- 현재 모바일은 vault 표시가 mock/demo 계산 기반이라 이번 턴에서 backend-only로 분리 가능

## Docs
- `docs/execplans/active/2026-03-20-vault-backend-implementation.md`
- 필요 시 후속 API/계약 변경 메모

## Shared
- jobs 공통 인프라
- JWT 보호 API 경로
- 공통 에러 응답 규약

# Contract Changes
- 신규 API 경로
  - `GET /api/vault/summary`
  - `POST /api/vault/deposits`
  - `POST /api/vault/withdrawals`
  - `GET /api/vault/transactions`
  - `GET /api/vault/transactions/{vaultTransactionId}`
- 신규 요청/응답 DTO
  - deposit/withdraw create request
  - create response
  - summary response
  - transaction list/detail response
- 신규 DB 엔티티/테이블
  - `vault_positions`
  - `vault_transactions`
  - `vault_yield_logs`
- 공통 jobs 계약 확장
  - `JobReferenceKind.VAULT`
  - `JobType.SUBMIT_VAULT_TRANSACTION`
  - `JobType.POLL_VAULT_TRANSACTION_RECEIPT`
- 기존 remittance 지갑 contract 재사용
  - vault는 remittance `UserWallet`의 주소/암호화 private key를 사용
- idempotency contract
  - 예치/출금 생성 요청은 `Idempotency-Key` 헤더를 필수로 요구

# Security Notes
- `/api/vault/**`는 기존 remittance와 동일하게 JWT 인증이 필수다.
- `SecurityConfig`의 permitAll 목록에는 vault 경로를 추가하지 않는다.
- 서버 관리형 지갑 private key 복호화는 기존 `WalletService` 경로를 재사용하되, controller에 노출하지 않는다.
- 환경 변수 기반 설정만 사용한다.
  - vault 주소
  - token 주소
  - rpc url
  - 체인 모드
- PRD 12.3에 맞춰 `테스트넷 데모 기준 예상값`, `실제 수익 보장 아님` 문구를 API 응답/모바일 후속 소비에서 유지할 수 있어야 한다.

# Maintainability Notes
- remittance와 유사하더라도 vault 도메인은 별도 feature 패키지로 분리한다.
- remittance 전송 로직을 일반화하려는 광범위 리팩터는 이번 범위에서 제외한다.
- jobs는 공통 인프라를 재사용하되 `reference_kind`와 `job_type`만 확장한다.
- 참고 브랜치의 `Recipient`/`Transfer` 관계 재정비, 별도 blockchain backend 삭제 이력은 이번 작업에 섞지 않는다.
- demo 계산과 chain adapter를 분리해 이후 mobile/backend 연결 시 contract drift를 줄인다.

# Implementation Steps
1. PRD와 참고 브랜치 기준으로 vault backend 범위를 확정하고 active execplan을 유지한다.
2. 현재 remittance/jobs/security 구조에 맞춰 vault domain 설계를 정리한다.
3. `vault` 패키지와 DTO/엔티티/저장소를 추가한다.
4. `VaultBlockchainGateway`와 demo adapter를 먼저 구현하고, 필요한 경우 sepolia adapter를 참고 브랜치에서 최소 이식한다.
5. `VaultService`에 요약/예치/출금/조회/예상 이자 계산을 구현한다.
6. `VaultController`를 추가하고 Bean Validation, OpenAPI, idempotency 처리 규칙을 연결한다.
7. jobs 공통 enum과 `VaultJobWorker`를 추가해 submit/poll 비동기 처리를 연결한다.
8. `application.yml`, `application-test.yml`, `DonDoneBackendApplication`을 업데이트한다.
9. `ErrorCode`를 확장하고 실패 시나리오를 공통 예외 응답으로 정리한다.
10. 통합 테스트와 필요한 단위 테스트를 추가한다.
11. 검증 후 모바일 후속 연결에 필요한 contract note를 정리한다.

# Test Plan
- 우선 실행
  - `cd apps/dondone-backend && ./gradlew test --tests '*Vault*' --tests '*WalletServiceTest'`
- 범위 확대
  - `cd apps/dondone-backend && ./gradlew test --tests '*Remittance*'`
- 최소 검증 항목
  - wallet 생성 후 summary 조회
  - deposit 생성 -> 잡 처리 -> 상세 조회 confirmed
  - withdrawal 생성 -> 잡 처리 -> 요약값 반영
  - wallet 없음 시 `WALLET_NOT_FOUND`
  - 잔액 부족 시 conflict 에러
  - `Idempotency-Key` 누락/재사용 payload mismatch 검증
  - list/detail 인증 사용자 격리 검증
- 환경 blocker가 있으면 Java/Docker/Testcontainers 여부를 명시한다.

# Review Focus
- PRD의 `보관/이자 = 데모 시뮬레이션` 제약을 지켰는가
- controller가 얇고 domain/service 경계가 유지됐는가
- remittance 공유 지갑을 사용하면서 보안 노출이 늘지 않았는가
- jobs 확장이 remittance 기존 흐름을 깨지 않는가
- DTO/API/DB 계약이 일관되고 idempotency가 안전한가
- 모바일 후속 연결 시 필요한 정보(잔액, 예상 이자, 상태, disclaimer)가 응답에 충분한가

# Worktree Split Decision
- Single lane

`vault` 추가는 shared jobs, JWT 보호 API, remittance wallet 재사용, 신규 DTO/엔티티/응답 계약이 동시에 움직인다. 공유 auth/security 규칙과 공통 jobs enum이 변경되므로 병렬 worktree 분할은 충돌 위험이 높고 리뷰 비용만 늘린다.

# Commit Plan
- 1차: `feat: vault backend skeleton 추가`
- 2차: `test: vault backend 통합 테스트 추가`
- 필요 시 3차: `docs: vault backend 실행 계획 정리`

# Open Questions
- 현재 턴에서 sepolia adapter까지 실제 연결 가능한 수준으로 넣을지, demo adapter만 필수로 둘지
- 모바일/API 연결은 이번 작업 뒤 후속으로 분리할지

# Assumptions
- 이번 작업의 1차 목표는 backend 계약과 동작 구현이며 모바일 코드는 즉시 변경하지 않는다.
- vault는 PRD 기준 `보관/이자(데모 시뮬레이션)`로 구현하고 실제 수익 보장 기능은 넣지 않는다.
- 현재 remittance `UserWallet` 구조와 `WalletService.getDecryptedPrivateKey()`를 vault에서도 재사용한다.
- 기존 remittance API/동작 회귀가 없어야 하므로 불필요한 schema/refactor는 제외한다.
