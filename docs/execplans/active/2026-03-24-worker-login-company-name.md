# Source Inputs
- User report: worker sees no company name after logout/login
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/service/AuthService.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/LoginResponse.java`
- `apps/dondone-backend/src/main/java/com/workproofpay/backend/auth/api/dto/response/MeResponse.java`
- `apps/dondone-mobile/android/app/src/main/java/com/dondone/mobile/data/auth/BackendAuthRepository.kt`

# Goal
- Keep worker company name visible after re-login.

# In Scope
- Derive worker company info from active employment membership during auth/me responses.
- Persist returned company name into mobile auth session on login/update-profile.
- Add narrow regression tests.

# Out of Scope
- Employer auth/profile flows
- Workplace-name restoration for worker login
- Multi-company worker selection UX

# Assumptions
- A worker has at most one current active membership that should drive the displayed company identity.
- Showing company name only is sufficient for the current mobile worker UX.

# Contract Changes
- Add optional `companyName` to worker auth/me responses.
- Existing fields remain unchanged; change is additive.

# Security Notes
- No auth/authz rule changes.

# Test Plan
- Backend integration tests for login/me after worker company registration redemption.
- Mobile targeted unit tests or compile-level verification for affected auth/session flow.

# Worktree Split Decision
- Single lane
