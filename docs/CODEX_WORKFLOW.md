# Codex Workflow for DonDone

This document describes the intended Codex operating model for this repository.

## Audience

This is both:

- a team-facing workflow guide
- an agent-facing reference for tasks that change workflow tooling, skills, multi-agent setup, or documentation process

Agents do not need to read this for routine feature implementation unless the task touches workflow setup itself or `AGENTS.md` explicitly points them here.

## Layers

- `AGENTS.md`: repository rules and permanent guardrails
- `.agents/skills/`: reusable workflow playbooks
- `.codex/config.toml`: multi-agent role configuration
- `docs/execplans/`: implementation plans by lifecycle
- `docs/reviews/`: review notes and follow-up findings by lifecycle

## Default Flow

1. Run `prd-breakdown` on the target PRD scope.
2. Use `explorer` sub-agents to gather backend, mobile, and contract impact when the task is non-trivial.
   - split at least by backend and mobile when both surfaces are affected
   - add a cross-cutting exploration pass when DTO, auth, security, or shared contract risk exists
3. Write an execution plan with `execplan-writer`.
4. Decide whether the task is single-lane or safe for parallel worktrees.
5. Implement:
   - use the main thread for small, clearly local changes
   - use `implementer` when a scoped implementation owner should work independently, especially in worktree-based parallel lanes
   - always follow `implement-checklist`
6. Add or update verification:
   - use `tester` as a separate sub-agent when DTO/API contracts, auth, validation, or major UI state transitions changed
   - otherwise a narrow verification pass in the implementation lane is acceptable
   - always follow `test-checklist`
7. Review with sub-agents:
   - always use `reviewer` for non-trivial review
   - additionally use `security_reviewer` when auth, authz, token, exposed-path, or sensitive-data impact exists
   - when both are used, wait for both results and merge the findings in the main thread
   - use `review-checklist` as the review playbook for each review agent
8. Use `docs_writer` when execution plans, review notes, or contract-facing docs need structured updates after implementation or review.
9. Prepare focused commits with `commit-grouping`.

## Document Lifecycle

### Execution Plans

- Put active plans in `docs/execplans/active/`.
- Use `docs/execplans/archive/` for completed, replaced, or stale plans.
- Recommended filename: `YYYY-MM-DD-feature-name.md`
- Prefer updating an existing active plan for the same task rather than creating duplicates.

### Review Notes

- Put active review artifacts in `docs/reviews/active/`.
- Use `docs/reviews/archive/` for closed or superseded review notes.
- Recommended filename: `YYYY-MM-DD-feature-name-review.md`
- Prefer one review note per task or branch unless security review must stand alone.

## Maintenance Rule

When creating or moving plan or review documents:

- keep the active directories small
- archive stale artifacts instead of deleting them when they still have historical value
- avoid multiple active documents that describe the same change set
- link the review note back to its execution plan when both exist

## Document Language

For `docs/execplans/` and `docs/reviews/` artifacts, default to Korean so the team can read them directly.

- Keep section titles and body text in Korean by default.
- Keep code identifiers, file paths, branch names, and command lines in their original technical form.
- If English source terminology is clearer, keep the term and explain it briefly in Korean on first use.

## Worktree Rule

Only split work into parallel git worktrees after the execution plan explicitly lists:

- lane owners
- branch names
- worktree paths
- file or module ownership

Keep auth/security, shared DTOs, common response contracts, and shared entities in a single lane unless the contract is already frozen.
