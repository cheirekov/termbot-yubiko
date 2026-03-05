# TKT-0230 — OpenPGP PIN prompt/fail-fast and Manage Keys UI cleanup

## Status: DONE
## Priority: HIGH
## Epic: TKT-0224

## Problem
- OpenPGP auth looked like a hang after PIN entry.
- Report showed OpenPGP signing error: `Invalid PIN/PUK. Remaining attempts: 2`.
- The OpenPGP PIN prompt reused PIV strings, increasing risk of entering wrong PIN.
- Manage Keys still contained `hwsecurity.dev` links/text.

## Fix
- `SecurityKeyActivity`:
  - OpenPGP prompt now uses dedicated OpenPGP PIN title/message.
  - Added OpenPGP bridge callbacks for dismiss/error handling.
  - OpenPGP auth errors now show clear message (`security_key_openpgp_invalid_pin`) and close flow.
- `SecurityKeySignatureProxy`:
  - Added non-retryable error handling for OpenPGP provider to avoid retry/wait loop ("hang") on permanent errors.
- `PubkeyListActivity`:
  - Replaced `hwsecurity.dev` URL/share text in Manage Keys "shop" with Yubico URL/text.
- `strings.xml`:
  - Added `security_key_openpgp_pin_prompt_title`
  - Added `security_key_openpgp_pin_prompt_message`
  - Added `security_key_openpgp_invalid_pin`

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeySignatureProxy.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/YubiKitOpenPgpAuthenticatorBridge.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `repos/termbot-termbot/app/src/main/res/values/strings.xml`

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 37s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-03T16-33-48+02-00.log`
