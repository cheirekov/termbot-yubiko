# TKT-0249 - Review WIP limit and board policy hardening

## Status: DONE
## Priority: HIGH
## Epic: TKT-0248

## Context capsule (must be complete)
### Goal
- Reduce closure latency and context drift by limiting the number of concurrent tickets in `Review`.

### Scope
- In scope:
- Add explicit board policy: max 2 items in `Review`.
- Define tie-break rule (oldest review ticket closes first).
- Update process docs where board policy is defined.
- Out of scope:
- Changing ticket technical content.
- Any app code changes.

### Constraints
- Platform/runtime constraints:
- Documentation-only process change.
- Security/compliance constraints:
- No secrets in docs/examples.
- Do NOT break:
- Existing delivery board format.

### Target areas
- Files/modules:
- `ai_docs/boards/DELIVERY_BOARD.md`
- `ai_docs/docs/WORKFLOW_CONTRACT.md` (or dedicated board policy section)
- Interfaces/contracts:
- Team closeout ritual and board movement policy.

### Acceptance criteria
- [x] Behavior:
- A clear `Review` WIP limit policy is documented and visible from board/process docs.
- [x] Tests (or explicit manual verification):
- Manual check confirms policy text exists and is unambiguous.
- [x] Docs:
- Ticket + board + state updated.
- [x] Observability (if relevant):
- N/A

### Verification (token-efficient)
- Docker command(s) to run:
- None.
- Manual script(s) the user can run:
- Read board/process docs and verify WIP limit text.
- Expected output(s):
- Policy present and actionable.

### Risks / rollout
- Regression areas:
- Over-constraining flow if urgent hotfixes stack in review.
- Rollback plan:
- Remove/relax WIP limit rule in docs.

## Notes
- Links:
- `ai_docs/docs/RETRO_2026-03-05_AI_PROCESS.md`
- Related tickets:
- `TKT-0248`

## Delivered Artifacts
- `ai_docs/boards/DELIVERY_BOARD.md` (policy section + ticket moved to Done)
- `ai_docs/docs/WORKFLOW_CONTRACT.md` (board movement policy section)
- `ai_docs/docs/HARD_RULES.md` (R12 review queue control)
- `ai_docs/STATE.md` (active/next actions updated)
