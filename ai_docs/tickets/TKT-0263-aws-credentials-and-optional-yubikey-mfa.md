# Ticket: TKT-0263 — AWS credential management + optional YubiKey MFA

## Context capsule (must be complete)
### Goal
- Add safe credential handling for SSM sessions, with optional future hook points for YubiKey-backed MFA paths.

### Scope
- In scope:
  - Define supported credential modes for initial release (explicitly ranked).
  - Implement secure credential storage/retrieval for chosen initial mode(s).
  - Ensure credential redaction and non-secret logging markers.
  - Add extension points for optional MFA integration.
- Out of scope:
  - Full IAM Identity Center UX if not selected as initial mode.
  - Broad security-key UX redesign.

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
  - Selected credential mode(s) can authenticate SSM session start successfully.
- [ ] Tests (or explicit manual verification):
  - Manual success + failure credential-path checks on device.
- [ ] Docs:
  - Credential policy documented with explicit unsupported modes.
- [ ] Observability (if relevant):
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
