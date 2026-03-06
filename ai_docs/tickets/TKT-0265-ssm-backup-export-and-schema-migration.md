# Ticket: TKT-0265 — SSM backup/export compatibility + schema migration

## Context capsule (must be complete)
### Goal
- Make SSM host configuration durable across persistence and encrypted backup/restore while remaining backward-compatible.

### Scope
- In scope:
  - Extend host schema for SSM-specific fields.
  - Add DB migration path for existing users.
  - Include SSM host metadata in encrypted backup export/import flows.
  - Validate backward compatibility for non-SSM hosts.
- Out of scope:
  - Non-host secret escrow/rotation systems.

### Constraints
- Platform/runtime constraints:
  - Migration must work from current DB versioned path without data loss.
- Security/compliance constraints:
  - Backup artifacts must not include plaintext secrets.
- Do NOT break:
  - Existing host/key/password backup/restore behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/HostDatabase.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`
- Interfaces/contracts:
  - Host serialization and backup JSON contract.

### Acceptance criteria
- [ ] Behavior:
  - SSM host fields persist and round-trip via backup/restore.
- [ ] Tests (or explicit manual verification):
  - Migration smoke from pre-SSM DB state.
  - Backup export/import smoke including SSM host records.
- [ ] Docs:
  - Migration notes documented in ticket and release docs.
- [ ] Observability (if relevant):
  - Non-secret backup markers include SSM host counts.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Create SSM host, export encrypted backup, clear app state, import backup, verify host restored.
- Expected output(s):
  - SSM host metadata restored without regressions on non-SSM hosts.

### Risks / rollout
- Regression areas:
  - Schema upgrade and import compatibility across older backup payloads.
- Rollback plan:
  - Keep migration additive and maintain fallback defaults when SSM fields absent.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0264-ssm-host-editor-and-session-ux.md`
- Related tickets:
  - `TKT-0262`, `TKT-0263`
