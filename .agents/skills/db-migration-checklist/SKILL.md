---
name: db-migration-checklist
description: Use when a DonDone backend task changes persistence structure or schema management, including adding, removing, renaming, or retyping tables or columns, changing indexes, constraints, triggers, Flyway migrations, persistence-owned enums, or switching away from Hibernate ddl-auto schema updates. Create or update migration-first changes, keep code and migration in the same change set, and run the minimum backend verification.
---

# DB Migration Checklist

Use this skill while implementing backend schema-related changes.

## Goal

Translate backend persistence changes into migration-first changes that are reviewable, reproducible, and safe for team collaboration.

## Before Editing

1. Reconfirm:
   - expected behavior
   - affected modules and files
   - contract changes (`DB schema`, `DTO`, `API response`)
   - security impact
   - non-functional impact
2. Confirm whether the task changes:
   - tables
   - columns
   - indexes
   - constraints
   - triggers
   - persistence-owned enums
   - `ddl-auto` or Flyway configuration
3. Inspect the current entity or model code and existing migration files together.
4. Keep shared entity and schema changes in a single lane unless the contract is already frozen.

## Migration Rules

- Treat DB schema as migration-owned.
- Do not rely on Hibernate `ddl-auto: update` for schema creation or cleanup.
- Add a new migration file instead of editing a shared migration.
- Use the filename format:
  - `V{YYYYMMDDHHMMSS}__{snake_case_description}.sql`
- Keep the migration description short and intention-revealing.
- Prefer one logical schema change per migration when practical.

## Change Patterns

### Additive Changes

- Prefer `ADD COLUMN`, new index, or new constraint first.
- Keep existing code paths working until the new structure is wired in.

### Destructive Changes

- For drop, rename, or incompatible type changes, use staged changes:
  1. add the new structure
  2. backfill or copy data if needed
  3. switch application code
  4. remove the old structure in a later migration

### Constraints

- Put important integrity rules in the database when feasible:
  - `FK`
  - `unique`
  - `check`
  - `index`
  - `partial unique index`

### Data Movement

- If a migration depends on existing data, state the data assumption clearly.
- Separate heavy backfill logic from simple schema creation when that improves safety and reviewability.

## Code Synchronization

- Keep entity, repository, service, and migration changes aligned in the same change set.
- Do not leave code expecting columns that are not created by migration.
- Do not leave migrations introducing columns that no code owns without calling that out explicitly.
- Update related validation, DTO, and API contract code when persistence shape changes affect them.

## Verification

- Run the narrowest useful backend verification for the change.
- Run `./gradlew test` when the scope is non-trivial and Java is available.
- When possible, verify that the migration sequence is understandable from baseline to latest.
- If verification cannot run, record the blocker explicitly.

## Output

Return a short completion note in Korean with:

- `Migrations Added`
- `Schema Changes`
- `Code Changes`
- `Contract Changes`
- `Tests Run`
- `Blocked Verification`
- `Follow-up Migration Needed`
- `Remaining Risks`
