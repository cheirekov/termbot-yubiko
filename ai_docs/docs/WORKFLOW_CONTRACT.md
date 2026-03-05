# Workflow Contract (Start → Execute → Close) — Hard Process Rules

Date: 2026-02-27

This is a **mechanical process** designed to prevent “LLM forgetting” after N tickets.

## The invariant
Every change must leave the workspace in a **consistent documented state**.

## Start ritual (per session)
1) Run bootstrap (existing or test build):
   - `ai_docs/scripts/bootstrap_existing.sh` OR `ai_docs/scripts/bootstrap_test_build_android.sh`
2) IDE agent opens:
   - `ai_docs/STATE.md`
   - `ai_docs/docs/HARD_RULES.md`
   - `ai_docs/docs/EXISTING/00_intake.md`
   - `references/*/SNAPSHOT.txt`

## Execute ritual (per ticket)
Work only from a ticket file in `ai_docs/tickets/` and keep PR-sized diffs.

## Board movement policy (WIP control)
- `Review` column WIP limit is 2 tickets.
- Tie-break rule: close the oldest `Review` ticket first.
- Transition rule (from 2026-03-05): if `Review` already has more than 2 tickets, do not move new tickets into `Review` until the queue is reduced to 2.

## Closeout ritual (MANDATORY per ticket)
After finishing a ticket (or a small ticket slice), the agent must:
1) Update at least one:
   - `ai_docs/boards/DELIVERY_BOARD.md` (move ticket state)
   - `ai_docs/docs/BUILD_STATUS.md` (if any build/test was run)
   - `ai_docs/docs/QUESTIONS.md` (if any unknowns found)
   - `ai_docs/STATE.md` (ONLY if truth changes)
   - `ai_docs/docs/HOST_DEPS.md` (ONLY if host install happened — discouraged)
2) Run the mechanical gate:
   - `ai_docs/scripts/closeout_check.sh <TICKET_ID>`

If the gate fails, fix missing documentation and re-run.

## Why this works
- The agent can forget instructions, but it cannot “forget” a failing gate.
- The repo stays auditably consistent after each change.
