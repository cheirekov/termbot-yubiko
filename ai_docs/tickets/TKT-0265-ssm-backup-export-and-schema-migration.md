# Ticket: TKT-0265 — SSM backup/export compatibility + schema migration

## Context capsule (must be complete)
### Goal
- Make SSM host configuration durable across persistence and encrypted backup/restore while remaining backward-compatible.

### Scope
- In scope:
  - Verify whether current SSM fields require new host schema columns; if not, keep persistence on existing host columns and avoid unnecessary DB version churn.
  - Add any required migration only if current persisted SSM shape cannot round-trip safely.
  - Include SSM host metadata in encrypted backup export/import flows.
  - Include persisted SSM scoped secrets in encrypted backup export/import flows without logging secret values.
  - Ensure restore identity distinguishes SSM targets so multiple SSM hosts in the same region do not collapse into one entry.
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
- [x] Behavior:
  - SSM host fields persist and round-trip via backup/restore.
- [x] Tests (or explicit manual verification):
  - No DB migration path was introduced in this slice because the current SSM host shape already round-trips on existing host columns.
  - Backup export/import smoke including SSM host records was operator-confirmed on 2026-03-07.
- [x] Docs:
  - Migration notes documented in ticket and release docs.
- [x] Observability (if relevant):
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

## Slice progress
- 2026-03-07 — Backup/export compatibility implementation:
  - Kept DB schema unchanged: current SSM host shape already persists on existing host columns (`protocol`, `username`, `hostname`, `port`, `postlogin`, `rememberpassword`), so no `HostDatabase.DB_VERSION` bump was required in this slice.
  - Extended encrypted backup JSON with additive `saved_scoped_secrets` entries for persisted SSM scoped secrets.
  - Restored SSM scoped secrets on import only for SSM hosts with `remember_password=true`.
  - Hardened restore dedupe so SSM host matching includes `post_login` / target, preventing two SSM targets in the same region from collapsing into one host.
  - Added non-secret backup markers:
    - `ssm_hosts=<count>`
    - `scoped_secrets=<count>`
  - Docker verification:
    - `references/logs/android_build_2026-03-07T15-27-09+02-00.log`
  - Operator smoke confirmed on 2026-03-07:
    - encrypted backup export/import restored SSM host target and saved secret without re-entry
  - Residual validation tracked separately:
    - same access key/region with multiple different SSM targets will be included in a later backup/import matrix smoke
