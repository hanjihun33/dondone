---
name: review-checklist
description: Use when reviewing a DonDone change set. Review for correctness, regressions, missing tests, contract drift, maintainability, and security risk, then report findings first with concrete file references and only brief secondary summary. Do not use for implementation planning or simple diff summarization.
---

# Review Checklist

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

## Review Mode

- Findings first
- Highest severity first
- Concrete file references
- No style-only comments unless they hide a real defect
- Treat maintainability as review-worthy when it creates future bug risk, unclear ownership, needless duplication, or hard-to-change structure.

## Output Format

Write the review result in Korean by default.

- `Findings`
- `Open Questions`
- `Testing Gaps`
- `Residual Risks`
- `Change Summary`

If there are no findings, say so explicitly and still mention residual risks or testing gaps.
