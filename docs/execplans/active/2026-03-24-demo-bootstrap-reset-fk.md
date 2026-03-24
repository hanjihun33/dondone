# Source Inputs
- User-provided backend startup log on 2026-03-24
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevEmployerInitializer.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkProof.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/model/WorkContract.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/workproof/repo/WorkProofRepository.java`

# Goal
- Make `demo` bootstrap reruns idempotent when prior demo workplace data still references the active `work_contracts` row.
- Prevent backend startup failure caused by deleting a contract before deleting all workplace-scoped `work_proofs`.

# In Scope
- `DevEmployerInitializer` reset ordering and delete scope
- `WorkProofRepository` support for workplace-scoped cleanup
- Narrow regression test for rerunning the `demo` bootstrap

# Out of Scope
- API/DTO changes
- Auth or security rule changes
- Schema migration changes
- Preserving prior demo workplace records across restarts

# Assumptions
- `demo` bootstrap owns the seeded company/workplace and is allowed to fully reset workplace-scoped records on rerun.
- Non-seeded users may remain, but their `work_proofs` under the seeded workplace should be deleted during the reset because the workplace and contract are recreated.

# Contract Changes
- None

# Security Notes
- None

# Test Plan
- Add an H2-backed Spring Boot regression test that:
  - boots with `test` + `demo`
  - creates an extra non-seeded `work_proof` referencing the seeded contract
  - reruns `DevEmployerInitializer`
  - verifies the rerun succeeds and reseeds the workplace

# Worktree Split Decision
- Single lane
