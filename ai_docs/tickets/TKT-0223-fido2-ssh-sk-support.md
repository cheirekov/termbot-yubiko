# Ticket: TKT-0223 — FIDO2 SSH-SK support (YubiKey resident/non-resident)

## Context capsule (must be complete)
### Goal
- Support SSH authentication using YubiKey FIDO2 keys (`sk-ecdsa-sha2-nistp256@openssh.com`, `sk-ssh-ed25519@openssh.com`) for hosts that already trust those keys.

### Scope
- In scope:
- Add/upgrade SSH transport support for OpenSSH security-key algorithms (`sk-*`).
- Implement two-step publickey auth tracing for FIDO2 path (`offer -> pk_ok -> sign`).
- Add user-visible import/enrollment flow for existing FIDO2 credentials required by SSH auth.
- Out of scope:
- WebAuthn browser integration.
- OTP management.

### Constraints
- Platform/runtime constraints:
- Must remain compatible with TermBot host/profile model.
- Security/compliance constraints:
- No PIN/UV secrets, no credential private material, no raw CTAP payloads in logs.
- Do NOT break:
- Existing OpenPGP and PIV auth flows.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/transport/SSH.java`
- SSH library dependency/integration layer (currently `com.github.connectbot:sshlib:2.2.15`)
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
- Interfaces/contracts:
- Hardware-key provider abstraction for FIDO2 signing callbacks.

### Acceptance criteria
- [x] Behavior:
- User can authenticate with a configured FIDO2 SSH key on supported server.
- [x] Tests (or explicit manual verification):
- Manual verification against server expecting `sk-*` public key.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Report clearly shows algorithm offered, `pk_ok` state, sign invocation, and end reason.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Attempt connection with FIDO2-bound host and export debug report.
- Expected output(s):
- `OFFERED_KEY_OPENSSH` shows `sk-*` key type and stage markers identify success/failure point.

### Risks / rollout
- Regression areas:
- SSH library compatibility and auth negotiation behavior.
- Rollback plan:
- Keep FIDO2 path guarded behind capability check and preserve current default behavior.

## Notes
- Links:
- Current report markers remain useful for library-native SK classes, but FIDO2 tracing now uses a manual two-step flow on top of current sshlib.
- Related tickets:
- TKT-0220, TKT-0213, TKT-0224

## Implementation progress (2026-03-01)
- Added FIDO2 key import action in Pubkey UI:
  - `Add YubiKey FIDO2 key` accepts pasted OpenSSH public key lines for:
    - `sk-ecdsa-sha2-nistp256@openssh.com`
    - `sk-ssh-ed25519@openssh.com`
- Added `SshSkPublicKey` model to parse/store/export `sk-*` public key blobs without secrets.
- Added OpenSSH public key copy/share support for imported FIDO2 entries.
- SSH auth path now:
  - parses FIDO2 key blobs from stored pubkeys,
  - runs manual two-step auth (`offer -> pk_ok -> sign`),
  - logs `SIGN_REQUESTED` and explicit end reason when signer is not yet wired (`PROVIDER_NOT_IMPLEMENTED` with `fido2_signer_missing`),
  - logs `SIGN_NOT_CALLED` when server rejects before signing.
- Current status:
  - Server-side acceptance stage can now be proven from in-app reports (`PK_OK` vs reject-before-sign).
  - Actual FIDO2 signing/CTAP execution remains pending (next slice).

## Completion update (2026-03-02)
- Implemented end-to-end FIDO2 signing path with YubiKit-backed assertions and strict no-secret logging.
- Fixed two protocol-level issues that caused server reject after `PK_OK`:
  - assertion/public-key match verification used incorrect signature encoding for local verification,
  - SSH `sk-*` signature packet was nested one level too deep; corrected to OpenSSH wire format:
    - `string key_type`
    - `string sigblob`
    - `byte flags`
    - `uint32 counter`
- Added/kept structured traces in exported report:
  - `OFFERED_KEY_OPENSSH`, `OFFERED_KEY_FP`, `SECURITY_KEY_PROVIDER_USED`
  - `SSH_AUTH_PK_STATUS` (`KEY_OFFER_SENT`, `PK_OK`, `KEY_ACCEPTED|KEY_REJECTED`)
  - `SK_SIGNATURE_META` (`sig_len`, `flags`, `counter`)
- Manual validation evidence:
  - Report: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260302-165725.txt`
  - Success markers present:
    - `SSH_AUTH_PK_STATUS ... PK_OK`
    - `SSH_AUTH_PK_STATUS ... KEY_ACCEPTED`
    - `SSH_AUTH_END_REASON: SUCCESS_PUBLICKEY_SECURITY_KEY detail=fido2`
