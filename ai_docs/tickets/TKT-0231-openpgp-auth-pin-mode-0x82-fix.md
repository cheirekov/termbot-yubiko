# TKT-0231 — OpenPGP auth PIN mode fix (APDU 0x6982)

## Status: DONE
## Priority: HIGH
## Epic: TKT-0224

## Problem
OpenPGP auth still failed while PIV/FIDO2 worked. Debug report (`termbot-report-20260303-164434.txt`) showed:
- `PK_OK` reached
- `SIGN_REQUESTED`
- failure: `OpenPGP authentication failed: APDU error: 0x6982`

`0x6982` indicates security status not satisfied, i.e. auth command executed without the correct PIN-verified state.

## Root Cause
`YubiKitOpenPgpAuthenticatorBridge` verified OpenPGP user PIN with:
- `session.verifyUserPin(pin, false)` -> PW1 mode `0x81`

For `INTERNAL AUTHENTICATE` (AUTH key / slot 9E), YubiKey expects PW1 mode `0x82`.

## Fix
- Switched OpenPGP PIN verification to:
  - `session.verifyUserPin(pin, true)`
- Added inline code comment clarifying why `true` is required for AUTH flow.

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/YubiKitOpenPgpAuthenticatorBridge.java`

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 22s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-03T16-49-23+02-00.log`
