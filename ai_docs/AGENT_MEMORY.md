# Agent Memory Card (Paste-invariant)

If you are an IDE agent, treat this file as non-negotiable.

## Always (no exceptions)
- Follow ai_docs/docs/HARD_RULES.md
- Docker-first; do not install toolchains on host
- Work from a ticket (ai_docs/tickets/TKT-*.md)
- Keep diffs small

## After every ticket / slice
- Update DELIVERY_BOARD.md
- Update BUILD_STATUS.md if you ran build/tests
- Update QUESTIONS.md if you discovered unknowns
- Update STATE.md only if “truth now” changed
- Run: ai_docs/scripts/closeout_check.sh <TICKET_ID>
