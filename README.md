# TermBot Workspace

This repository is the delivery workspace for **TermBot** and its AI-assisted engineering process.

## What This Is

- `repos/termbot-termbot/`: Android SSH client (TermBot), focused on YubiKey auth over NFC/USB.
- `ai_docs/`: ticket board, state tracking, runbooks, and scripts used to execute work in small verified slices.
- `references/`: build/test/export logs and supporting artifacts.

## What Has Been Upgraded So Far

- YubiKit-first key flows (OpenPGP, PIV, FIDO2).
- In-app debug report export (no adb required) with ring-buffer diagnostics.
- SSH auth verbose tracing for hardware key auth decisions.
- ProxyJump support.
- Optional saved passwords.
- Encrypted backup export/import.
- Host grouping/folders.
- Desktop mode and UI/UX hardening (including Day/Night theme fixes).

## AI Process (`ai_docs`)

This workspace follows a strict ticket-first process:

1. Work from a ticket in `ai_docs/tickets/`.
2. Implement a small diff in code/docs.
3. Build/test (Docker-first for Android).
4. Update delivery docs: `ai_docs/boards/DELIVERY_BOARD.md`, `ai_docs/docs/BUILD_STATUS.md` (if build/tests ran), and `ai_docs/STATE.md` (if current truth changed).
5. Run closeout gate: `ai_docs/scripts/closeout_check.sh <TICKET_ID>`.

Key process references:

- `ai_docs/docs/HARD_RULES.md`
- `ai_docs/docs/WORKFLOW_CONTRACT.md`
- `ai_docs/docs/HANDBOOK.md`

## Build (Docker-first)

From workspace root:

```bash
ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
```

APK outputs:

- `repos/termbot-termbot/app/build/outputs/apk/oss/debug/app-oss-debug.apk`
- `repos/termbot-termbot/app/build/outputs/apk/google/debug/app-google-debug.apk`

## GitHub CI

Root workflow is available at `.github/workflows/android-main-build.yml`.
It builds debug APKs on each push to `main` and uploads APK/log artifacts.
