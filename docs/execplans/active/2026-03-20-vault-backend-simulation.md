# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/prd-breakdown/SKILL.md`
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
  - `.agents/skills/test-checklist/SKILL.md`
- PRD / 계약 문서:
  - `docs/DonDone_PRD_v1.5.md`
  - `docs/DonDone_P0_Functional_Spec_v0.md`
  - `docs/DonDone_P0_API_Contract_v0.md`
- 현재 구현 기준:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`
  - `apps/dondone-backend/src/main/resources/application.yml`
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/**`
- 참고 구현:
  - `origin/feature/blockchain:apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/**`
  - `origin/feature/blockchain:apps/dondone-backend/src/test/java/com/workproofpay/backend/vault/VaultIntegrationTest.java`
  - `origin/feature/blockchain:apps/dondone-blockchain/src/StableVault.sol`
  - `origin/feature/blockchain:apps/dondone-blockchain/doc/vault-v1-design.md`
- 모바일 참고:
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/model/DemoModels.kt`
  - `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/domain/calculator/VaultCalculator.kt`

# Goal
DonDone P0 범위의 `Vault` 기능을 현재 `develop` 백엔드에 추가한다. 외부 계약은 PRD와 현재 P0 계약 문서에 맞춰 `보관/이자 시뮬레이션`으로 구현하고, 참고 브랜치의 `vault` 구조는 내부 도메인 분리, 요약 응답 구성, 예상 이자 계산, remittance 연계 패턴을 선택적으로 재사용한다.

# In Scope
- 백엔드 `vault` feature 패키지 신설
- `GET /api/vault/summary`
- `POST /api/vault/allocations`
- `POST /api/vault/releases`
- 보관 잔액 / 보관 가능 금액 / 송금 가능 금액 / 예상 이자 계산
- remittance wallet balance와 연계한 시뮬레이션 잔액 해석
- Vault 전용 엔티티 / repository / service / DTO 추가
- Vault 관련 validation / error code / config 추가
- Vault backend integration test / unit regression 추가
- 필요 시 P0 계약 문서와 실제 구현 차이 메모 보강

# Out of Scope
- 실제 온체인 예치 / 출금 트랜잭션 전송
- `StableVault.sol` 배포 또는 on-chain 연동 강제
- `VaultJobWorker`, tx receipt polling, tx hash 기반 상태 추적
- `GET /api/vault/transactions/**` 외부 노출
- 모바일 / 웹 화면 구현 변경
- 메인넷 / 실수익 / 파트너 yield 연동
- 실자금 정산 또는 투자성 표현

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/vault/**` 신규
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/adapter/DemoRemittanceBlockchainGateway.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/ErrorCode.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/DonDoneBackendApplication.java`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/config/OpenApiConfig.java`
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/src/test/resources/application-test.yml`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/vault/**` 신규

## Mobile
- 즉시 구현 범위 아님
- 추후 모바일이 backend 연결을 시작할 때 `summary`, `allocation`, `release` 계약만 소비하면 된다
- Android mock/demo의 `VaultData`, `VaultCalculator`는 현재 로컬 시뮬레이션 상태이므로 backend 연결 전까지 유지 가능

## Docs
- `docs/execplans/active/2026-03-20-vault-backend-simulation.md`
- 필요 시 `docs/DonDone_P0_API_Contract_v0.md` 또는 후속 계약 메모

## Shared
- `ApiResponse<T>` envelope 유지
- JWT 기반 인증 흐름 유지
- P0 필수 안내 문구: `테스트넷 데모 기준 예상값`, `실제 수익 보장 아님`

# Contract Changes
- 신규 API 추가:
  - `GET /api/vault/summary`
  - `POST /api/vault/allocations`
  - `POST /api/vault/releases`
- 요청 DTO
  - allocation: `amount`
  - release: `amount`, `target`
- 응답 DTO
  - summary: `storedAmount`, `availableToStoreAmount`, `availableToTransferAmount`, `interestPreview`, `disclaimer`
  - allocation/release: 시뮬레이션 결과 스냅샷 + 식별자 + 시각
- DB 계약
  - `vault_positions` 또는 동등 aggregate table
  - `vault_yield_logs`
  - allocation/release 이력용 table 여부는 구현 중 결정하되 외부 API가 tx tracking을 요구하지 않는 한 최소화
- 참고 브랜치와의 차이
  - 외부 API는 `deposits/withdrawals` 대신 `allocations/releases`
  - tx hash / worker / receipt는 외부 계약에서 제외

# Security Notes
- `SecurityConfig`의 permitAll 목록 변경 없이 `vault` endpoint는 인증 필요로 유지한다.
- `@AuthenticationPrincipal AuthenticatedUser` 기반으로 자기 데이터만 조회/변경한다.
- private key 복호화나 signed transaction 저장은 이번 범위에서 필요하지 않다.
- remittance wallet balance를 읽더라도 response/log에 민감정보는 추가 노출하지 않는다.
- `release` target은 화면 해석용 enum으로만 사용하고 임의 문자열 허용 금지.

# Maintainability Notes
- PRD / 계약 문서 기준의 `시뮬레이션 Vault`와 참고 브랜치의 `실제 tx 기반 Vault`를 같은 외부 계약으로 섞지 않는다.
- remittance는 실제 testnet transfer, vault는 P0 시뮬레이션이라는 경계를 코드에서 분명히 유지한다.
- controller는 얇게 유지하고, 잔액 해석 / 예상 이자 계산 / allocation-release 상태 변경은 service/domain이 소유한다.
- 참고 브랜치의 on-chain adapter, worker, tx state 모델은 구조 참고만 하고 이번 범위에 무리하게 이식하지 않는다.
- mobile이 나중에 연결하기 쉽도록 response shape는 P0 계약 문서와 최대한 일치시킨다.

# Implementation Steps
1. PRD / P0 계약 기준으로 Vault 외부 계약을 확정하고 참고 브랜치와 차이를 명시한다.
2. 현재 remittance wallet balance, 송금 가능 금액, demo 시뮬레이션 요구사항을 반영한 Vault aggregate 설계를 정한다.
3. `vault` feature 패키지(`api/service/repo/model`)를 추가하고 summary/allocation/release DTO를 정의한다.
4. Vault 엔티티와 repository를 추가하고, 사용자별 보관 상태와 예상 이자 누적 계산을 구현한다.
5. `WalletService` 또는 remittance adapter에서 Vault 계산에 필요한 balance snapshot 접근을 재사용한다.
6. Vault service에서 allocation/release validation, 잔액 계산, 안내 문구, id 생성 로직을 구현한다.
7. `VaultController`를 추가하고 OpenAPI 설명 / Bean Validation / 인증 주입을 연결한다.
8. Vault config / error code / app property 등록을 추가한다.
9. integration test와 필요한 unit test를 추가해 summary, allocation, release, auth, validation, replay/중복 동작을 검증한다.
10. 구현 결과와 계약 차이가 생기면 문서 메모를 후속 반영한다.

# Test Plan
- `cd apps/dondone-backend && ./gradlew test --tests '*Vault*'`
- `cd apps/dondone-backend && ./gradlew test --tests '*Remittance*'`
- 최소 회귀 검증
  - wallet 없는 상태에서 vault 접근 시 `404` 또는 정책상 정의된 오류 반환
  - allocation amount 검증 실패
  - available balance 초과 allocation 차단
  - release amount 검증 실패 / stored balance 초과 차단
  - summary의 disclaimer / 예상 이자 / 사용 가능 금액 계산
  - JWT 없는 접근 `401`
  - 다른 사용자 데이터 은닉
- Docker/Testcontainers 가능 시 integration test 우선

# Review Focus
- 구현이 PRD/P0 계약의 `시뮬레이션 Vault` 범위를 벗어나지 않았는지
- remittance wallet balance와 vault stored amount 해석이 중복 계산 또는 음수 상태를 만들지 않는지
- validation / auth / 자기 데이터 접근 제한이 정확한지
- 참고 브랜치의 on-chain 개념이 외부 API 계약에 새어 나오지 않았는지
- mobile이 연결할 response shape가 계약 문서와 크게 어긋나지 않는지

# Worktree Split Decision
- Single lane

Vault는 shared auth, remittance wallet balance, 새 DTO, 새 entity, 테스트가 함께 움직인다. 외부 계약과 내부 시뮬레이션 규칙이 동시에 정리되어야 하므로 병렬 레인 분리는 merge risk가 높다.

# Commit Plan
1. 실행 계획 문서 추가
2. vault backend feature + config + contracts 구현
3. tests and docs follow-up

# Open Questions
- 내부 이력 테이블을 `vault_allocations/releases`로 나눌지, 참고 브랜치처럼 단일 transaction 테이블로 둘지
- mobile 연결용으로 allocation/release 응답에 어느 정도 상세 snapshot을 넣을지
- 참고 브랜치의 APY 기본값(`500 bps`)을 그대로 사용할지 별도 값으로 둘지

# Assumptions
- 사용자 요구의 `vault 기능`은 PRD P0 기준 `보관/이자 시뮬레이션`을 의미한다.
- 외부 API는 현재 저장소의 P0 계약 문서(`allocations/releases`)를 우선하고, 참고 브랜치의 `deposits/withdrawals`는 내부 참고 자료로만 사용한다.
- 이번 단계는 backend 중심 구현이며 mobile UI 코드는 즉시 변경하지 않는다.
- 실제 on-chain deposit/withdraw, tx polling, `StableVault.sol` 연동은 후속 단계로 미룬다.
