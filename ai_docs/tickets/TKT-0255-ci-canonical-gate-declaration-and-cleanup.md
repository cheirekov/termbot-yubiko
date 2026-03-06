# Ticket: TKT-0255 — CI canonical gate declaration and cleanup

## Context capsule (must be complete)
### Goal
- Establish a single canonical CI gate for merge/release confidence and reduce drift from legacy parallel pipelines.

### Scope
- In scope:
  - Inventory active CI definitions and classify canonical vs legacy.
  - Define required gate checks for PR merge and release candidate promotion.
  - Update docs/board to reflect canonical CI policy.
- Out of scope:
  - Rewriting all CI jobs in one ticket.

### Constraints
- Platform/runtime constraints:
  - Must preserve Docker-first build expectation from workspace hard rules.
- Security/compliance constraints:
  - No secrets in workflow docs/log examples.
- Do NOT break:
  - Existing working GitHub Actions flow confirmed by user.

### Target areas
- Files/modules:
  - `ai_docs/docs/WORKFLOW_CONTRACT.md`
  - `ai_docs/docs/QUALITY_PLAN.md`
  - `ai_docs/docs/RELEASE.md`
  - Optional CI metadata files in `repos/termbot-termbot/`
- Interfaces/contracts:
  - PR/release gate policy used by maintainers.

### Acceptance criteria
- [ ] Behavior:
  - Team has one documented canonical CI gate path.
- [ ] Tests (or explicit manual verification):
  - Canonical gate command/check list is runnable and references current repo reality.
- [ ] Docs:
  - CI policy updated in workflow/quality/release docs.
- [ ] Observability (if relevant):
  - CI failure ownership and triage path stated.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Trigger canonical pipeline on a test branch and verify required checks match documented gate.
- Expected output(s):
  - Canonical gate status is explicit and reproducible.

### Risks / rollout
- Regression areas:
  - Premature deprecation of still-needed legacy jobs.
- Rollback plan:
  - Keep legacy jobs as non-blocking until canonical gate proves stable.

## Notes
- Links:
  - `repos/termbot-termbot/.github/workflows/`
  - `repos/termbot-termbot/Jenkinsfile`
  - `repos/termbot-termbot/.travis.yml`
- Related tickets:
  - `TKT-0256`
