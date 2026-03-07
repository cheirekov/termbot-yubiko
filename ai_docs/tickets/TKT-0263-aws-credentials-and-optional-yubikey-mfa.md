# Ticket: TKT-0263 — AWS credential management + MFA + role/jump readiness

## Context capsule (must be complete)
### Goal
- Add safe credential handling for SSM sessions, including MFA support planning (not limited to YubiKey) and role/jump-ready credential flow foundations.

### Scope
- In scope:
  - Define supported credential modes for initial release (explicitly ranked), starting with access key ID + secret access key.
  - Define MFA scope for SSM credentials (baseline + phased providers), including but not limited to YubiKey-based factors.
  - Implement secure credential storage/retrieval for chosen initial mode(s).
  - Ensure credential redaction and non-secret logging markers.
  - Add extension points for MFA integration and STS/assume-role follow-up.
- Out of scope:
  - Full IAM Identity Center UX if not selected as initial mode.
  - Broad security-key UX redesign.
  - Full cross-account role-chain/jump orchestration in first delivery (follow-up after baseline credentials + PM/BA UX prioritization).

### Constraints
- Platform/runtime constraints:
  - Must fit current Android/Java stack and not require host-installed toolchains.
- Security/compliance constraints:
  - No plaintext credential persistence in logs or debug exports.
- Do NOT break:
  - Existing saved password/key behavior for SSH.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/SavedPasswordStore.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/aws/` (credential provider classes)
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/SecurityKeyDebugLog.java`
- Interfaces/contracts:
  - Credential lookup contract used by SSM transport.

### Acceptance criteria
- [ ] Behavior:
  - Selected credential mode(s) can authenticate SSM session start successfully, and MFA decision path is explicitly documented.
- [ ] Tests (or explicit manual verification):
  - Manual success + failure credential-path checks on device.
- [x] Docs:
  - Credential policy documented with explicit unsupported modes.
- [x] Observability (if relevant):
  - Marker-level credential type only (no values).

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Attempt SSM connect with valid and invalid credentials; verify surfaced errors and redaction.
- Expected output(s):
  - Clear auth errors without secret leakage.

### Risks / rollout
- Regression areas:
  - Credential storage collisions with existing SSH saved-password behavior.
- Rollback plan:
  - Disable persisted credential path and keep runtime-only credentials.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0261-ssm-transport-feasibility-spike.md`
- Related tickets:
  - `TKT-0262`, `TKT-0264`
- User direction (2026-03-06):
  - Prioritize easy credential baseline first (access key + secret key already shipping via TKT-0262 path), then proceed with role-based and account-jump scenarios.
  - IAM Identity Center remains backlog.
  - Initial shipment may proceed without YubiKey MFA.
- Operator note (2026-03-07):
  - `TKT-0263` should cover MFA as a capability area (not only YubiKey support).
  - PM/BA + UX prioritization is required for role/account-id/jump field placement before UI-heavy implementation starts.
- PM/BA decision (2026-03-07):
  - Prioritize temporary session credentials on the current SSM host flow before any assume-role or multi-account jump UI.
  - Keep phase-1 MFA as externally issued session-token support; no in-app MFA prompt UX ships in this ticket.
  - Future role/account-jump UX will use a mixed model: direct session-scoped inputs stay per-host, but multi-account role/jump chains move to a shared AWS profile abstraction in follow-up work.

## Slice progress
- 2026-03-07 — Slice A (backend credential foundation, no new role/jump UI fields):
  - Added `org.connectbot.aws.SsmCredentialResolver` and wired `transport/SSM.java` to use a dedicated credential resolution contract instead of inline host/password handling.
  - Added scoped secure-storage slots in `SavedPasswordStore` for SSM secrets:
    - `SCOPE_SSM_SECRET_ACCESS_KEY`
    - `SCOPE_SSM_SESSION_TOKEN` (extension hook for session credentials/STS follow-up)
  - Kept backward compatibility by allowing one-way read fallback from legacy host password slot and migrating to scoped storage on successful session start.
  - Added credential observability markers without values:
    - `credential_mode` (`long_lived_key` / `session_token`)
    - `secret_source` (`saved_scoped` / `saved_legacy` / `prompt`)
    - `mfa_prompted` (boolean)
  - Added MFA/STS extension hooks in resolver (`SessionCredentialEnhancer`) without shipping MFA UX in this slice.
  - Updated host editor actions (`EditHostActivity`) so “forget saved password” and remember-password-off cleanup clear SSM scoped secrets too.
  - Docker verification:
    - `references/logs/android_build_2026-03-07T15-13-08+02-00.log` (`assembleDebug`, success).
  - Operator smoke:
    - valid SSM credentials: confirmed connect, `whoami`, `exit`
    - invalid secret: confirmed clear auth error with no visible secret leakage
    - remember-password OFF: confirmed reconnect prompts again
- 2026-03-07 — Slice B (temporary session credentials on current SSM host flow):
  - Resolved PM/BA sequence to ship baseline temporary session credentials before assume-role/account-jump UI work.
  - Added session-token prompt support to `SsmCredentialResolver` for temporary AWS access keys (`ASIA...`) when no saved session token exists.
  - Reused scoped secure storage so `remember password` now persists temporary session tokens alongside SSM secret access keys.
  - Added non-secret observability marker `session_token_source` (`saved_scoped` / `prompt` / `none`) while keeping credential values out of logs.
  - Docker verification:
    - `references/logs/android_build_2026-03-07T15-48-07+02-00.log` (`assembleDebug`, success).
  - Additional evidence:
    - debug report confirmed the new session-token prompt path was exercised (`credential_resolved mode=session_token ... session_token_source=prompt`).
  - Sequencing note:
    - app-driven assume-role work was extracted into `TKT-0266` once the operator confirmed that manually generated temporary credentials are not the preferred long-term workflow.
