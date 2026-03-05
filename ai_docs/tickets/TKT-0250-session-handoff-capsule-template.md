# TKT-0250 - Session handoff capsule template and adoption

## Status: DONE
## Priority: HIGH
## Epic: TKT-0248

## Context capsule (must be complete)
### Goal
- Standardize agent/user handoff so long-running work can resume without context loss.

### Scope
- In scope:
- Add a short handoff template in `ai_docs/docs/HANDBOOK.md`.
- Include required fields: last known good build, active risks, open review items, next 1-3 actions.
- Add one example filled capsule.
- Out of scope:
- Changing ticket execution logic.
- Rewriting historical tickets.

### Constraints
- Platform/runtime constraints:
- Docs-only change.
- Security/compliance constraints:
- No secret values in template examples.
- Do NOT break:
- Existing handbook structure and brevity.

### Target areas
- Files/modules:
- `ai_docs/docs/HANDBOOK.md`
- `ai_docs/STATE.md` (pointer to handoff section if needed)
- Interfaces/contracts:
- End-of-session handoff ritual.

### Acceptance criteria
- [x] Behavior:
- Handoff template exists and can be used in <2 minutes.
- [x] Tests (or explicit manual verification):
- Manual dry-run handoff filled from current state.
- [x] Docs:
- Ticket + board + state updated.
- [x] Observability (if relevant):
- N/A

### Verification (token-efficient)
- Docker command(s) to run:
- None.
- Manual script(s) the user can run:
- Open handbook and copy/fill the template.
- Expected output(s):
- Clear concise handoff capsule with no ambiguity.

### Risks / rollout
- Regression areas:
- Template may become too long and ignored.
- Rollback plan:
- Trim template fields to minimal required set.

## Notes
- Links:
- `ai_docs/docs/RETRO_2026-03-05_AI_PROCESS.md`
- Related tickets:
- `TKT-0248`

## Delivered Artifacts
- `ai_docs/docs/HANDBOOK.md` (2-minute session handoff capsule template + completed example)
- `ai_docs/boards/DELIVERY_BOARD.md` (ticket moved to Done)
- `ai_docs/STATE.md` (active work and next actions refreshed)
