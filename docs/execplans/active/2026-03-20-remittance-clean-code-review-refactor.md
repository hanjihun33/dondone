# Source Inputs
- 저장소 규칙: `AGENTS.md`
- 워크플로우 / 스킬 기준:
  - `.agents/skills/execplan-writer/SKILL.md`
  - `.agents/skills/review-checklist/SKILL.md`
  - `.agents/skills/implement-checklist/SKILL.md`
- 브랜치 변경 범위:
  - `git diff origin/develop..HEAD`
  - `git log --oneline origin/develop..HEAD`
- 주요 대상:
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
  - `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
  - `apps/dondone-backend/src/main/resources/application.yml`
  - `deploy/observability/**`
  - `deploy/sql/2026-03-20-remittance-jobs-baseline.sql`
  - `docker-compose.dev.yml`

# Goal
`origin/develop..HEAD` 범위의 remittance/jobs/observability 변경을 clean code/maintainability 관점으로 재검토하고, 실제로 change safety를 해치는 구조 문제만 최소 범위로 리팩토링한다.

# In Scope
- remittance/jobs 서비스와 worker의 책임 분리 검토
- observability 계측이 도메인 코드에 끼친 결합도와 복잡도 점검
- schema-first remittance 리팩토링이 코드/SQL/테스트에 남긴 유지보수 리스크 정리
- 리뷰 결과에 따라 필요한 최소 리팩토링 구현
- 관련 테스트 갱신 및 재실행

# Out of Scope
- 모바일 화면/상태 구조 리팩토링
- remittance 외 다른 도메인 구조 개선
- 대규모 아키텍처 재설계
- Flyway 도입, 브랜치 정리, git history 수정

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/remittance/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/jobs/**`
- `apps/dondone-backend/src/main/resources/application.yml`

## Mobile
- 직접 구현 없음
- 필요 시 backend contract 영향만 검토

## Docs
- `docs/execplans/active/2026-03-20-remittance-clean-code-review-refactor.md`
- 필요 시 `docs/reviews/active/` 또는 기존 remittance 설계 문서 후속 정리

## Shared
- `deploy/observability/**`
- `deploy/sql/2026-03-20-remittance-jobs-baseline.sql`
- `docker-compose.dev.yml`

# Contract Changes
- 기본 목표는 외부 API/DTO 계약 유지
- 불가피한 경우 내부 계층 구조와 observability wiring만 정리
- DB baseline SQL은 엔티티 의도와 어긋나는 지점만 보정

# Security Notes
- remittance auth/authz, JWT 공개 경로, 민감정보 노출 규칙은 변경하지 않는다
- observability 추가가 민감정보나 토큰 데이터를 로그/metric에 노출하지 않는지 확인한다
- admin remittance 조회가 도메인 범위를 벗어나지 않도록 review 대상에 포함한다

# Maintainability Notes
- remittance 서비스에 계측 코드가 직접 섞여 있으면 책임이 흐려지므로, 구조적 결합이 높은 지점을 우선 정리한다
- worker와 service는 도메인 로직, 관측 로직, 운영 복구 로직이 한 메서드에 뒤섞이지 않도록 제한한다
- clean code 명목의 광범위한 정리보다, change safety를 실제로 떨어뜨리는 hotspot만 다룬다
- schema-first refactor와 observability 변경이 같은 파일에서 얽힌 경우, 후속 변경이 안전하도록 경계를 더 분명히 한다

# Implementation Steps
1. `origin/develop..HEAD` 범위의 backend remittance/jobs/observability 변경을 파일 단위로 정리한다.
2. 서브 에이전트 리뷰를 backend 구조와 cross-cutting observability 축으로 분리해 수행한다.
3. findings를 severity 기준으로 합치고, 실제 리팩토링 대상만 좁힌다.
4. 최소 범위 코드 수정으로 책임 분리, naming, coupling 문제를 정리한다.
5. 관련 테스트를 보강하거나 갱신한다.
6. `./gradlew test --tests '*remittance*'` 와 필요 시 `./gradlew test`로 회귀를 확인한다.

# Test Plan
- `cd apps/dondone-backend && ./gradlew test --tests '*remittance*'`
- 필요 시 `./gradlew test --tests '*JobServiceTest' '*RemittanceJobWorkerTest' '*RemittanceOpsServiceTest'`
- 구조 변경 폭이 크면 `./gradlew test`

# Review Focus
- 계측 코드가 도메인 흐름을 읽기 어렵게 만들었는지
- worker/service 메서드가 책임을 과하게 떠안고 있는지
- schema/SQL/entity/test가 서로 다른 truth source를 만들고 있지 않은지
- 운영용 job/metric/summary 경로가 숨은 coupling을 만들고 있지 않은지

# Worktree Split Decision
- Single lane

최근 remittance/jobs/schema/observability 변경이 같은 서비스와 worker 파일에 겹쳐 있어 병렬 구현은 충돌 위험이 높다. 리뷰는 서브 에이전트로 분리하되, 실제 수정은 한 레인에서 순차 처리한다.

# Commit Plan
1. clean code review/refactor execplan 추가
2. remittance/jobs clean code 리팩토링
3. 관련 테스트 및 문서 후속 정리

# Open Questions
- observability 계측은 서비스 내부 직접 호출을 유지할지, 별도 helper/aspect/event 경계로 뺄지
- recent commits 중 mobile/home 잔액 갱신 변경은 이번 리팩토링에서 read-only review만 할지

# Assumptions
- 사용자 의도는 branch history 기반 clean-code review 후 필요한 refactor까지 진행하는 것이다
- remittance/jobs/backend observability가 이번 턴의 주 수정 대상이다
- 외부 API 계약은 가능하면 유지하고, 내부 구조 개선을 우선한다
