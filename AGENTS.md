# DawnDone Agent Guide

## Goal
Implement DawnDone (`S14P21C202`) as a demo-ready MVP based on `docs/WorkProofPay_PRD_v1.4.md`, with delivery quality suitable for team collaboration and review.

## Repository Scope
- `dundun-backend/`: Spring Boot backend (feature-first skeleton + auth baseline)
- `dundun-frontend/`: frontend app
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

## Product Constraints (PRD v1.4)
- Public product name: **DawnDone**
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
  - `cd dundun-backend`
  - `./gradlew bootRun`
- Frontend:
  - `cd dundun-frontend`
  - `npm install`
  - `npm run dev`

## Delivery Checklist
1. Implementation matches PRD scope (`P0` prioritized).
2. Security and validation paths reviewed.
3. Build/test (or blocker) reported clearly.
4. Docs updated for changed contracts/flows.
5. No secrets or generated artifacts committed.

## Reference Policy (`S14P11C205`)
- Reuse only proven patterns (workflow, conventions, structure).
- Adapt naming and behavior to DawnDone PRD.
- Do not copy legacy domain logic that is out of current MVP scope.
