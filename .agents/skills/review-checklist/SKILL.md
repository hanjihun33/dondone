---
name: review-checklist
description: Use when reviewing a DonDone change set. Review for correctness, regressions, missing tests, contract drift, maintainability, and security risk, then report findings first with concrete file references and only brief secondary summary. Do not use for implementation planning or simple diff summarization.
---

# Review Checklist

For non-trivial reviews, do not keep all review work in the main thread. Spawn the appropriate review sub-agents and merge their findings after they complete.

## Review Agent Split

- Always use `reviewer` for correctness, regressions, contract drift, maintainability, and test gaps.
- Also use `security_reviewer` when the change affects auth, authz, tokens, exposed endpoints, secrets, or sensitive data handling.
- If the change is tiny and clearly local, a single `reviewer` pass is enough.
- The main thread should aggregate findings, remove duplicates, and produce the final review summary.

## Primary Review Questions

- Does the implementation match the accepted scope?
- Are there behavior regressions or edge cases not handled?
- Did DTO/API/DB changes stay consistent across layers?
- Are validation and auth rules still correct?
- Are required tests missing or too weak?
- Did the change introduce avoidable complexity?
- Did the change make ownership boundaries less clear or increase maintenance cost without enough payoff?
- Did the change break evidence-first positioning, required disclaimer text, or testnet-only scope boundaries?
- Did the change introduce backward-compatibility risk, noisy logging, or performance-sensitive regressions that matter for this flow?
- Did the change introduce magic numbers, unexplained hardcoded strings, or hidden constants that should be named or centralized?
- Do names still explain intent clearly enough for the next maintainer?
- Does any function, class, or component now carry too many responsibilities?
- Did the change add needless duplication, deep branching, or tight coupling that will make future changes harder?
- Did the structure become harder to test because logic is mixed with framework, UI, or transport concerns?

## Review Mode

- Findings first
- Highest severity first
- Concrete file references
- No style-only comments unless they hide a real defect
- Treat maintainability as review-worthy when it creates future bug risk, unclear ownership, needless duplication, or hard-to-change structure.
- Treat magic numbers, hardcoded domain values, unclear naming, and responsibility creep as review-worthy when they reduce readability, change safety, or testability.

## Output Format

Write the review result in Korean by default.

- `Findings`
- `Open Questions`
- `Testing Gaps`
- `Residual Risks`
- `Change Summary`

If there are no findings, say so explicitly and still mention residual risks or testing gaps.
