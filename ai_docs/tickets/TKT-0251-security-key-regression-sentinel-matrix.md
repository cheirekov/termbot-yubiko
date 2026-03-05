# TKT-0251 - Security-key regression sentinel smoke matrix

## Status: DONE
## Priority: HIGH
## Epic: TKT-0248

## Context capsule (must be complete)
### Goal
- Add a minimal repeatable smoke matrix to quickly detect regressions across OpenPGP/PIV/FIDO2, NFC/USB, and ProxyJump paths.

### Scope
- In scope:
- Define matrix in docs with required pass cases and expected outcomes.
- Cover import + auth for OpenPGP/PIV/FIDO2.
- Include transport split (NFC and USB where supported) and one ProxyJump auth case.
- Add short execution checklist for manual device smoke.
- Out of scope:
- Full automated instrumentation tests.
- Provider feature expansion.

### Constraints
- Platform/runtime constraints:
- Device/manual smoke first; keep checks short.
- Security/compliance constraints:
- Never capture PIN/private key/APDU data in matrix notes.
- Do NOT break:
- Existing QA workflow and closeout rituals.

### Target areas
- Files/modules:
- `ai_docs/docs/QUALITY_PLAN.md` (matrix section)
- `ai_docs/docs/RELEASE.md` (reference matrix for release gate)
- Interfaces/contracts:
- Manual smoke gate before moving major auth tickets to Done.

### Acceptance criteria
- [x] Behavior:
- Matrix exists with clear pass/fail conditions per provider/transport.
- [x] Tests (or explicit manual verification):
- One dry run completed against a debug build and documented.
- [x] Docs:
- Ticket + board + state updated.
- [x] Observability (if relevant):
- Matrix references debug report export for failure capture.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Execute listed smoke steps and export debug report if any step fails.
- Expected output(s):
- Complete pass/fail matrix record for current build.

### Risks / rollout
- Regression areas:
- Matrix may drift if new provider flows are added without update.
- Rollback plan:
- Keep matrix minimal and versioned with each auth-flow ticket.

## Notes
- Links:
- `ai_docs/docs/RETRO_2026-03-05_AI_PROCESS.md`
- Related tickets:
- `TKT-0248`

## Delivered Artifacts
- `ai_docs/docs/QUALITY_PLAN.md` (security-key sentinel matrix + execution checklist + baseline dry run)
- `ai_docs/docs/RELEASE.md` (release checklist + RC gate references to sentinel matrix)
- `ai_docs/boards/DELIVERY_BOARD.md` (ticket moved to Done)
- `ai_docs/STATE.md` (active work and next actions updated)
