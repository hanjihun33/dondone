# DonDone Backend Agent Guide

## Goal
Implement backend features for DonDone based on `docs/DonDone_PRD_v1.5.md`.

## Current Scope
- Feature-first skeleton modules
- JWT login/auth baseline
- P0-first delivery (WorkProof, Wage Shield, Documents, Remittance)

## Rules
- Keep package structure per feature (`api/service/repo/model/adapter`).
- Do not hardcode secrets; use env/config.
- Keep security endpoints explicit in `SecurityConfig`.
- Add tests for auth and business-rule changes.
- Treat backend DB schema as migration-owned; do not rely on Hibernate `ddl-auto: update` for backend schema changes.
- Use `db-migration-checklist` when a backend task adds, removes, renames, or retypes tables/columns, or changes indexes, constraints, Flyway config, or persistence-owned enums.
- Keep migration files and related entity/service changes in the same change set.

## Run
- `./gradlew bootRun`
- `./gradlew test`
