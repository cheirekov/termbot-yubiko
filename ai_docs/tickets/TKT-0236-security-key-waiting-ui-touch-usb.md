# TKT-0236 — Security-key waiting UI: prominent touch/USB guidance

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Problem
Security-key flows (OpenPGP/PIV/FIDO2) used short toasts for "touch key" guidance, which is easy to miss. Users requested a more visible waiting screen like the earlier hwsecurity UX (clear prompt with large visual cue for NFC/USB).

## Goal
Provide a persistent, prominent in-app waiting prompt for YubiKey touch/insert states during authentication.

## Scope
- In scope:
  - Replace provider wait toasts with a reusable modal wait dialog in `SecurityKeyActivity`.
  - Show a large security-key icon and explicit NFC/USB guidance text.
  - Wire dialog lifecycle into OpenPGP/PIV/FIDO2 auth flows (show on wait, dismiss on discovery/finish/cancel).
  - Add structured debug markers for wait UI show/hide/cancel events.
- Out of scope:
  - Full visual redesign pass (covered by TKT-0233/TKT-0226).
  - PIN keyboard normalization (covered by TKT-0232).

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `repos/termbot-termbot/app/src/main/res/layout/dia_security_key_wait_for_touch.xml`
- `repos/termbot-termbot/app/src/main/res/values/strings.xml`

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 34s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-03T18-04-42+02-00.log`
