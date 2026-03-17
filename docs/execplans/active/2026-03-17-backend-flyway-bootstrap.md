# Source Inputs
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/build.gradle`
- `apps/dondone-backend/src/main/resources/db/drafts/baseline_v1_draft.sql`
- `apps/dondone-backend/AGENTS.md`
- `.agents/skills/db-migration-checklist/SKILL.md`

# Goal
- DonDone backend DB schema 기준을 Hibernate `ddl-auto: update`에서 Flyway baseline migration으로 전환한다.

# In Scope
- backend Flyway dependency and configuration
- baseline draft를 `db/migration`의 `V1__baseline.sql`로 승격
- `ddl-auto`를 migration-first 운영에 맞게 조정
- backend boot/test 기준 검증

# Out of Scope
- `work_proofs` shape 재설계
- raw `Long` 관계 전면 리팩토링
- 운영용 break-glass, 승인 매트릭스, 대규모 운영 정책
- 모바일 변경

# Affected Modules
## Backend
- `apps/dondone-backend/build.gradle`
- `apps/dondone-backend/src/main/resources/application.yml`
- `apps/dondone-backend/src/main/resources/db/migration/`

## Mobile
- 없음

## Docs
- `docs/execplans/active/2026-03-17-backend-flyway-bootstrap.md`

## Shared
- 없음

# Contract Changes
- DB schema source of truth를 migration-first로 전환
- backend startup 시 Flyway migration 실행 후 JPA validate 사용

# Security Notes
- auth/authz contract 변경 없음
- secret handling 변경 없음

# Maintainability Notes
- baseline migration은 현재 코드 기준을 우선 반영하고, 이후 구조 정리는 추가 migration으로 분리한다.
- migration과 entity/config 변경은 같은 change set에서 유지한다.

# Implementation Steps
1. backend build/config에서 Flyway 도입에 필요한 최소 설정 반영
2. baseline draft를 `V1__baseline.sql`로 승격
3. `ddl-auto`를 `validate`로 전환
4. boot/test 검증 후 잔여 mismatch 정리

# Test Plan
- `./gradlew test`
- 가능하면 backend boot 시 migration startup 확인

# Review Focus
- baseline SQL과 현재 entity mismatch 여부
- startup 시 Flyway -> JPA validate 순서의 안정성
- 테스트 환경에서 migration 적용 호환성

# Worktree Split Decision
- Single lane
- shared schema, backend config, migration baseline이 동시에 움직이므로 병렬 분리 이득보다 merge risk가 크다.

# Commit Plan
- `chore: bootstrap flyway baseline for backend schema`

# Open Questions
- baseline SQL을 startup 검증까지 한 번에 확정할지, draft 보정 후 후속 migration으로 나눌지

# Assumptions
- 로컬/개발 DB는 baseline 기준으로 재생성 가능하다.
- 현재 baseline draft가 DonDone backend의 우선 기준 구조다.
