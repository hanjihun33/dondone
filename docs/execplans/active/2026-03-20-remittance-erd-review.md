# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
- 현재 송금 백엔드 구현:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/UserWallet.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Recipient.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/model/Transfer.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/UserWalletRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/RecipientRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/repo/TransferRepository.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/WalletService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RecipientService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/TransferService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/service/RemittancePolicyService.java`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/model/Job.java`
  - `apps/dondone-backend/src/main/resources/application.yml`
- 기존 설계 메모:
  - `docs/reviews/active/2026-03-18-remittance-async-design-note.md`
  - `docs/execplans/active/2026-03-18-remittance-backend-sepolia.md`
- 테스트 참고:
  - `apps/dondone-backend/src/test/java/com/workproofpay/backend/remittance/RemittanceIntegrationTest.java`

# Goal
현재 remittance JPA 엔티티 기준의 실질 ERD를 텍스트로 정리하고, 권장 ERD와 리팩토링 고려사항을 같은 문서에 남겨 이후 schema refactor, migration, entity 정리의 기준점으로 사용한다.

# In Scope
- 현재 `user_wallets`, `recipients`, `transfers`, `jobs`, `users`의 관계와 제약 상태 문서화
- 현재 JPA 중심 설계의 구조적 약점 정리
- 권장 remittance ERD 제안
- JPA 관점의 N+1 및 조회 패턴 고려사항 정리
- ERD 리팩토링 과정에서 필요한 마이그레이션, 무결성, 보안, 테스트 체크리스트 정리

# Out of Scope
- 실제 엔티티 코드 변경
- Flyway/Liquibase 도입
- 실제 DB migration SQL 작성
- API 계약 변경
- 모바일/웹 화면 변경

# Affected Modules
## Backend
- 영향 없음
- 참고 대상만 정리:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`

## Mobile
- 없음

## Docs
- 신규: `docs/execplans/active/2026-03-20-remittance-erd-review.md`
- 신규: `docs/infra/REMITTANCE_ERD_REFACTOR_NOTE.md`

## Shared
- 없음

# Contract Changes
- 없음
- 이번 작업은 설계 문서 산출물만 추가한다.

# Security Notes
- 문서에서 private key 저장 위치와 민감정보 보호 요구사항을 유지해야 한다.
- 권장 ERD는 송금 이력 보존과 사용자 소유권 검증 기준을 함께 설명해야 한다.
- `jobs`와 같이 FK를 강하게 걸기 어려운 테이블은 보상 제약과 운영 검증 포인트를 명시해야 한다.

# Maintainability Notes
- `recipient`를 주소록 aggregate로 유지하고 `transfer`를 불변 이력 aggregate로 분리하는 기준을 문서에서 명확히 해야 한다.
- 현재 구조의 "스칼라 FK + 서비스 수동 join" 패턴을 무조건 연관관계 객체 그래프로 바꾸지 말고, 조회 패턴별 read model 전략을 함께 적어야 한다.
- N+1 방지와 무결성 확보는 별개이므로, 권장 ERD에서 FK/스냅샷/인덱스와 조회 전략을 분리해 서술해야 한다.

# Implementation Steps
1. 현재 remittance 엔티티, repository, service에서 사실상 사용 중인 관계와 제약을 수집한다.
2. 현재 ERD를 "논리 관계"와 "실제 DB 제약 부재"를 구분해서 텍스트로 정리한다.
3. 권장 ERD를 user ownership, recipient contact, transfer snapshot, wallet ownership 기준으로 재구성한다.
4. JPA/N+1 고려사항과 조회 전략을 권장 ERD 아래에 정리한다.
5. migration, backfill, data cleanup, enum, delete policy, 테스트 관점의 리팩토링 체크리스트를 정리한다.
6. 문서 톤과 파일 위치를 저장소 규칙에 맞게 정리한다.

# Test Plan
- 애플리케이션 테스트 변경 없음
- 수동 검증:
  - 문서에 현재 테이블/관계/약점이 실제 코드와 맞는지 확인
  - 권장 ERD가 현재 remittance API 흐름과 충돌하지 않는지 확인
  - N+1, snapshot, FK, unique, index, migration 관점이 모두 포함되었는지 확인

# Review Focus
- 현재 ERD가 실제 코드보다 과도하게 추정되어 있지 않은지
- 권장 ERD가 "현재 주소록"과 "과거 송금 이력"을 분리해 설명하는지
- JPA N+1 회피 전략이 과도한 양방향 연관관계 도입으로 흐르지 않는지
- migration 단계 고려사항이 빠지지 않았는지

# Worktree Split Decision
- Single lane

이번 작업은 코드 변경이 아니라, 현재 구현과 권장 구조를 한 문서에서 일관되게 설명하는 설계 정리 작업이다. 엔티티, 서비스, 조회 패턴, 리팩토링 순서를 함께 다뤄야 해서 분리 이점이 낮다.

# Commit Plan
1. remittance ERD 검토 실행 계획 문서 추가
2. 현재/권장 ERD 및 리팩토링 고려사항 문서 추가

# Open Questions
- `user_wallets`를 MVP 동안 `user_id` PK로 유지할지, `wallet_id` surrogate key로 미리 전환할지
- generic `jobs` 테이블을 계속 유지할지, remittance 전용 job subtype 또는 `reference_kind` 기준을 확장할지

# Assumptions
- 이번 턴에서는 문서만 추가하고 코드/스키마는 변경하지 않는다.
- MVP 기준으로 한 사용자는 주 송신 지갑 1개를 갖는 모델을 우선 가정한다.
- 권장 ERD는 향후 migration-first 전환을 위한 기준안이며 즉시 모든 필드를 강제 변경하지는 않는다.
