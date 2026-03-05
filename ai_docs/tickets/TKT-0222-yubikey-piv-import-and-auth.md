# Ticket: TKT-0222 — YubiKey PIV import and SSH auth (existing keys)

## Context capsule (must be complete)
### Goal
- Let users import an existing YubiKey PIV SSH key (already on token) and authenticate to SSH hosts similarly to the current OpenPGP hardware-key flow.

### Scope
- In scope:
- Import PIV public key identity from YubiKey (starting with `AUTHENTICATION` slot `9A`).
- Persist imported PIV key identity in pubkey list and allow host binding.
- Execute SSH publickey auth using YubiKey PIV signing only after server key acceptance (`offer -> pk_ok -> sign`).
- Structured tracing markers for PIV auth path and end reasons in debug report.
- Out of scope:
- PIV key generation/import-to-token management UI.
- Non-YubiKey smart card support.

### Constraints
- Platform/runtime constraints:
- Must keep current OpenPGP path functional.
- Security/compliance constraints:
- No PIN/private key/APDU payload logging.
- Do NOT break:
- Current working jump-host flow and saved-password behavior.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `app/src/main/java/org/connectbot/transport/SSH.java`
- `app/src/main/java/org/connectbot/securitykey/*`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
- Interfaces/contracts:
- Extend hardware-key provider abstraction so OpenPGP and PIV can coexist.

### Acceptance criteria
- [ ] Behavior:
- User can add/select a YubiKey PIV key and authenticate successfully against a host that has that public key.
- [ ] Tests (or explicit manual verification):
- Manual verification with physical YubiKey and server configured with matching PIV pubkey.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Report includes `OFFERED_KEY_OPENSSH`, fingerprint, `PIV_SLOT_USED`, and auth stage markers.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Import PIV key, assign to host, connect, tap key/enter PIN as prompted, export debug report.
- Expected output(s):
- `PK_OK` then `SIGN_REQUESTED` then key accepted/success markers.

### Risks / rollout
- Regression areas:
- Existing OpenPGP callback/service flow.
- Rollback plan:
- Keep PIV path behind provider-specific guard and default to existing OpenPGP behavior when unavailable.

## Notes
- Links:
- Story clarification from user: support already-stored PIV keys like current OpenPGP usage model.
- Related tickets:
- TKT-0220, TKT-0213, TKT-0216

## Implementation progress (2026-03-01)
- Added `SecurityKeyProviderProfile` metadata plumbing for security-key entries, without DB schema migration.
- New OpenPGP security-key entries now persist explicit provider/slot profile metadata (`openpgp`, `AUTH (9E)`).
- SSH auth tracing now emits:
  - `SECURITY_KEY_PROVIDER_USED`
  - `PIV_SLOT_USED` (when relevant)
- Export Debug Report now includes:
  - `SECURITY_KEY_PROVIDER_USED=...`
  - `PIV_SLOT_USED=...`
- Current status:
  - OpenPGP path unchanged and still active.
  - PIV/FIDO provider routing is now explicit in SSH path; full PIV signing flow implementation remains pending.

## Closeout note (2026-03-02)
- User confirmed: YubiKey PIV SSH auth is working end-to-end on device with current codebase.
- Acceptance criteria met via manual device verification.
- Ticket closed as Done.
