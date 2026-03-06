# DonDone Frontend Agent Guide

## Goal
Implement DonDone frontend flows that align with PRD P0 journey:
workproof -> wage check -> action (Proof Pack / Claim Kit / Instant Claim) -> remittance.

## Rules
- Keep UI state explicit (loading/empty/error/success).
- Match backend DTO contracts exactly.
- Preserve multi-language friendly copy and PRD disclaimer texts.
- Avoid adding dependencies unless required.

## Run
- `npm install`
- `npm run dev`
