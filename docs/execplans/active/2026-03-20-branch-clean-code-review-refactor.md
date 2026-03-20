# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/review-checklist/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 브랜치 범위:
  - `git log --oneline --decorate -8`
  - `git diff --stat origin/develop..HEAD`
  - `git diff --name-only origin/develop..HEAD`
- 최근 변경 산출물:
  - remittance/jobs schema-first refactor commits
  - remittance observability / latency 분석 commits
  - mobile home wallet balance refresh commit

# Goal
`origin/develop..HEAD` 범위의 최근 커밋을 clean-code/maintainability 관점에서 리뷰하고, 실제 유지보수 리스크가 확인된 지점만 최소 범위로 리팩토링한다.

# In Scope
- backend remittance/jobs/service 구조의 책임 분리 및 중복 완화
- remittance observability 변경 중 구조적으로 과한 결합 또는 noisy path 정리
- mobile `DemoSessionViewModel`의 상태 갱신 흐름에서 ownership/duplication/테스트 가독성 이슈 정리
- branch 내 execplan/review/doc consistency 점검 후 필요한 최소 문서 정리

# Out of Scope
- 새 기능 추가
- API 계약 변경
- DB schema 재설계 2차 작업
- observability stack 자체 재구성
- broad formatting / naming-only cleanup

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
- 필요 시 `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/**`

## Mobile
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/app/session/DemoSessionViewModel.kt`
- `apps/dondone-mobile/android/app/src/test/java/com/dondone/mobile/app/session/DemoSessionViewModelTest.kt`

## Docs
- `docs/execplans/active/2026-03-20-branch-clean-code-review-refactor.md`
- 필요 시 이번 브랜치에서 추가된 remittance 관련 execplan/review/docs 일부

## Shared
- remittance 상태/계약을 참조하는 demo session state

# Contract Changes
- 원칙적으로 없음
- 리뷰 결과가 DTO/API drift를 지적하더라도, 이번 작업은 계약 수정 없이 내부 구조 정리를 우선한다

# Security Notes
- clean-code refactor 중에도 auth/authz 규칙은 바꾸지 않는다
- observability 정리 시 민감정보 로그 노출을 늘리지 않는다
- mobile session state refactor는 토큰/세션 처리 의미를 바꾸지 않는다

# Maintainability Notes
- 관측용 계측 코드가 도메인 흐름을 과도하게 삼키지 않도록 경계를 확인한다
- service 메서드가 로깅, 상태전이, 조회조립, 정책판단을 동시에 떠안고 있으면 작은 보조 구조로 분리한다
- mobile balance refresh는 UI state ownership을 명확히 하되 backend contract와 결합을 늘리지 않는다
- 문서 정리는 현재 코드 기준선과 branch scope 설명의 불일치를 없애는 수준으로 제한한다

# Implementation Steps
1. `origin/develop..HEAD` 범위의 최근 커밋과 변경 파일을 정리한다.
2. backend 중심 clean-code 리뷰를 서브 에이전트로 수행한다.
3. mobile/docs 중심 clean-code 리뷰를 서브 에이전트로 수행한다.
4. findings를 severity와 리팩토링 비용 기준으로 추린다.
5. 계약 변경 없이 해결 가능한 구조 이슈부터 최소 범위로 수정한다.
6. 관련 테스트를 보강하거나 기존 테스트를 갱신한다.
7. backend/mobile 검증을 실행하고 남은 리스크를 정리한다.

# Test Plan
- backend: `cd apps/dondone-backend && ./gradlew test`
- 필요 시 remittance 집중 검증:
  - `./gradlew test --tests '*remittance*'`
- mobile:
  - `cd apps/dondone-mobile/android && ./gradlew test`
- 테스트 불가 시 blocker와 미검증 범위를 명시

# Review Focus
- service 책임이 과도하게 섞여 있는지
- observability/perf 계측이 hot path 가독성과 변경 안전성을 해치지 않는지
- mobile session refresh 로직에 중복 상태 갱신이나 ownership 혼선이 없는지
- branch 문서와 실제 구현/커밋 범위 설명이 어긋나지 않는지

# Worktree Split Decision
- Single lane

최근 커밋이 backend remittance, observability, mobile session, docs를 함께 건드리고 있고 공통 상태/계약 설명이 아직 고정되지 않았다. 리뷰는 병렬로 하되 실제 리팩토링 반영은 한 레인에서 모아서 진행하는 편이 안전하다.

# Commit Plan
1. clean-code refactor 코드 변경
2. 테스트 보강
3. 필요한 문서 정리

# Open Questions
- observability/perf 계측은 현재 브랜치 범위에서 구조 정리만 할지, 일부는 후속 branch로 미룰지
- mobile session refresh가 remittance API 후속 계약을 어느 수준까지 추적해야 하는지

# Assumptions
- 현재 브랜치 기준 최근 커밋 전부를 조사하되, 실제 리팩토링은 리뷰에서 가치가 확인된 지점만 수행한다
- clean-code refactor는 동작 보존이 우선이다
- 기존 미커밋 변경이 있다면 이번 작업과 무관한 파일은 건드리지 않는다
