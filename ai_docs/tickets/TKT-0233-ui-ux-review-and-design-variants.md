# TKT-0233 — Professional UI/UX review and design variants

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Problem
Core functionality is stable, but UI/UX quality is uneven after rapid security-key iterations.

## Goal
Run a structured UI/UX pass and produce multiple design variants for key screens.

## Scope
- In scope:
  - UX audit: Host list, Add/Edit host, Manage Keys, Security-key auth dialogs, Backup/Import.
  - Dedicated redesign of the security-key waiting state with a prominent touch/USB guidance panel (large visual cue, not micro-icon only).
  - 2–3 visual variants (style directions) with component specs.
  - Mobile accessibility review (contrast, focus order, touch targets, text scale).
  - Implementation plan with phased rollout and low-risk migration path.
- Out of scope:
  - Full redesign implementation in one batch.
  - Backend protocol changes.

## Deliverables
- UI audit report (current issues + severity + quick wins).
- Variant mockups/spec notes (tokens, typography, spacing, states).
- Specific variant set for "waiting for key" and "tap/insert key" states in OpenPGP/PIV/FIDO2 flows.
- Prioritized implementation backlog (small/medium/large items).

## Acceptance Criteria
- [x] Audit completed with actionable findings.
- [x] At least 2 viable design variants documented.
- [x] Implementation backlog linked to tickets.

## Delivered Artifacts
- `ai_docs/docs/UI_UX_AUDIT_2026-03-04.md`
- `ai_docs/docs/UI_VARIANTS_2026-03-04.md`

## Linked Backlog Tickets
- `TKT-0237` Security-key import UX parity and waiting-state redesign
- `TKT-0238` Theme token and component refresh
- `TKT-0239` Backup import/export UX hardening
- `TKT-0225` Host grouping/folders (already planned)
- `TKT-0234` App icon rebrand/store assets (already planned)
