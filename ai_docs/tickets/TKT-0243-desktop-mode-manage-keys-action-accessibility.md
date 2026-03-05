# TKT-0243 — Desktop Mode Manage Keys action accessibility

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Context capsule (must be complete)
### Goal
- Fix Manage Keys usability in Android Desktop Mode where Add Key actions are not reliably reachable (user sees only private-key import path, cannot access OpenPGP/PIV/FIDO2 add flows).

### Scope
- In scope:
- Add a desktop-safe Add Key action picker in `PubkeyListActivity` that does not depend on bottom-sheet behavior.
- Keep full action parity: generate, import private key, add OpenPGP, add PIV, add FIDO2, and OpenPGP setup algorithm flow.
- Desktop input-device hardening for key auth prompts in `SecurityKeyActivity`:
- hardware keyboard Enter submits PIN dialog, Escape cancels dialog.
- show wait/touch prompt again after PIV/FIDO2 PIN entry so touch-required USB keys have explicit UI state.
- Keep phone UX intact (bottom sheet remains for narrow/mobile layouts).
- Out of scope:
- Full desktop UI redesign/layout overhaul.
- Host grouping redesign work.

### Constraints
- Keep diff minimal and reversible.
- Preserve existing security-key import/auth behavior.
- No secret logging changes.

### Target areas
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`

### Acceptance criteria
- [x] Behavior (implementation):
- In wide/desktop windows, tapping Add Key opens a regular dialog with all key-add actions.
- OpenPGP setup remains available via algorithm selection dialog.
- On narrow/mobile windows, existing bottom-sheet flow remains available.
- PIN dialogs support desktop keyboard submit/cancel behavior.
- PIV/FIDO2 auth keeps explicit wait/touch prompt after PIN submission.
- [x] Tests (manual):
- In Android Desktop Mode, Manage Keys -> Add Key shows all actions and each action launches corresponding flow.
- [x] Docs:
- Ticket + board + state + build status updated.

### Verification
- Docker build:
- `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Build log:
- `references/logs/android_build_2026-03-04T16-58-49+02-00.log`

## Notes
- User report date: 2026-03-04
- Symptom captured by user: Manage Keys in desktop mode did not expose full key-add options.
- User confirmation date: 2026-03-05 (desktop mode smoke passed; behavior works as expected).

## Delivered Artifacts
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `app/src/main/java/org/connectbot/transport/AuthAgentService.java`
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
