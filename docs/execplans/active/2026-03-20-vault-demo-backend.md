## Source Inputs
- `docs/DonDone_PRD_v1.5.md`
  - `3.1 Demo MVP에서 반드시 보여줄 것(P0)`
  - `4. 범위(P0/P1) 확정표`
  - `7G. 보관/이자(Vault)`
  - `12.3 보관/이자 안내 문구(필수)`
- `origin/feature/blockchain` 참고 구현
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/VaultJobWorker.java`
  - `apps/dondone-blockchain/src/StableVault.sol`
  - `apps/dondone-blockchain/doc/vault-v1-design.md`
- 현재 develop 구조 탐색
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/SecurityConfig.java`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`

## Goal
참고 `origin/feature/blockchain` 계약을 기준으로 현재 backend 구조에 testnet/demo `vault`를 신규 추가한다. 인증 사용자가 remittance 지갑 잔액을 기준으로 보관 요약, 예치 요청, 출금 요청, 거래 상태 조회를 할 수 있어야 하며, 예상 이자는 demo 추정값으로만 제공한다.

## In Scope
- backend `vault` feature-first 모듈 신규 추가
- `GET /api/vault/summary`
- `POST /api/vault/deposits`
- `POST /api/vault/withdrawals`
- `GET /api/vault/transactions`
- `GET /api/vault/transactions/{vaultTransactionId}`
- vault 비동기 tx 제출/receipt polling worker 추가
- remittance 지갑/개인키/잔액 재사용
- vault 설정값(`vault.policy`, `vault.worker`, `vault.chain`) 추가
- vault 엔티티/리포지토리/에러코드/테스트 추가
- `apps/dondone-blockchain`에 `StableVault.sol` 및 기본 Foundry 테스트 추가

## Out of Scope
- Android/mobile 화면 연결
- 실제 메인넷 yield 파트너 연동
- fiat 온램프/정산/실수익 계산
- real-money settlement behavior
- 기존 `apps/dondone-blockchain/backend` 정리 또는 구조 개편
- remittance unrelated refactor

## Affected Modules
### Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/service/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/repo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/model/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/adapter/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/config/VaultProperties.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobReferenceKind.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/JobType.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/service/VaultJobWorker.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/DonDoneBackendApplication.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/src/test/resources/application-test.yml`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/vault/VaultIntegrationTest.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/WalletServiceTest.java`

### Mobile
- 이번 작업에서는 직접 수정하지 않음
- 후속으로 finance/vault 화면이 backend contract에 맞춰 연결 필요

### Docs
- 본 execplan
- `docs/DonDone_P0_API_Contract_v0.md`
- `docs/DonDone_P0_Functional_Spec_v0.md`

### Shared
- JWT 인증/`AuthenticatedUser` 재사용
- `jobs` 테이블 enum/check 제약 호환성 확인 필요
- `ApiResponse` 및 OpenAPI 패턴 재사용

## Contract Changes
- 신규 인증 API 추가
  - `GET /api/vault/summary`
  - `POST /api/vault/deposits`
  - `POST /api/vault/withdrawals`
  - `GET /api/vault/transactions`
  - `GET /api/vault/transactions/{vaultTransactionId}`
- `POST` 요청은 `Idempotency-Key` 헤더를 필수로 사용
- 신규 DTO
  - `CreateVaultTransactionRequest`
  - `CreateVaultTransactionResponse`
  - `VaultSummaryResponse`
  - `VaultInterestPreviewResponse`
  - `VaultTransactionListResponse`
  - `VaultTransactionItemResponse`
  - `VaultTransactionDetailResponse`
- 신규 JPA 테이블/엔티티
  - `vault_positions`
  - `vault_transactions`
  - `vault_yield_logs`
- `jobs` 도메인 계약 확장
  - `JobReferenceKind.VAULT`
  - `JobType.SUBMIT_VAULT_TRANSACTION`
  - `JobType.POLL_VAULT_TRANSACTION_RECEIPT`

## Security Notes
- 모든 vault API는 JWT 인증 필요. `SecurityConfig`의 permitAll 확장은 하지 않는다.
- 서버 저장 encrypted private key를 복호화해 vault tx를 서명하므로 remittance와 동일한 민감도 경로로 취급한다.
- `Idempotency-Key` 누락 및 다른 payload 재사용은 명시적으로 차단한다.
- 동일 사용자 active vault tx는 1건으로 제한해 중복 예치/출금 충돌을 막는다.
- 요약/상세 응답에는 private key나 내부 signed transaction을 절대 노출하지 않는다.
- 안내 문구는 `테스트넷 데모 기준 예상값`, `실제 수익 보장 아님`을 포함해야 한다.

## Maintainability Notes
- remittance와 vault는 지갑/체인 연동 패턴을 공유하되, 정책/상태 모델은 별도 모듈로 분리한다.
- worker 구현은 remittance 패턴을 따라가되 vault 전용 failure code와 share 반영 책임을 분리한다.
- `jobs` 확장은 현재 active key/reference kind 규칙을 유지하는 선에서 최소 변경으로 처리한다.
- mobile 계약이 아직 움직일 수 있으므로 backend 응답은 명확한 수치 필드와 disclaimer를 포함하되, 화면 전용 조합 필드는 피한다.

## Implementation Steps
1. PRD 및 참고 브랜치 기준으로 vault backend 계약을 확정한다.
2. `docs/execplans/active/2026-03-20-vault-demo-backend.md`를 기준 문서로 유지한다.
3. `vault` 패키지에 model/repo/service/api/adapter/config 골격을 추가한다.
4. `jobs` 도메인에 vault reference kind/type을 추가하고 vault worker를 구현한다.
5. `WalletService`/demo chain gateway와의 연동 지점을 추가해 demo mode에서 deposit/withdraw state를 시뮬레이션한다.
6. `application.yml`과 `application-test.yml`에 vault 설정을 추가한다.
7. `ErrorCode`와 validation 규칙을 보강한다.
8. Postgres integration test로 요약/예치/출금/idempotency/잔액 차감/상태 전이를 검증한다.
9. `apps/dondone-blockchain`에 `StableVault.sol`과 기본 Foundry 테스트를 추가한다.
10. 검증 결과와 후속 mobile 계약 포인트를 정리한다.

## Test Plan
- backend unit/regression
  - `cd apps/dondone-backend && ./gradlew test --tests '*WalletServiceTest'`
- backend integration
  - `cd apps/dondone-backend && ./gradlew integrationTest --tests '*VaultIntegrationTest'`
- 가능하면 관련 회귀
  - `cd apps/dondone-backend && ./gradlew test --tests '*RemittanceIntegrationTest'`
- contract
  - `cd apps/dondone-blockchain && forge test --match-path test/StableVault.t.sol`
- Docker/Foundry 부재 시 blocker를 명시한다.

## Review Focus
- PRD의 demo-only/disclaimer가 API contract와 설정값에 반영됐는지
- 동일 사용자 중복 active tx 차단과 idempotency replay가 안전한지
- vault share/principal/yield 상태 전이가 예치/출금 후 일관적인지
- remittance 지갑/잔액 로직 재사용이 기존 송금 플로우를 깨지 않는지
- `jobs` 확장이 현재 schema/check constraint와 충돌하지 않는지
- signed transaction, private key, 내부 오류 메시지 노출이 없는지

## Worktree Split Decision
Single lane

현재 작업은 shared DTO, `jobs` 도메인, wallet/security, backend contract, blockchain contract 초안이 함께 움직인다. 공유 엔티티와 공통 응답 계약이 동시에 변하므로 병렬 lane으로 나누면 충돌 위험이 높다.

## Commit Plan
- `feat: vault demo backend API 추가`
- `test: vault integration test 추가`
- 필요 시 `feat: stable vault contract 초안 추가`

## Open Questions
- mobile backend 연결을 같은 턴에 일부라도 맞출지 여부
- `jobs` 테이블에 PostgreSQL check constraint가 이미 존재하는 환경에서 별도 초기화 코드가 필요한지 여부
- sepolia 실연동까지 같은 범위에 포함할지 여부

## Assumptions
- 이번 작업은 backend-first 구현이며 mobile 수정은 후속으로 둔다.
- vault는 testnet/demo 비동기 기능이며 실제 수익 보장이나 실정산은 구현하지 않는다.
- remittance 지갑과 encrypted private key 재사용은 허용된 현재 구조로 본다.
- JPA `ddl-auto=update` 또는 테스트의 `create-drop`로 신규 엔티티 스키마를 반영할 수 있다고 가정한다.
