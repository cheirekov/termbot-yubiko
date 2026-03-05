# Decision Log

## 2026-02-27 — Pack installed / initialized
- Initialize local AI operating pack (ai_docs/ + references/).

## 2026-03-05 — Process reliability guardrails for long sessions
- Add a recurring AI-process retro checkpoint to control context drift.
- Treat review-column WIP limits and session handoff capsules as first-class process controls.

## 2026-03-05 — Review queue policy activated
- Enforce `Review` WIP limit of 2 tickets with oldest-first closure.
- Apply transition freeze when historical `Review` queue is already above the limit.

## 2026-03-05 — Standardized session handoff capsule adopted
- Add a 2-minute handoff template in handbook with required fields: last good build, active risks, open review items, and next actions.

## 2026-03-05 — Security-key sentinel smoke matrix adopted
- Treat `SK-01..SK-08` matrix as mandatory release smoke gate for OpenPGP/PIV/FIDO2 and ProxyJump key-auth paths.
- Require immediate in-app debug report export on first smoke failure.
