---
name: prd-breakdown
description: Use when starting feature work from docs/DonDone_PRD_v1.5.md or another DonDone planning document. Extract the exact scope, acceptance criteria, affected modules, contract changes, security impact, and open questions before implementation begins. Do not use once scope is already fixed and the task is in active implementation, test-only work, or final review.
---

# PRD Breakdown

Use this skill before any implementation or estimate. Treat the PRD as the source of truth unless the user explicitly overrides it.

## Inputs

- Target PRD section, epic, or feature name
- Requested priority slice such as `P0` only
- Optional target surface: backend, mobile, docs, or cross-cutting

## Process

1. Read only the PRD sections needed for the requested scope.
2. Identify:
   - user-facing behavior
   - explicit in-scope items
   - explicit out-of-scope items
   - affected backend/mobile/docs modules
   - DTO/API/DB contract implications
   - auth/authz or exposed-path impact
   - unanswered questions that block safe implementation
3. If the PRD is ambiguous, state assumptions explicitly instead of guessing silently.
4. Prefer a compact result that can feed directly into `execplan-writer`.

## Output

Return a structured breakdown with these headings:

- `PRD Evidence`
- `Scope`
- `Acceptance Criteria`
- `Affected Areas`
- `Contract Changes`
- `Security Impact`
- `Non-Functional Impact`
- `Open Questions`
- `Assumptions`

Under `Affected Areas`, separate:

- `Backend`
- `Mobile`
- `Docs`
- `Shared`

## DonDone Notes

- Prioritize `P0` flows unless the user says otherwise.
- Keep wage-related outputs evidence-first and avoid presenting anomaly checks as final legal or financial judgments.
- Do not expand testnet/demo remittance into real-money behavior.
