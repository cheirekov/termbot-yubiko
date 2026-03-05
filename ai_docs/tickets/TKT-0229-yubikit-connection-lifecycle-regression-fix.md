# TKT-0229 — YubiKit connection lifecycle regression fix (auth + import)

## Status: DONE
## Priority: HIGH
## Epic: TKT-0224

## Problem
After external edits, hardware-key flows regressed:
- OpenPGP auth reached `PK_OK` but signing failed with:
  - `OpenPGP authentication failed: Call connect() first!`
  - `OpenPGP authentication failed: APDU error: 0x6d00`
- PIV/FIDO2 imports failed with `IllegalStateException`.

The shared pattern was using `YubiKeyDevice.requestConnection(...)` and carrying the callback
connection into later async code paths (PIN prompt, sign, import handlers), after the callback
scope had already ended.

## Root Cause
`requestConnection(...)` is callback-scoped. Connection objects are not safe to retain and use
later in asynchronous flows. Auth bridges and import handlers were operating on stale/closed
connections.

## Fix
- Refactored bridges to hold `YubiKeyDevice` and open fresh `SmartCardConnection` on demand:
  - `YubiKitOpenPgpAuthenticatorBridge`
  - `PivSecurityKeyAuthenticatorBridge`
  - `Fido2SecurityKeyAuthenticatorBridge`
- Reworked `SecurityKeyActivity` discovery path to dispatch `YubiKeyDevice` directly instead of
  passing callback connection objects.
- Updated `PubkeyListActivity` PIV/FIDO2/OpenPGP import flows to use
  `device.openConnection(SmartCardConnection.class)` inside the active operation.
- Hardened PIV parsing error conversion in `PivSupport.readPublicKey` to avoid raw
  `IllegalStateException` leakage in UI/debug logs.

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/YubiKitOpenPgpAuthenticatorBridge.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/PivSecurityKeyAuthenticatorBridge.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/Fido2SecurityKeyAuthenticatorBridge.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/PivSupport.java`

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 31s` (62 actionable tasks: 17 executed, 45 up-to-date)
  - Log: `references/logs/android_build_2026-03-03T16-14-29+02-00.log`

## Notes
- This ticket stabilizes auth/import reliability by fixing connection lifetime handling.
- UI polish and further modernization work remain tracked under existing epics.
