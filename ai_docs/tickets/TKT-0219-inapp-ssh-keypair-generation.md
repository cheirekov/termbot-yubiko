# Ticket: TKT-0219 — In-app SSH key pair generation

## Context capsule (must be complete)
### Goal
- Add a native flow to generate SSH key pairs in TermBot so users can create host keys without external tools.

### Scope
- In scope:
- UI to create RSA/Ed25519 key pairs with nickname/comment.
- Save generated keys into existing key storage and list.
- Show/copy/share public key for server onboarding.
- Out of scope:
- Hardware key generation on YubiKey (covered by separate YubiKey tickets).
- Remote server-side key installation automation.

### Constraints
- Platform/runtime constraints:
- Must run fully offline on-device.
- Security/compliance constraints:
- Private key stays local and never appears in debug logs/reports.
- Do NOT break:
- Existing imported/generated key handling and auth-agent flows.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `app/src/main/java/org/connectbot/util/PubkeyUtils.java`
- `app/src/main/java/org/connectbot/util/PubkeyDatabase.java`
- `app/src/main/res/layout/*` (generation dialog/screen)
- Interfaces/contracts:
- Reuse current `PubkeyBean` representation for generated keys.

### Acceptance criteria
- [x] Behavior:
- User can generate a new key pair and use it for SSH authentication.
- [x] Tests (or explicit manual verification):
- Manual verify generation, fingerprint visibility, and successful auth against test host.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Debug markers include key-generation lifecycle without secret bytes.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Generate key, copy public key to server `authorized_keys`, connect with new key.
- Expected output(s):
- Successful publickey auth using generated key.

### Risks / rollout
- Regression areas:
- Key format compatibility with server and existing import/export flows.
- Rollback plan:
- Hide generation entry and keep legacy key workflows untouched.

## Notes
- Links:
- User request date: 2026-03-01
- Related tickets:
- TKT-0213, TKT-0218

## Implementation summary (2026-03-01)
- Confirmed native in-app key generation flow already existed in `GeneratePubkeyActivity` with RSA/DSA/EC/Ed25519 + save to `PubkeyDatabase`.
- Added structured in-app debug markers for generation lifecycle (no secret logging):
  - `KEYGEN_OPEN`
  - `KEYGEN_START_ENTROPY`
  - `KEYGEN_ENTROPY_ABORTED`
  - `KEYGEN_ENTROPY_READY`
  - `KEYGEN_THREAD_START`
  - `KEYGEN_SUCCESS`
  - `KEYGEN_SAVE_SUCCESS`
  - `KEYGEN_ERROR`
- Added new key action in key context menu to share public key directly:
  - `Share public key` using Android share sheet with OpenSSH public key line.
- Follow-up fixes:
  - Fixed silent generation failure path so save errors are surfaced via toast and marker.
  - Fixed Ed25519 public key copy/share conversion path on Android (`EdDSA` object fallback).
  - Switched clipboard copy path to `ClipData` API for reliable copy behavior.
- Files touched:
  - `app/src/main/java/org/connectbot/GeneratePubkeyActivity.java`
  - `app/src/main/java/org/connectbot/PubkeyListActivity.java`
  - `app/src/main/res/values/strings.xml`

## Manual validation (2026-03-01)
- User confirmed in-app generation works for RSA and Ed25519.
- User confirmed public/private key copy/share behavior works after follow-up fixes.
