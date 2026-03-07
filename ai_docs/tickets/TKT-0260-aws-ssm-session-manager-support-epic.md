# Ticket: TKT-0260 — AWS SSM Session Manager support (EPIC)

Type: EPIC

## Context capsule (must be complete)
### Goal
- Add AWS Systems Manager (SSM) Session Manager as a first-class connection transport in TermBot, allowing users to start interactive shell sessions to EC2 instances (and on-premises managed nodes) without opening inbound SSH ports, using AWS IAM + optional YubiKey-backed credentials for authentication.

### Scope
- In scope:
  - **Phase 1 — Spike / feasibility:** Evaluate AWS SSM Session Manager plugin protocol, Android runtime constraints, and credential delivery options (AWS IAM Identity Center, temporary credentials, WebSocket tunnel).
  - **Phase 2 — Transport implementation:** Add an `SSM` transport class alongside existing SSH/Telnet/Local transports that opens an SSM StartSession WebSocket channel and bridges it to the existing terminal emulator.
  - **Phase 3 — Credential management:** Support AWS credential input (access key / session token / IAM Identity Center SSO), optional secure storage via `SavedPasswordStore` or new credential store, and optional YubiKey-backed FIDO2/PIV assertion for IAM auth (AWS `aws:mfa` or FIDO2 WebAuthn).
  - **Phase 4 — UX integration:** Add SSM as a connection type in host editor, display SSM-specific status/errors in session UI, and support SSM port-forwarding sessions.
  - **Phase 5 — Backup/export compatibility:** Ensure SSM host entries are included in encrypted backup/restore cycle.
- Out of scope:
  - Full AWS SDK bundling (prefer lightweight HTTP/WebSocket approach or minimal SDK subset).
  - AWS console/web UI integration.
  - Non-interactive SSM document execution (RunCommand).

### Constraints
- Platform/runtime constraints:
  - Android minSdkVersion 19; WebSocket libraries must be compatible.
  - APK size budget: avoid pulling full AWS SDK; prefer targeted HTTP + SigV4 signing.
  - Must work alongside existing SSH transport without regressions.
- Security/compliance constraints:
  - AWS credentials (access keys, session tokens) must never be logged.
  - Stored credentials must use the same encryption discipline as saved passwords/keys.
  - YubiKey FIDO2/PIV integration for MFA must follow existing security-key bridge patterns.
- Do NOT break:
  - Existing SSH/Telnet/Local transport functionality.
  - Security-key auth flows (OpenPGP/PIV/FIDO2).
  - Backup/restore compatibility for existing host/key data.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/` — new `SSM.java` transport
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/` — `HostEditorActivity` (connection type), `ConsoleActivity` (session rendering)
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/` — credential storage, host database schema extension
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/` — `HostBean` extension for SSM fields
  - New package: `repos/termbot-termbot/app/src/main/java/org/connectbot/aws/` — SigV4 signer, SSM API client, WebSocket session manager
- Interfaces/contracts:
  - `AbsTransport` contract (existing transport abstraction).
  - `TransportFactory` registration for new SSM transport.
  - Host database schema migration for SSM-specific fields (region, instance ID, profile, SSM document name).

### Acceptance criteria
- [ ] Behavior:
  - User can create a host entry with type "SSM", specifying AWS region + instance ID.
  - User can start an interactive shell session to a managed EC2 instance via SSM without SSH port exposure.
  - Session renders in existing terminal emulator with full I/O parity.
  - Optional: User can authenticate SSM session start with YubiKey FIDO2 MFA assertion.
- [ ] Tests (or explicit manual verification):
  - Manual smoke: create SSM host, connect, run commands, disconnect.
  - Manual smoke: SSM + YubiKey MFA flow (if Phase 3 FIDO2 integration is delivered).
  - Verify backup/restore includes SSM host entries.
- [ ] Docs:
  - Architecture decision record for SSM transport design choices.
  - User-facing connection type documentation or in-app help text.
- [ ] Observability (if relevant):
  - Non-secret SSM session lifecycle markers in debug log (session start, WebSocket open/close, errors).
  - Credential type used (IAM user / SSO / session token) logged as non-secret category marker.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Configure an SSM-managed EC2 instance, create host entry in app, connect and verify interactive shell.
- Expected output(s):
  - Build green with SSM transport compiled in.
  - Interactive SSM session functional on device.

### Risks / rollout
- Regression areas:
  - Transport factory changes could affect SSH/Telnet/Local transport resolution.
  - Host database schema migration must be backward-compatible.
  - APK size increase from AWS signing/WebSocket dependencies.
- Rollback plan:
  - SSM transport can be feature-flagged or compile-time excluded without affecting existing transports.
  - Schema migration is additive (new columns/tables); rollback = ignore new fields.

## Sub-tickets (to be created as work proceeds)
| Phase | Ticket | Title | Status |
|---|---|---|---|
| 1 | TKT-0261 | SSM transport feasibility spike (protocol, deps, credential flow) | Done (2026-03-06) |
| 2 | TKT-0262 | SSM WebSocket transport implementation | Done (2026-03-06) |
| 3 | TKT-0263 | AWS credential management + optional YubiKey MFA | In Progress (Slice A operator-smoked; Slice B build-verified; broader MFA/jump follow-up scope remains) |
| 3b | TKT-0266 | SSM assume-role baseline | Review (build-verified 2026-03-07; operator smoke pending) |
| 4 | TKT-0264 | SSM host editor UX + session UI integration | Done (2026-03-07) |
| 5 | TKT-0265 | SSM backup/export compatibility + schema migration | Done (2026-03-07) |

## Notes
- Links:
  - AWS SSM Session Manager plugin protocol: https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html
  - AWS SigV4 signing: https://docs.aws.amazon.com/general/latest/gr/sigv4_signing.html
  - Existing transport abstraction: `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/AbsTransport.java`
- Related tickets:
  - `TKT-0215` (ProxyJump — related multi-hop pattern)
  - `TKT-0252` (auth lifecycle hardening — patterns to follow for SSM credential lifecycle)
