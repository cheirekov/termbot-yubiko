# Hard Rules for Continuation Engineering (Anti‑Hallucination)

## R0 — Grounding & evidence
- Never assume requirements, files, APIs, environments exist.
- Reference **file paths + symbols** for claims about code.
- If missing, write questions to `ai_docs/docs/QUESTIONS.md` and stop.

## R1 — Context capsule is mandatory
Every ticket must include the “context capsule” (goal/scope/constraints/acceptance/verification/risks).
Template: `ai_docs/tickets/TEMPLATE.md`

## R2 — Small PRs only
Prefer PRs that touch:
- ≤ 5 files, or
- ≤ 300 LOC change

## R3 — No time magic
- Use absolute dates (YYYY-MM-DD).
- Record decisions as ADRs with dates.

## R4 — No silent defaults
- If there is a tradeoff, create an ADR (`ai_docs/docs/adr/`).
- Present 2–3 options with pros/cons.

## R5 — Output contract (all agents)
Every response must include:
1) Summary
2) Evidence / assumptions
3) Deliverable (doc/plan/diff)
4) Risks & mitigations
5) Next questions OR patch

## R6 — Stop conditions
Stop and ask if:
- business rules/expected behavior are unclear
- external interfaces are uncertain (APIs, schemas, auth)
- runtime/deploy model is unknown and affects design

## R7 — Safety
- Never print secrets/tokens/private keys.
- Prefer Docker runner for tests to keep host clean (see DOCKER_TESTING.md).


## R8 — STATE.md is mandatory truth
- The authoritative “Now” file is `ai_docs/STATE.md`.
- After meaningful work, update STATE.md (or assign a State Steward).
- Keep STATE.md small; move details to docs and link.
- If STATE.md grows, run: `ai_docs/scripts/state_compact.sh` (archives old state).


## R9 — STATE size gate (prevent context bloat)
- Run `ai_docs/scripts/state_check.sh` regularly (or in CI).
- If it fails, run `ai_docs/scripts/state_compact.sh` and move details into docs/ADRs.


## R10 — Clean host policy (Docker-first)
- Default: **do not install new toolchains on the host** (Android SDK, Gradle, Node, etc.).
- Prefer Docker runners:
  - generic: `ai_docs/scripts/docker_run.sh`
  - Android: `ai_docs/scripts/android_docker_build.sh`
- If a host install is absolutely necessary (rare), it must be:
  1) recorded in `ai_docs/docs/HOST_DEPS.md` (what/why/how to remove),
  2) approved explicitly by the user,
  3) minimized to the smallest tool required.


## R11 — Mandatory closeout gate (prevents forgetting)
After each ticket/slice:
- Update DELIVERY_BOARD.md
- Update BUILD_STATUS.md if build/tests ran
- Update QUESTIONS.md if unknowns found
- Update STATE.md only if truth changed
- Run: `ai_docs/scripts/closeout_check.sh <TICKET_ID>`
If the gate fails, fix missing docs and re-run.
