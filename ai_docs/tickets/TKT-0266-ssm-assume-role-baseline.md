# Ticket: TKT-0266 — SSM assume-role baseline

## Context capsule (must be complete)
### Goal
- Let the app obtain temporary SSM session credentials automatically by calling AWS STS `AssumeRole` from base AWS credentials, so the operator does not need to pre-generate temporary credentials manually.

### Scope
- In scope:
  - Add one optional SSM-only per-host field for role ARN.
  - Persist the role ARN in host storage with backward-compatible schema migration.
  - Use base AWS credentials from the existing SSM credential flow, then call STS `AssumeRole` when a role ARN is configured.
  - Use assumed-role temporary credentials only for the live SSM session start, while continuing to persist only the base credentials.
  - Include the role ARN in encrypted backup export/import and SSM host restore matching.
  - Add non-secret markers for assume-role enabled/success/failure path.
- Out of scope:
  - Shared AWS account/profile manager screen.
  - Multi-hop role chains or account-jump graphs.
  - In-app MFA prompt UX for `AssumeRole`.
  - External ID / source identity / custom session policy fields.

### Constraints
- Platform/runtime constraints:
  - Must fit the current Android/Java stack and existing host editor structure.
- Security/compliance constraints:
  - Never log role credentials, session tokens, or role ARN-derived secrets.
- Do NOT break:
  - Existing SSH/Telnet/Local hosts.
  - Existing direct SSM host flow without a role ARN.
  - Saved-password and encrypted-backup behavior for current hosts.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/HostDatabase.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/HostEditorFragment.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/aws/`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`
- Interfaces/contracts:
  - SSM credential resolution contract (`SsmCredentialResolver`)
  - Host persistence / encrypted backup JSON contract

### Acceptance criteria
- [x] Behavior:
  - When an SSM host has a role ARN configured, the app uses base AWS credentials to call STS `AssumeRole` and starts the SSM session with the returned temporary credentials.
  - When no role ARN is configured, direct SSM behavior remains unchanged.
- [x] Tests (or explicit manual verification):
  - Manual smoke with valid base credentials + role ARN.
- [ ] Tests (or explicit manual verification):
  - Manual smoke with invalid role ARN or denied assume-role permission.
- [x] Docs:
  - Role baseline scope and unsupported fields are documented.
- [x] Observability (if relevant):
  - Non-secret markers show whether assume-role was configured and whether role credentials were used.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Configure an SSM host with base AWS access key/secret and a role ARN, then connect and verify the host opens via assumed-role credentials.
- Expected output(s):
  - App prompts only for base secret material and performs STS `AssumeRole` internally before SSM `StartSession`.

### Risks / rollout
- Regression areas:
  - DB migration and encrypted backup compatibility for hosts.
  - Persisting the wrong credential set (assumed credentials instead of base credentials).
- Rollback plan:
  - Make role ARN optional and ignore it if the assume-role path is disabled or reverted.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0263-aws-credentials-and-optional-yubikey-mfa.md`
  - `ai_docs/tickets/TKT-0265-ssm-backup-export-and-schema-migration.md`
- Related tickets:
  - `TKT-0260`, `TKT-0263`

## Progress
- 2026-03-07 — Implementation/build slice:
  - Added optional SSM-only `Assume role ARN` field in host editor and persisted it through `HostBean` + `HostDatabase` (`DB_VERSION=30`).
  - Added encrypted backup export/import support for `ssm_role_arn` and included role ARN in SSM host restore matching.
  - Added `StsApiClient` and SigV4 form-post signing support so the app can call STS `AssumeRole` directly from base AWS credentials.
  - Updated `SsmCredentialResolver` to separate persisted base credentials from runtime credentials, preventing assumed-role temporary credentials from being saved back to secure storage.
  - Updated `transport/SSM.java` to activate role assumption only when a role ARN is configured, and to emit non-secret `assume_role_configured` / `credential_enhanced` markers.
  - Docker verification:
    - `references/logs/android_build_2026-03-07T16-40-42+02-00.log` (`assembleDebug`, success).
- 2026-03-07 — Operator smoke / closeout:
  - Operator confirmed on-device assume-role smoke and explicitly approved marking the ticket done.
  - Residual note:
    - invalid or denied role ARN surfacing remains a later validation case, but no happy-path blocker remains for this baseline ticket.
