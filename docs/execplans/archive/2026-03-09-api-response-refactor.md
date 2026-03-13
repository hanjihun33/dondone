## Source Inputs

- `S14P11C205/itda-backend` shared response and exception classes
- Current DonDone backend shared response, exception, security filter, and controller code
- Repository `AGENTS.md`
- `apps/dondone-backend/AGENTS.md`

## Goal

Refactor DonDone backend responses into a simpler but stronger shared contract that centralizes HTTP status handling, error codes, and validation details without overcomplicating the MVP codebase.

## In Scope

- Replace the current backend-wide response wrapper with a centralized `ApiResponse` contract
- Add shared success and error code enums for consistent response metadata
- Refactor exceptions to carry typed error codes instead of ad hoc string and status pairs
- Update global exception handling and JWT filter error responses to the new contract
- Update all current controllers to use centralized response factories
- Update backend tests for the new response shape and status semantics

## Out of Scope

- Mobile or mockup client updates
- Endpoint behavior changes unrelated to the response contract
- New business features or DTO shape changes inside `data`
- OpenAPI schema polishing beyond what naturally changes with the new response wrapper

## Affected Modules

### Backend

- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/api/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/exception/**`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/shared/security/**`
- Current controllers under `auth`, `workproof`, `wage`, `demo`, `documents`, `jobs`, `remittance`, `safepay`
- Backend tests asserting response bodies

### Mobile

- No direct code change
- Existing consumers will need to adapt from `{success, code, message, data, timestamp}` to the new contract if they call backend endpoints directly later

### Docs

- This execution plan
- Review note for the refactor outcome

### Shared

- Shared API response contract and typed error codes become the backend-wide default

## Contract Changes

- Remove `success` and `timestamp` from the shared response body
- Standardize the response body as `{ code, message, data, details }`
- Add centralized factory methods:
  - `ApiResponse.success(data)`
  - `ApiResponse.success()`
  - `ApiResponse.created(data)`
  - `ApiResponse.accepted(data)`
  - `ApiResponse.error(errorCode, message, details)`
- Validation failures return structured field-level details
- `code` values become enum-backed rather than ad hoc string literals

## Security Notes

- JWT filter unauthorized responses must use the same shared error contract as controller exceptions
- Auth behavior should not change beyond response body format and code consistency
- No public endpoint allowlist changes are planned

## Maintainability Notes

- Keep the response contract lean; do not duplicate HTTP status inside the body
- Keep one typed source of truth for backend error codes to prevent drift
- Move status/body assembly out of controllers so feature controllers stay thin
- Preserve existing service ownership; only the error signaling mechanism changes

## Implementation Steps

1. Add the exec plan and freeze the target response contract.
2. Introduce shared success and error code enums plus a centralized `ApiResponse`.
3. Refactor `ApiException` to carry typed error codes.
4. Update `GlobalExceptionHandler` and `JwtAuthenticationFilter` to emit the new body shape.
5. Refactor all controllers to use centralized response factories.
6. Update tests asserting the old `success` boolean or loose string codes.
7. Run backend verification and capture migration risks for clients.

## Test Plan

- Update response assertions in auth and Week 1 backend tests
- Verify validation error `details` structure on at least one endpoint
- Verify unauthorized filter responses still return 401 with the new body
- Run `./gradlew test --no-daemon`

## Review Focus

- Response shape is consistent across controllers, exception handler, and JWT filter
- HTTP status and body code semantics do not contradict each other
- Validation details remain machine-readable
- No controller accidentally bypasses the shared response factories

## Worktree Split Decision

Single lane

This refactor touches shared response code, exceptions, security, and multiple controllers at once. Splitting would create avoidable merge risk across the most shared backend surfaces.

## Commit Plan

- Commit 1: shared response/error contract refactor
- Commit 2: controller and test updates
- Commit 3: review note only if needed

## Open Questions

- Whether clients should see `CREATED` as a body code or keep a single `SUCCESS` code with a creation message
- Whether validation `details` should be a list or a field-keyed map long term

## Assumptions

- Backend consumers can absorb a response wrapper change at this stage of MVP development
- A leaner contract than the reference repo is preferable for DonDone as long as it preserves typed error handling and structured validation details
