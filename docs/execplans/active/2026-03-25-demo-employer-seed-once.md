# Source Inputs
- User clarification on 2026-03-25 that `demo` employer data should seed only once on an empty schema and remain across restart/redeploy.
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevUserInitializer.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializerIntegrationTest.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/employer/service/EmployerWorkerRegistrationCodeService.java`
- `apps/dondone-web/src/app/AppShell.tsx`

# Goal
- Change `demo` employer bootstrap to a non-destructive one-time seed model so existing employer workspace data survives backend restart and redeploy unless the schema/data is explicitly recreated.

# In Scope
- `DevEmployerInitializer` startup behavior for existing demo employer profiles
- Regression coverage for rerunning the `demo` employer bootstrap
- Keep current demo employer/company/workplace seed values when bootstrapping an empty database

# Out of Scope
- Changes to worker registration code business rules
- Changes to `DevUserInitializer` or `DevAdvancePolicyInitializer`
- DTO/API/schema contract changes
- Data migration for already-reset demo environments outside normal startup behavior

# Affected Modules
## Backend
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- `apps/dondone-backend/src/test/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializerIntegrationTest.java`

## Mobile
- None

## Docs
- `docs/execplans/active/2026-03-25-demo-employer-seed-once.md`

## Shared
- None

# Contract Changes
- None

# Security Notes
- No auth/authz rule changes.
- No token or exposed endpoint changes.

# Maintainability Notes
- Keep ownership of demo bootstrap behavior inside `DevEmployerInitializer`; avoid spreading skip/reset rules into unrelated services.
- Preserve a clear rule: empty DB seeds once, existing demo employer profile means no destructive reset.
- Update the regression test to reflect the intended startup contract so future bootstrap fixes do not reintroduce destructive reruns.

# Implementation Steps
1. Remove the destructive rerun path in `DevEmployerInitializer` that deletes the existing employer scope before reseeding.
2. Change startup flow to create the demo employer account only when absent and skip workspace reseeding when an `EmployerProfile` already exists for that account.
3. Keep the empty-database seed path unchanged so a fresh schema still gets the expected demo employer workspace.
4. Replace the existing rerun-reset integration assertion with a persistence-oriented regression test that proves rerunning the initializer preserves the same employer scope.

# Test Plan
- Update `DevEmployerInitializerIntegrationTest` to assert rerunning the initializer:
  - does not throw
  - keeps the same `EmployerProfile` / company / workplace scope
  - does not duplicate seeded records
- Run the narrowest relevant backend test command for the bootstrap change.

# Review Focus
- Confirm rerun behavior is now non-destructive for existing demo employer data.
- Confirm first boot on an empty schema still seeds the required employer workspace.
- Confirm no duplicate employer/company/workplace rows are created on repeated startup.

# Worktree Split Decision
- Single lane

This change touches one bootstrap owner and one regression test, with shared startup behavior and test expectations moving together. Splitting it would add merge risk without meaningful parallelism.

# Commit Plan
- `fix: preserve demo employer workspace on restart`

# Open Questions
- None at implementation start.

# Assumptions
- Presence of an `EmployerProfile` for the fixed demo employer account is the correct signal that the employer demo workspace has already been seeded.
- Preserving existing data is preferred over repairing partially deleted demo employer workspaces during restart.
