# TKT-0232 — PIN input policy and keyboard consistency (OpenPGP/PIV/FIDO2)

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Problem
PIN input UX is inconsistent:
- OpenPGP currently uses numeric keypad.
- PIV/FIDO2 use full text password field.

This can confuse users and may block valid PIN formats depending on provider policy.

## Goal
Define provider-specific PIN policy and apply consistent, intentional input behavior:
- OpenPGP: allow full password charset (unless policy enforces numeric only).
- PIV: enforce provider-safe PIN format with clear validation message.
- FIDO2: keep full charset entry.

## Implemented Policy (2026-03-04)
- OpenPGP auth PIN:
  - Numeric keypad input.
  - Required.
  - Digits-only local validation.
  - Minimum length 6.
- PIV PIN:
  - Text password input (alphanumeric-capable).
  - Required.
  - Local length validation: 6-8 characters.
- FIDO2 PIN:
  - Text password input (full charset).
  - Optional (empty allowed for keys with no FIDO2 PIN).
  - If provided, local minimum length validation: 4.

All policy rejections log a non-secret flow marker only:
- `SK_ACTIVITY_PIN_LOCAL_FORMAT_REJECTED provider=<provider> reason=<reason>`

## Scope
- In scope:
  - Input type + hints + validation for PIN dialogs in `SecurityKeyActivity`.
  - User-facing helper text for each provider.
  - Debug log marker for rejected local PIN format (without logging PIN).
- Out of scope:
  - Server-side auth changes.
  - Key provisioning/reset flows.

## Acceptance Criteria
- [x] PIN dialogs match provider policy and are documented.
- [x] User cannot submit obviously invalid local format where policy is known.
- [x] No secret leakage in logs.
- [x] Docker `assembleDebug` succeeds.

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `repos/termbot-termbot/app/src/main/res/values/strings.xml`

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 42s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-04T11-51-57+02-00.log`
