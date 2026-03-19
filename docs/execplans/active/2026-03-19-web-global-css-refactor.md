# 2026-03-19 Web Global CSS Refactor Plan

## Scope
- apps/dondone-web/src/shared/styles/global.css
- usage verification against apps/dondone-web/src/**/*.tsx

## Goals
- remove selectors that are provably unused
- keep current UI behavior unchanged
- avoid broad risky stylistic rewrites

## Steps
1. Identify candidate unused selectors by searching JSX className references.
2. Remove only selectors with zero references and no dynamic usage path.
3. Run build verification (`npm run build`).
4. Summarize removed selectors and residual risk.

## Risk Notes
- Dynamic className strings may hide usage; exclude selectors used in template literals/conditional classes.
- Prefer conservative cleanup in this pass.
