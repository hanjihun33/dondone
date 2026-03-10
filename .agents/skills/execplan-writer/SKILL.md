---
name: execplan-writer
description: Use when a DonDone task has been scoped and you need an implementation plan document under docs/execplans. Produce a concise execution plan covering scope, affected files/modules, contract changes, test strategy, review focus, and whether the work can be split across git worktrees. Do not use for tiny one-file fixes that do not need durable planning.
---

# ExecPlan Writer

Create a compact execution plan before coding. The plan should be specific enough that an implementer, tester, and reviewer can work from the same document.

Write the resulting document in Korean by default so both teammates and agents can use it directly. Keep code identifiers, paths, branch names, and commands in their original technical form.

## Target Path

Write to `docs/execplans/active/YYYY-MM-DD-<feature-name>.md` unless the user specifies a different path.

## Required Sections

- `Source Inputs`
- `Goal`
- `In Scope`
- `Out of Scope`
- `Affected Modules`
- `Contract Changes`
- `Security Notes`
- `Maintainability Notes`
- `Implementation Steps`
- `Test Plan`
- `Review Focus`
- `Worktree Split Decision`
- `Commit Plan`
- `Open Questions`
- `Assumptions`

## Source Inputs

List the concrete planning inputs used to create the plan, such as:

- PRD sections
- prior PRD breakdown output
- codebase exploration notes
- existing contract or schema references

## Affected Modules

Split this section into:

- `Backend`
- `Mobile`
- `Docs`
- `Shared`

## Maintainability Notes

Capture only the maintainability concerns that materially affect implementation or review, such as:

- ownership boundaries that must stay clear
- duplication that should be avoided during this task
- complexity hotspots that should not grow further
- required refactor boundaries if a small cleanup is necessary for safe implementation

Do not turn this section into a generic style guide. Keep it tied to the specific change.

## Worktree Split Decision

State one of:

- `Single lane`
- `Parallel lanes allowed`

If parallel lanes are allowed, list:

- each lane owner
- branch name
- worktree directory
- files/modules each lane owns
- what must be merged first if lanes have a dependency order

State the reason for the decision in one short paragraph.

Do not mark work as parallel-safe when shared DTOs, auth/security, shared entities, or common response contracts are still moving.

Default to `Single lane` unless ownership boundaries are concrete and merge risk is low.

## Quality Bar

- Keep the plan short and actionable.
- Tie every section back to evidence from the PRD or codebase exploration.
- Prefer file/module names over vague area names.
- Make the plan precise enough that an implementer, tester, and reviewer can each act without re-scoping the task.
- Call out maintainability risks only when they change how the work should be split, implemented, or reviewed.
