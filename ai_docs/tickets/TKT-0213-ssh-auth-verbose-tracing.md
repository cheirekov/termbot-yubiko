# Ticket: TKT-0213 — SSH auth verbose tracing for OpenPGP hardware security key path

## Context capsule (must be complete)
### Goal
- Add in-app SSH authentication tracing similar to `ssh -vvv` so exported debug reports can prove where publickey auth fails (key offer, sign invocation, rejection/cancel/timeout/disconnect outcomes).

### Scope
- In scope:
- Structured SSH auth trace markers for key offer/result/reason codes.
- Public key identity tracing (key type + SHA256 fingerprint) for file keys and security-key keys.
- `SIGN_REQUESTED` and `SIGN_NOT_CALLED` tracing around hardware security key signing flow.
- Include last 300 log lines in exported report.
- Out of scope:
- Full packet-level SSH wire logging.
- Logging secrets/PIN/APDU payloads.

### Constraints
- Platform/runtime constraints:
- No `adb` access on target device; diagnostics must be exportable in-app.
- Security/compliance constraints:
- Never log PIN values, private key material, or raw APDU payloads.
- Do NOT break:
- Existing OpenPGP/NFC auth behavior and prior lifecycle fixes.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/transport/SSH.java`
- `app/src/main/java/org/connectbot/SecurityKeySignatureProxy.java`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugLog.java`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
- Interfaces/contracts:
- Trilead `Connection.authenticateWithPublicKey(...)` and `SignatureProxy.sign(...)` auth path.

### Acceptance criteria
- [x] Behavior:
- Auth attempts include structured key identity traces and auth end reason markers.
- [x] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds.
- Manual report verification confirms `KEY_OFFER_SENT` -> `PK_OK` -> `SIGN_REQUESTED` -> `KEY_ACCEPTED`.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Exported report includes last 300 trace lines including new SSH auth verbose markers.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Trigger an SSH hardware-key auth attempt, then export debug report from app menu.
- Expected output(s):
- Report contains `SSH_AUTH_KEY_OFFER`, `SSH_AUTH_PK_STATUS`, `SSH_AUTH_KEY_RESULT`, `SSH_AUTH_END_REASON`, and `SIGN_REQUESTED` / `SIGN_NOT_CALLED` when applicable.

### Validation evidence
- 2026-03-01 debug report: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260301-171237.txt`
- Key proof lines:
- `OFFERED_KEY_OPENSSH=ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGCszS6weisCiJFmRuFbLezBc09BPksVVqeTXMOVGatH`
- `SSH_AUTH_PK_STATUS ... KEY_OFFER_SENT`
- `SSH_AUTH_PK_STATUS ... PK_OK`
- `SIGN_REQUESTED: SHA-512`
- `SK_SIGN_END: SHA-512`
- `SSH_AUTH_PK_STATUS ... KEY_ACCEPTED`
- `SSH_AUTH_KEY_RESULT ... result=SUCCESS`

### Risks / rollout
- Regression areas:
- Additional trace logging volume/noise.
- Rollback plan:
- Revert TKT-0213 changes in SSH/proxy debug-trace helpers.

## Notes
- Links:
- `references/prompt_bundles/test_build_android_master.md`
- Related tickets:
- TKT-0211
