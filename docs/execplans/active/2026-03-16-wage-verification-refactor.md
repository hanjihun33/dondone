# Source Inputs
- `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`
- `docs/execplans/active/2026-03-16-wage-verification-contract.md`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/wage/WageDemoIntegrationTest.java`

# Goal
`feature/wage-verification-contract` 브랜치에서 이미 추가한 verification 기능의 API/DB 계약은 유지한 채, `WageService`와 `WageSummaryCalculator`의 책임을 더 읽기 쉽게 정리한다.

# In Scope
- `WageService`의 중복 summary 조립 제거
- verification draft 조립을 별도 helper/factory 성격 메서드로 정리
- `WageSummaryCalculator`의 estimate/verify 공통 계산 추출
- comment-only가 아닌 구조 변경 후 기존 backend 테스트 재검증

# Out of Scope
- endpoint 추가/삭제
- request/response DTO 필드 변경
- entity schema 변경
- security rule 변경
- WorkProof contract 변경
- Documents/Claim downstream 구현

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/service/WageSummaryCalculator.java`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-16-wage-verification-refactor.md`

## Shared
- 없음

# Contract Changes
- 없음
- `POST /api/wage/verifications`
- `GET /api/wage/verifications/{verificationId}`
- 기존 `summary`, `estimate`, `deposit` 응답 shape 유지

# Security Notes
- 보안 규칙 변경 없음
- 소유권 확인, JWT 보호, 404 masking 동작은 기존 구현 그대로 유지

# Maintainability Notes
- `WageService`는 orchestration에 집중하고, DTO/aggregate 조립 세부는 helper 메서드로 묶는다.
- `WageSummaryCalculator`는 숫자 계산 규칙을 한 곳에서 재사용하게 해 lane 1 규칙 수정 시 누락 위험을 줄인다.
- broad cleanup은 하지 않고, 현재 verification 변경분에 직접 닿는 중복만 줄인다.

# Implementation Steps
1. `WageService`에서 `WageMonthlySummaryResponse` 조립을 helper로 추출한다.
2. `WageService`에서 verification draft 생성용 내부 helper를 만들어 `new WageVerificationDraft(...)`의 의미를 블록 단위로 드러낸다.
3. `WageSummaryCalculator`에서 base/overtime/night/total 계산을 공통 record/helper로 추출한다.
4. `summarize`, `estimate`, `verify`가 공통 계산 결과를 사용하도록 정리한다.
5. `test`와 `integrationTest`를 다시 실행해 계약 회귀가 없는지 확인한다.

# Test Plan
- `./gradlew.bat test integrationTest --console=plain`

# Review Focus
- public API contract가 바뀌지 않았는지
- verification snapshot 저장 필드 순서/의미가 유지되는지
- estimate와 verify의 계산 결과가 리팩토링 전과 동일한지
- helper 추출이 오히려 책임을 숨기지 않았는지

# Worktree Split Decision
- Single lane

이미 같은 브랜치에서 DTO, entity, integration test가 함께 움직이고 있어서 이번 리팩토링은 같은 작업 트리에서 연속적으로 정리하는 편이 안전하다.

# Commit Plan
- `refactor: simplify wage verification assembly and calculation flow`

# Open Questions
- verification draft 조립을 장기적으로 `WageVerificationDraftFactory` 클래스로 승격할지 여부
- summary status 문자열을 별도 enum으로 올릴지 여부

# Assumptions
- 현재 lane 1 계산 규칙은 그대로 유지한다.
- 이번 작업은 팀원이 읽기 쉬운 구조 정리가 목적이며, 동작 변화는 허용하지 않는다.
