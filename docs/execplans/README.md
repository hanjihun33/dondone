# Execution Plans

Store non-trivial implementation plans here with a simple lifecycle structure.

## Directories

- `active/`: current implementation plans
- `archive/`: completed, replaced, or stale plans worth keeping

## Naming

Use date-prefixed names:

- `2026-03-09-workproof-backend.md`
- `2026-03-09-wage-shield-analysis.md`
- `2026-03-09-mobile-proof-ui.md`

## Content

Each plan should cover:

- goal
- scope and out-of-scope
- affected modules
- contract changes
- security notes
- implementation steps
- test plan
- review focus
- worktree split decision
- commit plan

Prefer updating one active plan per task instead of creating multiple competing versions.
