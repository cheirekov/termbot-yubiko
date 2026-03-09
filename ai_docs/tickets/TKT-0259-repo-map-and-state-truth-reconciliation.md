# Ticket: TKT-0259 — Repo map and state truth reconciliation

## Context capsule (must be complete)
### Goal
- Eliminate documentation drift between configured repo map, workspace reality, and STATE truth so future continuation work is grounded in accurate inputs.

### Scope
- In scope:
  - Reconcile `ai_docs/config/repos.list` with repositories actually present in workspace.
  - Align `ai_docs/STATE.md` repo map and related docs to the reconciled truth.
  - Define a lightweight snapshot hygiene step for future sessions.
- Out of scope:
  - Restoring or migrating historical external repos.

### Constraints
- Platform/runtime constraints:
  - Keep multi-repo support structure intact even if currently single-repo active.
- Security/compliance constraints:
  - No sensitive local-path disclosure beyond existing workspace conventions.
- Do NOT break:
  - Existing bootstrap/snapshot scripts used by workflow.

### Target areas
- Files/modules:
  - `ai_docs/config/repos.list`
  - `ai_docs/STATE.md`
  - `ai_docs/docs/ONBOARDING.md` or workflow docs (if repo-map guidance needs sync)
- Interfaces/contracts:
  - Session start ritual assumptions about available repos/snapshots.

### Acceptance criteria
- [ ] Behavior:
  - Repo map reflects actual workspace and current runtime direction.
- [ ] Tests (or explicit manual verification):
  - Snapshot scripts run without referencing missing repo paths.
- [ ] Docs:
  - Questions/decisions resolved and recorded.
- [ ] Observability (if relevant):
  - N/A.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ai_docs/scripts/snapshot_all.sh`
- Manual script(s) the user can run:
  - Confirm generated snapshot folders match active repos list.
- Expected output(s):
  - No missing-path errors and consistent repo inventory references.

### Risks / rollout
- Regression areas:
  - Historical references to removed repo entries in older docs.
- Rollback plan:
  - Reintroduce prior repo entry as explicitly marked archival-only metadata.

## Notes
- Links:
  - `ai_docs/docs/QUESTIONS.md`
- Related tickets:
  - `TKT-0255`
