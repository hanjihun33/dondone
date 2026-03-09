---
name: implement-checklist
description: Use when an implementer is about to code a scoped DonDone change. Follow this checklist to confirm scope, ownership, contract updates, validation, verification, and documentation before and after editing files. Do not use for PRD scoping, execution-plan writing, or read-only review work.
---

# Implement Checklist

Use this skill while coding, not for planning.

## Before Editing

1. Confirm the active scope matches the ExecPlan.
2. Confirm file/module ownership. If ownership overlaps another lane, stop and resolve it first.
3. Reconfirm:
   - expected behavior
   - contract changes
   - security impact
   - non-functional impact

## While Editing

- Keep controller logic thin.
- Preserve feature-first structure on the backend.
- Validate request DTOs.
- Keep auth rules explicit.
- Hide external integrations behind interfaces.
- Keep UI states explicit for frontend/mobile changes: loading, empty, error, success.
- Preserve backend/mobile contract consistency and required disclaimer messaging.
- Prefer the smallest defensible design that keeps responsibilities clear.
- Replace magic numbers and hardcoded domain values with named constants or clearly owned configuration when that improves readability and change safety.
- Avoid unexplained hardcoded strings for statuses, keys, or messages when they are reused or domain-significant.
- Keep names intention-revealing enough that the next maintainer can understand the role of a function, field, or component quickly.
- Avoid mixing domain logic with transport, framework, or UI glue when one layer can own it clearly.
- Prefer simpler control flow over deep nesting when restructuring is low-risk and improves clarity.
- Do not increase coupling between modules or layers without a clear need.
- Keep the resulting structure testable; avoid designs that make business logic hard to exercise in isolation.
- Avoid spreading one business rule across multiple layers when one owner can hold it clearly.
- Reduce duplication only when it improves clarity for this scoped change; do not perform broad cleanup work.
- Leave brief comments only where future readers would otherwise miss an important constraint or non-obvious tradeoff.
- Avoid unrelated refactors.

## Before Finishing

Check whether the change also requires updates to:

- tests
- OpenAPI or API-facing docs
- plan or review notes
- mobile/backend contracts on the opposite surface

Run the narrowest useful verification commands for the changed scope. If they cannot run, record the blocker explicitly.

## Output

Return a short completion note in Korean with:

- files changed
- contract changes made
- tests run
- blocked verification or follow-up items
- docs updated
- maintainability notes
- remaining risks
