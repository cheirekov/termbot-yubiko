# Build Status (Short)

Keep this file short. Link to full logs in references/logs/.

## Latest build (TKT-0244 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: FIDO2 USB parity fix; FIDO2 auth bridge (`Fido2SecurityKeyAuthenticatorBridge`) and FIDO2 key import flow (`PubkeyListActivity`) now open CTAP2 over `FidoConnection` first with `SmartCardConnection` fallback, so USB and NFC both work for FIDO2
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T17-30-28+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0243 desktop input+touch hardening — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 31s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: Added Desktop Mode auth prompt hardening in `SecurityKeyActivity` (PIN dialog supports hardware keyboard Enter submit / Escape cancel) and explicit post-PIN wait/touch UI for PIV/FIDO2 flows to better handle touch-policy keys on USB
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T16-58-49+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0243 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: Desktop Mode Manage Keys fix; `PubkeyListActivity` now uses a desktop-safe add-key actions dialog (generate/import/OpenPGP/PIV/FIDO2/setup) for wide windows while keeping existing bottom-sheet flow for narrow/mobile layouts
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T16-13-07+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0242 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 25 executed, 37 up-to-date)
- Changes: P0 USB YubiKey reliability hardening; moved USB/NFC discovery to lifecycle-gated start/stop in `SecurityKeyActivity`, delayed OpenPGP discovery until PIN is collected (race fix for pre-attached USB key), added USB/NFC discovery flow markers including source (`usb`/`nfc`), and added non-required `android.hardware.usb.host` manifest feature declaration
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-58-25+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0225 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 24s (62 actionable tasks: 9 executed, 53 up-to-date)
- Changes: host grouping UX refinement; group headers are always visible (including empty/new groups), headers are expandable/collapsible, and host list now includes synthetic `Ungrouped` and `All` sections with per-group host counts
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-36-57+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0241 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 26s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: P0 jump-host auth hotfix hardening; in addition to restoring auth flags around jump auth, target `authenticate()` now logs `SSH_AUTH_CONTEXT`, forces a publickey attempt for jump-host targets when method advertise is inconsistent, and includes security-key fallback for `PUBKEYID_ANY` to avoid direct password fallback after jump-forward setup
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-18-18+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0225 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 38s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0225 host grouping/folders; added host-group table+host `groupid`, editor group assignment, host-list grouped headers + create/rename/delete group actions, and encrypted-backup groups export/import remap
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T14-35-43+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0240 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 36s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0240 UI contrast hotfix; switched app theme to AppCompat DayNight, added `values-night` terminal-friendly palette, fixed v11 list text color override, added popup menu background theming, and added Settings app theme selector (System/Light/Dark)
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T14-05-28+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0234 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 34s (62 actionable tasks: 27 executed, 35 up-to-date)
- Changes: TKT-0234 app icon rebrand; launcher assets replaced (legacy + adaptive + monochrome), manifest switched to `@mipmap/icon` / `@mipmap/icon_round`, and store draft package generated
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-49-59+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0238 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 35s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0238 theme token/component refresh; new color + spacing/type tokens, tokenized list row typography/spacing, dialog text contrast, bottom-sheet surface/icon/text styling, and host-list FAB tokenized action colors
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-39-44+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0239 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 36s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0239 backup UX hardening; backup password dialogs now keep open with inline validation, and export/import now run with duplicate-operation lock + visible non-cancelable progress dialog + non-secret backup operation flow markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-11-59+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0237 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0237 import UX parity; OpenPGP/PIV/FIDO2 import flows now use persistent cancelable waiting dialog in `PubkeyListActivity`, with non-secret wait-state markers and OpenPGP import lifecycle markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T12-51-16+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Note
- A default-image run (`ghcr.io/cirruslabs/android-sdk:34`) failed before the successful build because it attempted NDK auto-install into a read-only SDK path (`/opt/android-sdk-linux`).
- Failed log: `android_build_2026-03-04T12-50-54+02-00.log`

## Latest build (TKT-0232 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 42s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0232 PIN input policy consistency; local provider-aware PIN validation in `SecurityKeyActivity` (OpenPGP digits+min6, PIV 6-8 chars, FIDO2 optional+min4 when provided), with non-secret reject marker `SK_ACTIVITY_PIN_LOCAL_FORMAT_REJECTED`
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T11-51-57+02-00.log`
- Errors: 0 | Warnings: deprecation + existing lint warnings

## Previous build (TKT-0236 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 34s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0236 security-key waiting UI upgrade; replaced OpenPGP/PIV/FIDO2 wait toasts with persistent dialog (large key icon + NFC/USB guidance) and added wait UI debug markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T18-04-42+02-00.log`
- Errors: 0 | Warnings: deprecation + existing lint warnings

## Previous build (TKT-0231 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 22s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0231 OpenPGP auth fix for APDU 0x6982 by switching OpenPGP PIN verify mode to PW1 0x82 before INTERNAL AUTHENTICATE
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-49-23+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0230 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 37s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0230 OpenPGP prompt/fail-fast fix + Manage Keys link cleanup; OpenPGP PIN prompt text corrected, OpenPGP auth errors are non-retryable (no hang loop), `hwsecurity.dev` links replaced by Yubico URL/text
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-33-48+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0229 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 31s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0229 connection-lifecycle fix; auth/import now open fresh `SmartCardConnection` from `YubiKeyDevice` per operation (OpenPGP/PIV/FIDO2) instead of reusing callback-scoped connections
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-14-29+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0228 — 2026-03-02)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL (log: android_build_2026-03-02T21-40-14+02-00.log)
- Changes: OpenPGP NFC two-tap flow (pre-tap PIN); PIV/FIDO2 NFC race fix (removed new Thread() wrapping); `security_key_openpgp_tap_now` string added
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0222 — 2026-03-02)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 35s (62 tasks: 21 executed, 41 up-to-date)
- Changes: OpenPGP import stub replaced with `session.getPublicKey(KeyRef.AUT).toPublicKey()`; Ed25519 algorithm name normalization; `pubkey_add_openpgp_success` string resource added
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0227 — 2026-03-02)
- Date: 2026-03-02
- Repo: `./repos/termbot-termbot`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Result: PASS — `BUILD SUCCESSFUL in 47s` (62 tasks: 19 executed, 43 up-to-date)
- Notes: TKT-0227 complete. hwsecurity SDK fully replaced with YubiKit. Zero de.cotech references.
- Log file: `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T19-31-41+02-00.log`

## Previous builds
- 2026-03-02 TKT-0221: debug log PIN/APDU redaction — PASS (26s) — log: `android_build_2026-03-02T18-34-50+02-00.log`
- 2026-03-02 Phase D: appcompat/constraintlayout/material bumped — PASS (58s) — log: `android_build_2026-03-02T18-24-21+02-00.log`
- 2026-03-02 Phase C: compileSdk/targetSdk 29→34, aapt2 override, manifest exported fix — PASS (11s) — log: `android_build_2026-03-02T18-19-19+02-00.log`
- 2026-03-02 Phase B: AGP 4.0.1→4.2.2, Gradle 6.1.1→6.7.1, buildToolsVersion→34.0.0 — PASS (1m 41s) — log: `android_build_2026-03-02T17-46-00+02-00.log`
- 2026-03-02 Phase A: jcenter removal, duplicate google() removal — PASS — log: `android_build_2026-03-02T17-13-02+02-00.log`
