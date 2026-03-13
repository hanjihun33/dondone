## Source Inputs

- Current `auth` backend implementation in `apps/dondone-backend`
- Repository `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`
- Prior discussion about moving creation responsibility out of `AuthService`

## Goal

Reduce object-construction and DTO-mapping responsibility inside `AuthService` by introducing static factory methods where they clarify ownership without adding unnecessary extra classes.

## In Scope

- Add a static factory method on `User` for standard app-user registration
- Add DTO `from(...)` methods where `AuthService` currently repeats field mapping
- Refactor `AuthService` and dev seed bootstrap to use the new factories
- Keep API behavior unchanged

## Out of Scope

- New auth features
- Password policy changes
- Repository or controller contract changes
- Introducing a separate `UserFactory` class

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/model/User.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/MeResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/LoginResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/bootstrap/DevUserInitializer.java`

### Mobile

- No direct changes

### Docs

- This execution plan only

### Shared

- No shared contract changes

## Contract Changes

- None. Request and response payloads remain unchanged.

## Security Notes

- Password hashing remains in service/bootstrap before entity creation
- No change to JWT behavior or authz rules

## Maintainability Notes

- Keep `AuthService` focused on orchestration and policy checks
- Put default app-user role selection in the `User` model because that is domain construction logic
- Use DTO factory methods only where they remove repeated mapping, not as blanket style

## Implementation Steps

1. Add `User.register(...)`.
2. Add `MeResponse.from(User)` and `LoginResponse.of(...)` or equivalent.
3. Refactor `AuthService` and `DevUserInitializer` to use the factories.
4. Run backend tests.

## Test Plan

- Run `./gradlew test --no-daemon`

## Review Focus

- No behavior change in signup/login/me flows
- Construction ownership is clearer and less duplicated
- No unnecessary abstraction was introduced

## Worktree Split Decision

Single lane

This is a small shared auth refactor touching the same files and does not benefit from parallel lanes.

## Commit Plan

- Commit 1: auth factory-method refactor

## Open Questions

- None for the scoped change

## Assumptions

- A static factory on the entity is sufficient and preferable to a separate factory class for the current complexity level
