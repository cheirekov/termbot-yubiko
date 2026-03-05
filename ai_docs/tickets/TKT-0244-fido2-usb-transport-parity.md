# TKT-0244 — FIDO2 USB transport parity (import + auth)

## Status: DONE
## Priority: HIGH
## Epic: TKT-0220

## Context capsule (must be complete)
### Goal
- Fix FIDO2 behavior where NFC works but USB fails for both key import and SSH auth/signing.

### Scope
- In scope:
- Update FIDO2 auth bridge to prefer USB-native FIDO transport (`FidoConnection`) and fallback to smartcard transport.
- Update FIDO2 import flow in `PubkeyListActivity` to use same transport strategy (FIDO first, smartcard fallback).
- Keep existing NFC behavior working.
- Out of scope:
- OpenPGP/PIV transport changes.
- New FIDO2 UI redesign.

### Constraints
- No secrets in logs.
- Keep diff minimal and reversible.
- Maintain existing FIDO2 key format/signature behavior.

### Target areas
- `app/src/main/java/org/connectbot/securitykey/Fido2SecurityKeyAuthenticatorBridge.java`
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`

### Acceptance criteria
- [x] Behavior (implementation):
- FIDO2 auth on USB no longer depends only on smartcard transport.
- FIDO2 import on USB no longer depends only on smartcard transport.
- NFC still works via fallback path.
- [x] Tests (manual):
- Import FIDO2 key via USB succeeds.
- SSH auth with imported FIDO2 key via USB succeeds (with touch/PIN as required by key policy).
- [x] Docs:
- Ticket + board + state + build status updated.

### Verification
- Docker build:
- `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Build log:
- `references/logs/android_build_2026-03-04T17-30-28+02-00.log`

## Notes
- User report date: 2026-03-04
- Evidence: `termbot-report-20260304-172400.txt` shows `SK_ACTIVITY_FIDO2_DISCOVERED: source=usb` followed by `SK_SIGN_RETRY: IOException(FIDO2 application is not available on this YubiKey)` while NFC path succeeds.
- User confirmation date: 2026-03-05 (USB import/auth smoke passed and working).

## Delivered Artifacts
- `app/src/main/java/org/connectbot/securitykey/Fido2SecurityKeyAuthenticatorBridge.java`
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`
