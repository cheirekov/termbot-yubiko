# Ticket: TKT-0256 — Release-candidate smoke gate automation script

## Context capsule (must be complete)
### Goal
- Reduce manual release drift by adding a lightweight script/checklist runner that standardizes RC smoke gate execution and evidence capture.

### Scope
- In scope:
  - Define a script-assisted RC smoke process using existing sentinel matrix (`SK-01..SK-08`).
  - Standardize output artifact location and naming for smoke evidence.
  - Document pass/fail gating conditions for RC readiness.
- Out of scope:
  - Full device automation of security-key taps/touches.

### Constraints
- Platform/runtime constraints:
  - Keep Docker-first build command as prerequisite.
- Security/compliance constraints:
  - Smoke artifacts must avoid secret leakage.
- Do NOT break:
  - Existing manual smoke process currently used by maintainers.

### Target areas
- Files/modules:
  - `ai_docs/scripts/` (new helper script)
  - `ai_docs/docs/QUALITY_PLAN.md`
  - `ai_docs/docs/RELEASE.md`
- Interfaces/contracts:
  - RC checklist contract and artifact expectations.

### Acceptance criteria
- [ ] Behavior:
  - RC smoke execution follows one repeatable command+checklist path.
- [ ] Tests (or explicit manual verification):
  - One dry run performed and recorded for a debug build.
- [ ] Docs:
  - RC gate instructions updated with command, inputs, and output files.
- [ ] Observability (if relevant):
  - Failures produce structured notes and debug report capture references.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Execute new RC smoke helper script and complete prompted matrix items.
- Expected output(s):
  - Timestamped smoke artifact file with pass/fail entries and linked debug reports for failures.

### Risks / rollout
- Regression areas:
  - Over-automation may give false confidence for steps that still need human hardware interaction.
- Rollback plan:
  - Keep manual matrix process as fallback and mark helper script optional until stable.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0251-security-key-regression-sentinel-matrix.md`
- Related tickets:
  - `TKT-0255`
