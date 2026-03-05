# Retro - 2026-03-05 AI Process and Context-Window Review

## Scope
- Period reviewed: 2026-02-27 to 2026-03-05
- Input artifacts:
  - `ai_docs/STATE.md`
  - `ai_docs/boards/DELIVERY_BOARD.md`
  - `ai_docs/tickets/TKT-02xx*.md`
  - `ai_docs/docs/RETRO_2026-03-03.md`
  - `ai_docs/scripts/closeout_check.sh`
- Lens: "AI specialist" process review (delivery reliability, context hygiene, handoff durability)

## Current Snapshot
- Board inventory:
  - Done tickets: 33
  - Review tickets: 5
  - In Progress: 0
  - Ready: 0
  - Backlog: 2
- Process maturity:
  - Strong ticket discipline and closeout gate usage.
  - Stable Docker-first build path in local workflow.
  - High-value observability via in-app debug report.

## What Worked Well
- `STATE.md` stayed compact and operationally useful for rehydration.
- Ticketed closeout pattern prevented silent drift of repo truth.
- User-driven smoke loops (real device) found regressions early.
- Security-key diagnostics moved from adb dependency to in-app exports.

## Where Friction Still Exists
- Long conversation chains increase risk of stale assumptions after context compression.
- "Review" column is overloaded; too many items wait there at once.
- External edits (outside ticket ritual) can bypass guardrails and create regression spikes.
- Process knowledge is spread across many docs; newcomers need a thinner onboarding path.

## How the Process Feels in Practice
- When rituals are followed (ticket -> small diff -> closeout -> state update), execution is fast and reliable.
- Failure mode is not coding complexity; it is context continuity under long, multi-ticket sessions.
- The strongest protection is mechanical (`closeout_check.sh`), not memory.

## Root Causes
- Context-window pressure from long, mixed-intent chat history.
- Parallel concerns (bugfix + UX + CI + docs) compete for short-term memory.
- Board policy does not limit "Review" work-in-flight, so closure latency grows.

## Recommended Improvements (Prioritized)
1. `P0` Add review WIP limit:
   - Rule: max 2 tickets in Review; oldest must close before adding another.
   - Owner: PM/State Steward.
2. `P0` Add "context checkpoint" trigger:
   - After every 3 ticket closeouts, run `state_compact.sh` review and refresh `STATE.md` next actions.
   - Owner: State Steward.
3. `P1` Add session handoff capsule:
   - One short template in `ai_docs/docs/HANDBOOK.md` with "last known good build", "active risks", "blocked items".
   - Owner: Tech Writer + Eng lead.
4. `P1` Add regression sentinel matrix:
   - Minimal smoke script per provider (OpenPGP/PIV/FIDO2, NFC+USB, ProxyJump).
   - Owner: QA/QE.
5. `P2` Add monthly process retro cadence:
   - Short process-only retro independent of feature retros.
   - Owner: PM/BA.

## Suggested Follow-Up Tickets
- `TKT-0249`: Review WIP limit and board policy hardening.
- `TKT-0250`: Session handoff capsule template + adoption.
- `TKT-0251`: Security-key regression sentinel smoke matrix.

## Keep Doing
- Keep `STATE.md` small and authoritative.
- Keep Docker-first build commands explicit in docs.
- Keep non-secret debug markers as first-class diagnostics.
