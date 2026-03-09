## Goal
- Reframe DonDone P0 advance policy in the PRD from a broad "alternative credit scoring" narrative to an evidence-first advance eligibility and repayment risk policy.
- Reflect the agreed approach in PRD sections 7B, 7C, 12, and 14 without expanding P0 into real-money settlement behavior.

## In Scope
- Update PRD wording for WorkProof so it clearly separates recorded work, reflected work, verified work, and advance-eligible work.
- Update P0 advance policy wording to describe a two-layer engine:
  - WorkProof integrity
  - repayment confidence
- Add hard-stop policy examples for last-pay-cycle and contract-end risk handling.
- Update disclaimers and risk language so advance is framed as a demo eligibility policy, not a general-purpose credit score.

## Out of Scope
- Backend or mobile implementation changes
- New DTO, API, DB schema, or auth changes
- P1 open banking, MyData, payroll integration, or real repayment execution logic
- Utility/telecom data integration beyond noting it as a future auxiliary signal

## Affected Modules
- `docs/DonDone_PRD_v1.5.md`

## Contract Changes
- No API or DB contract changes in this task
- PRD terminology will introduce policy outputs that future implementation may adopt:
  - `verified_hours`
  - `pending_hours`
  - `repayment_tier`
  - `block_reason_codes`

## Security Notes
- Keep the PRD explicit that P0 is demo-only and not real financial service execution.
- Avoid automatic payroll-offset wording; use linked account repayment consent / repayment attempt wording instead.
- Preserve evidence-first and explainable-policy framing to reduce misuse and overclaim risk.

## Implementation Steps
1. Update WorkProof section to define integrity-based verification outputs for advance eligibility.
2. Update Advance section to define the P0 policy as eligibility and repayment confidence, plus hard-stop rules.
3. Update disclaimers to avoid "automatic deduction" implications and reinforce demo-only scope.
4. Update risk/open-issue language to reflect the new framing and agreed policy defaults.

## Test Plan
- Manual review of the edited PRD sections for consistency:
  - `7B`
  - `7C`
  - `12.2`
  - `14`
  - related open-issue wording if touched

## Review Focus
- Does the PRD now avoid broad alternative-credit-score claims?
- Is the P0 scope still clearly demo-only?
- Are last-pay-cycle and contract-end rules stated as explainable policy examples rather than hardcoded production commitments?
- Is utility/telecom data clearly demoted to P1 auxiliary-signal status?

## Worktree Split Decision
- Single lane
- Shared terminology in one PRD file is still moving, so parallel lanes are not worktree-safe for this task.

## Commit Plan
- `docs: reframe advance policy around eligibility and repayment risk`

## Open Questions
- Exact user-facing exposure level for `verified_hours`, `pending_hours`, and reason codes remains a UX decision.

## Assumptions
- P0 keeps using demo seed data and `asOf` time-travel rather than adding an admin control surface.
- "Last pay cycle" should be expressed as a policy example:
  - warn or reduce within 30 days of contract end
  - block within 14 days of contract end or last pay cycle entry
- The term "credit scoring" should be removed from outward-facing PRD language for this flow.
