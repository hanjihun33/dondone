## Source Inputs

- `docs/DonDone_PRD_v1.5.md` sections 7B, 7D, 9.3, 13.4
- Repository `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`
- Current backend exploration of `auth`, `workproof`, `wage`, `shared/security`, and test layout

## Goal

Implement the minimum backend slice required to support Kang Boseung's Week 1 UI with real authenticated API contracts instead of ping-only placeholders.

## In Scope

- Authenticated WorkProof create/list/detail APIs
- Monthly WorkProof summary API with total workdays, total minutes, overtime minutes, night minutes, edited record count, and reflected/pending counts
- Wage deposit input and monthly wage summary API with reference-only estimate and optional anomaly preview
- Demo state API that accepts `asOf` query input and returns a combined Week 1 state payload
- Bean Validation for all new request DTOs
- Targeted backend tests for auth, validation, and scoped business behavior

## Out of Scope

- WorkProof edit flow `W4`
- Proof Pack, Claim Kit, PDF generation, and document storage
- Real blockchain remittance flow, tx hash generation, and SafePay policy execution
- Advance eligibility policy engine
- Full Week 2 wage anomaly reason engine and evidence DTO expansion
- Demo seed/reset endpoints

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/wage/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/demo/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/security/**`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/**`

### Mobile

- No direct code changes planned
- New contracts should be usable by Week 1 home, monthly summary, deposit input, difference screen shell, and time-travel UI

### Docs

- This execution plan
- Optional short backend README or PR notes only if endpoint usage becomes unclear during implementation

### Shared

- `ApiResponse` and global exception behavior remain shared; no shape change planned

## Contract Changes

- Add `POST /api/workproof`
- Add `GET /api/workproof`
- Add `GET /api/workproof/{id}`
- Add `GET /api/workproof/monthly-summary?yearMonth=YYYY-MM`
- Add `POST /api/wage/deposits`
- Add `GET /api/wage/summary?yearMonth=YYYY-MM`
- Add `GET /api/demo/state?asOf=YYYY-MM-DD&yearMonth=YYYY-MM`
- All new endpoints require JWT auth
- `asOf` is implemented as an explicit query parameter for this slice; `X-Demo-AsOf` header support is deferred

## Security Notes

- Keep all new feature endpoints authenticated under existing JWT rules
- Enforce per-user ownership on WorkProof and deposit reads/writes
- Avoid exposing cross-user identifiers or aggregate data
- Keep explicit allowlist in `SecurityConfig` and JWT filter unchanged except if a new public endpoint becomes necessary, which is not planned

## Maintainability Notes

- Keep controller logic thin; place time calculations and summary logic in feature services
- Avoid mixing demo aggregation with controller glue; `demo` should compose feature services rather than duplicate logic
- Reuse shared response and exception patterns instead of introducing feature-specific wrappers

## Implementation Steps

1. Add the Week 1 backend exec plan and confirm single-lane scope.
2. Implement `workproof` model, repository, DTOs, service, and controller endpoints.
3. Implement monthly summary calculation using `WorkProof` records and `yearMonth` plus `asOf` filtering.
4. Implement `wage` deposit model, repository, DTOs, service, and monthly summary endpoint.
5. Implement `demo` state controller/service that composes WorkProof and Wage summaries using `asOf`.
6. Add focused tests for validation, auth requirements, per-user ownership, and summary behavior.
7. Run backend verification and capture any residual gaps tied to deferred Week 2 logic.

## Test Plan

- Add MVC or Spring Boot tests for:
  - unauthorized access on new endpoints
  - request validation failures
  - WorkProof create/list/detail happy path
  - per-user isolation on detail/summary access
  - monthly summary calculation for overtime/night buckets
  - wage deposit save and summary response
  - demo state filtering by `asOf`
- Run `./gradlew test --no-daemon`

## Review Focus

- DTO and API shapes align with PRD Week 1 scope and do not overreach into Week 2
- Summary math is explainable and consistent between direct feature endpoints and demo state composition
- Auth and ownership checks are enforced on every read/write path
- Demo state does not duplicate business rules or drift from feature services

## Worktree Split Decision

Single lane

Shared DTOs, auth boundaries, entities, and response contracts are all moving in this slice. Splitting now would increase merge risk without enough parallel value.

## Commit Plan

- Commit 1: exec plan + WorkProof domain and API
- Commit 2: wage + demo state domain/API and tests
- Commit 3: follow-up docs or contract clarifications only if needed

## Open Questions

- Whether mobile wants `asOf` only in query params or eventually also in `X-Demo-AsOf`
- Whether monthly summary should treat currently open shifts as zero minutes or as `asOf`-bounded partial minutes

## Assumptions

- Week 1 backend support should make the UI server-connectable, not fully production-complete
- Open shifts without `clockOutAt` are excluded from worked-minute summaries for now
- Overtime is any worked minute beyond 8 hours within a single local calendar day
- Night minutes are minutes between 22:00 and 06:00 local time
- Wage anomaly output remains evidence-first and reference-only; no legal or final payroll judgment wording is added
