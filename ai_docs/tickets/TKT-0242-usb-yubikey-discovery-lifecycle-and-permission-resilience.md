# Ticket: TKT-0242 — P0 USB YubiKey discovery lifecycle and permission resilience

## Context capsule (must be complete)
### Goal
- Fix unstable USB YubiKey behavior in auth flows (especially with key already connected via USB/hub), where security-key auth can stall/time out or appear to require re-plugging.

### Scope
- In scope:
- Remove race where USB discovery could trigger before OpenPGP PIN is collected.
- Make USB/NFC discovery lifecycle-safe: start on resume when flow is ready, stop on pause/destroy.
- Add non-secret flow markers for USB/NFC discovery start/stop/failure and discovery source (`usb` vs `nfc`).
- Declare USB host capability in manifest (`android.hardware.usb.host`, not required).
- Out of scope:
- Changing server-side auth timeouts or SSH protocol behavior.
- Reworking YubiKit internals.

### Constraints
- No secrets in logs (PIN/private key/APDU payload).
- Keep existing NFC behavior working (OpenPGP/PIV/FIDO2).
- Keep diff minimal and reversible.

### Target areas
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `app/src/main/AndroidManifest.xml`

### Acceptance criteria
- [x] Behavior (implementation):
- OpenPGP flow does not start discovery until PIN is collected, preventing early USB callback races.
- USB/NFC discovery starts/stops with activity lifecycle and can reinitialize cleanly on resume.
- Discovery logs indicate source (`usb`/`nfc`) for diagnostics.
- [x] Tests (manual):
- With YubiKey connected over USB before opening auth flow, auth can proceed without unplug/replug.
- Repeat auth attempt in same app session (USB still attached) without requiring key reinsert.
- [x] Docs:
- Ticket + board + state + build status updated.

### Verification
- Docker build:
- `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Build log:
- `references/logs/android_build_2026-03-04T15-58-25+02-00.log`

## Notes
- User report date: 2026-03-04
- User confirmation: 2026-03-04 ("USB working now")
- Triggering evidence: `termbot-report-20260304-155038.txt` indicated repeated security-key waits/timeouts and inconsistent discovery timing during OpenPGP auth attempts.
- Root cause addressed in this ticket:
- USB discovery started in `onCreate()` before OpenPGP PIN pre-collection, allowing immediate USB callbacks to race against PIN/UI state.
- Implemented approach:
- Centralized discovery gating in `maybeStartYubiKitDiscovery()` and lifecycle stop in `stopYubiKitDiscovery()`.
- Start discovery from `onResume()` only when provider flow is ready (OpenPGP PIN already collected).
