# Ticket: TKT-0214 — Secure saved password auth (optional, per host)

## Context capsule (must be complete)
### Goal
- Add optional password saving for password-auth hosts so reconnect does not require retyping each time.

### Scope
- In scope:
- Per-host toggle: `Remember password on this device`.
- Password autofill in SSH password auth flow when available.
- Explicit clear action (`Forget saved password`) in host edit UI.
- Out of scope:
- Plaintext password storage.
- Syncing passwords across devices.

### Constraints
- Platform/runtime constraints:
- Must keep existing behavior for hosts without saved password.
- Security/compliance constraints:
- Never store password in plaintext in app DB/logs/report exports.
- Use Android Keystore-backed encryption when available.
- Do NOT break:
- Publickey/security-key auth paths and existing host storage schema.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/transport/SSH.java`
- `app/src/main/java/org/connectbot/HostEditorFragment.java`
- `app/src/main/java/org/connectbot/EditHostActivity.java`
- `app/src/main/java/org/connectbot/util/HostDatabase.java`
- `app/src/main/java/org/connectbot/bean/HostBean.java`
- `app/src/main/java/org/connectbot/util/*` (new secure credential helper)
- Interfaces/contracts:
- Password prompt path in `PromptHelper`.
- Host persistence and DB migration.

### Acceptance criteria
- [x] Behavior:
- User can save password per host and connect without password prompt.
- [ ] Tests (or explicit manual verification):
- Manual verify: save, reconnect, forget, reconnect prompts again.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Debug report includes only marker `PASSWORD_SOURCE=saved|prompt`, never value.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Configure host with password auth, save password, reconnect twice, then forget and retry.
- Expected output(s):
- First reconnect: no password prompt.
- After forget: password prompt appears again.

### Risks / rollout
- Regression areas:
- Incorrect migration causing missing host settings.
- Keystore/key invalidation edge cases.
- Rollback plan:
- Disable saved-password reads and clear saved secrets table while keeping host data intact.

## Notes
- Links:
- N/A
- Related tickets:
- TKT-0213

## Implementation progress (2026-03-01)
- Added per-host remember flag in host model/database:
- `HostBean.rememberPassword`
- `HostDatabase.FIELD_HOST_REMEMBERPASSWORD`
- DB migration to version `27` with upgrade path + default false.
- Added host editor toggle and save behavior:
- UI switch: `Remember password on this device`.
- Host edit menu action: `Forget saved password`.
- Added secure password storage helper:
- New `SavedPasswordStore` with Android Keystore-backed AES/GCM on API 23+.
- Non-plaintext encrypted fallback for older APIs (app-private encrypted payload).
- Integrated password source tracing in SSH auth:
- `PASSWORD_SOURCE=saved`
- `PASSWORD_SOURCE=prompt`
- No password values are logged.
- Build PASS:
- `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-01T18-06-16+02-00.log`
- Manual device verification is still required for full ticket closeout.
