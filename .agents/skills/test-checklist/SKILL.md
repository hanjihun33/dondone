---
name: test-checklist
description: Use when adding or updating tests for a DonDone change. Determine the minimum necessary unit, integration, validation, auth, contract, and regression checks, then run the narrowest useful verification commands and report blockers and untested areas clearly.
---

# Test Checklist

## Goal

Translate a scoped feature change into concrete verification work.

## Review Areas

- request validation
- business rules
- auth/authz behavior
- DTO and API contract consistency
- regression risk on adjacent flows
- loading/empty/error/success states for UI changes
- disclaimer or evidence-first messaging that must remain intact
- non-functional impact when relevant: logging, backward compatibility, performance-sensitive paths

## Backend Expectations

- Prefer the smallest targeted test change first.
- Add auth and validation coverage when behavior changed there.
- Use unit tests for isolated business logic, integration tests for cross-layer behavior, and contract-focused checks when DTO or API shapes change.
- Confirm validation error behavior and auth failure behavior when those paths changed.
- Run `./gradlew test` when Java is available and the scope justifies it.

## Frontend/Mobile Expectations

- Prefer project-defined build or test commands.
- Verify state transitions for loading, empty, error, and success paths when the UI flow changed.
- Verify API failure behavior and required disclaimer text when those user paths changed.
- If automated coverage is missing, state the manual critical path checks explicitly.

## Output

Return the result in Korean:

- `Added/Updated Tests`
- `Test Levels Chosen`
- `Commands Run`
- `Blocked Commands`
- `Manual Checks`
- `Coverage Gaps`
- `Residual Risk`
