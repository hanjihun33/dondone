# DonDone Agent Guide

## Goal
Implement DonDone (`S14P21C202`) as a demo-ready MVP based on `docs/DonDone_PRD_v1.5.md`, with delivery quality suitable for team collaboration and review.

## Repository Scope
- `apps/dondone-backend/`: Spring Boot backend (feature-first skeleton + auth baseline)
- `apps/dondone-mobile/`: mobile mockup and Android app workspace
- `docs/`: PRD and project documentation
- `S14P11C205/`: reference repository (read-only, for patterns only)

## Requirement Clarification (MANDATORY)
Before implementing changes, remove ambiguity first. Confirm:
- expected behavior (input/output/error cases),
- exact in-scope modules/files/endpoints,
- contract changes (DTO, DB schema, API response),
- security impact (authn/authz, token handling, exposed paths),
- non-functional impact (performance, logging, backward compatibility).

If requirements remain ambiguous, state assumptions explicitly before coding.

## Product Constraints (PRD v1.5)
- Public product name: **DonDone**
- MVP focus: `P0` first (WorkProof -> Wage Shield -> Docs/Claim -> Testnet Remittance)
- Policy: testnet/demo scope only; do not implement real-money settlement behavior
- Wage result positioning: anomaly detection + evidence-first, not legal/financial final judgment

## Backend Technical Baseline
- Java 17, Spring Boot 3.2.x
- Spring Security + JWT (stateless)
- Spring Data JPA + Querydsl
- PostgreSQL
- OpenAPI/Swagger

### Backend Implementation Rules
- Preserve feature-first structure: `api/service/repo/model/adapter`.
- Validate all request DTOs with Bean Validation.
- Keep security rules explicit in `SecurityConfig` and JWT filter exclusion lists.
- Separate domain logic from controller logic.
- Hide external dependencies (chain/PDF/storage/AI) behind interfaces.
- Never hardcode secrets; use env or config placeholders.

## Frontend Baseline Rules
- Keep states explicit: loading / empty / error / success.
- Maintain strict API contract consistency with backend DTOs.
- Avoid ad-hoc global state; prefer structured store/state patterns.
- Preserve PRD disclaimers and evidence-first UX messaging.

## Git Safety (MANDATORY)
- Do not run destructive git commands without explicit user request.
- Forbidden examples:
  - `git reset --hard`
  - `git checkout -- <path>`
  - `git restore <path>`
  - `git clean -fd`, `git clean -fdx`
- Prefer minimal `apply_patch` changes.

## Testing & Verification
- Backend changes:
  - run `./gradlew test` when Java is available,
  - add/update tests for auth/validation/business rules.
- Frontend changes:
  - run `npm run build` (or project-defined checks),
  - verify responsive behavior and critical flows manually if tests are absent.
- If execution environment blocks tests (e.g., missing Java), report it explicitly.

## Local Run Quick Commands
- Backend:
  - `cd apps/dondone-backend`
  - `./gradlew bootRun`
- Mobile mockup:
  - `cd apps/dondone-mobile/mockup`
  - `python -m http.server 4173`

## Delivery Checklist
1. Implementation matches PRD scope (`P0` prioritized).
2. Security and validation paths reviewed.
3. Build/test (or blocker) reported clearly.
4. Docs updated for changed contracts/flows.
5. No secrets or generated artifacts committed.

## Codex Workflow
- Before coding, produce or update an execution plan under `docs/execplans/` for non-trivial work.
- For tasks that change workflow tooling or process guidance under `.codex/`, `.agents/`, or `docs/CODEX_WORKFLOW.md`, read `docs/CODEX_WORKFLOW.md` first.
- Prefer repository skills for repeatable steps:
  - `db-migration-checklist`
  - `prd-breakdown`
  - `execplan-writer`
  - `implement-checklist`
  - `test-checklist`
  - `review-checklist`
  - `commit-grouping`
- Keep agent roles thin and use skills as the detailed playbooks.
- Use multi-agent exploration/review before or after implementation, not as an excuse to skip a scoped plan.
- For non-trivial feature work, explicitly use `explorer` sub-agents to separate backend, mobile, and cross-cutting contract impact before implementation starts.
- Use a separate `tester` sub-agent when the change affects DTO/API contracts, auth behavior, validation logic, or significant UI state transitions.
- For non-trivial reviews, explicitly use sub-agents instead of doing all review work in the main thread.
- Default review split:
  - `reviewer` for correctness, regressions, contracts, and maintainability
  - `security_reviewer` when auth, token, exposed-path, or sensitive-data impact exists
- Use `docs_writer` when execution plans or review notes need durable cleanup or when the task materially changes documented contracts or workflow guidance.
- Parallel implementation is allowed only after the execution plan explicitly marks the task as worktree-safe.
- Do not split work across parallel lanes when shared DTOs, auth/security rules, shared entities, or common response contracts are still moving.
- Record implementation plans in `docs/execplans/` and review findings or follow-up notes in `docs/reviews/` when the task is large enough to need them.
- Keep plan and review documents organized by lifecycle:
  - active work in `docs/execplans/active/` and `docs/reviews/active/`
  - finished or stale artifacts moved to `archive/`
- Use date-prefixed filenames such as `2026-03-09-workproof-backend.md` when creating new plan or review documents.

## Reference Policy (`S14P11C205`)
- Reuse only proven patterns (workflow, conventions, structure).
- Adapt naming and behavior to DonDone PRD.
- Do not copy legacy domain logic that is out of current MVP scope.
