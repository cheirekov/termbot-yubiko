# TermBot Workspace

This repository is the delivery workspace for **TermBot**, an Android SSH client with YubiKey support and AWS SSM support.

## Shipped Capability Summary

### SSH and YubiKey

- Direct SSH hosts
- SSH `Jump via host` support
- YubiKey-backed SSH auth with OpenPGP, PIV, and FIDO2
- NFC and USB YubiKey transport support
- Optional saved passwords
- Host grouping/folders
- Encrypted backup export/import
- In-app debug report export
- Day/Night theme and UX hardening

### AWS SSM

- Direct SSM shell sessions
- SSM with long-lived or temporary AWS credentials
- SSM assume-role support through STS
- SSM MFA support through STS `GetSessionToken` and MFA-capable `AssumeRole`
- SSM local port forwarding to the managed node
- SSM local port forwarding to remote hosts behind the managed node
- SSH `Route via SSM host`
- Combined `Route via SSM host` + `Jump via host`
- Background/foreground session survival on the current validated builds

## Operator Docs

- `MANUAL.md`: primary usage guide for SSH, jump hosts, SSM shell, SSM MFA, roles, port forwarding, and SSH-over-SSM routing
- `ai_docs/docs/RELEASE.md`: current release-candidate scope and smoke expectations
- `repos/termbot-termbot/README.md`: app-focused technical summary

## What This Repository Contains

- `repos/termbot-termbot/`: the Android app source
- `ai_docs/`: tickets, board, state tracking, release/process docs, and closeout scripts
- `references/`: build logs, packaged APK copies, and exported report artifacts

## Upstream Origin

- Original public TermBot archive (hwsecurity-based): `https://github.com/hwsecurity-sdk/termbot`
- This workspace continues from that base and modernizes the app for current Android tooling while migrating runtime key flows to YubiKit.

## Android App Stack

- App type: Android SSH client (TermBot, ConnectBot-based) with YubiKey and AWS SSM support
- Android levels: `minSdkVersion 19`, `compileSdkVersion 34`, `targetSdkVersion 34`
- Build toolchain: Android Gradle Plugin `4.2.2`, Gradle Wrapper `6.7.1`, Java 11 for Gradle builds
- Security-key SDK: YubiKit `2.4.0` modules: `android`, `core`, `openpgp`, `piv`, `fido`
- SSH/auth libs: `com.github.connectbot:sshlib:2.2.15`, `org.connectbot:jbcrypt:1.0.2`
- Crypto/provider flavors: `org.conscrypt:conscrypt-android:2.5.1` (oss flavor), `com.google.android.gms:play-services-basement:17.5.0` (google flavor)
- UI libs: AndroidX `appcompat 1.6.1`, `material 1.6.1`, `constraintlayout 2.1.4`

## Build (Docker-first)

From workspace root:

```bash
ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local \
ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
```

APK outputs:

- `repos/termbot-termbot/app/build/outputs/apk/oss/debug/app-oss-debug.apk`
- `repos/termbot-termbot/app/build/outputs/apk/google/debug/app-google-debug.apk`

## Delivery Process

This workspace follows a ticket-first delivery model:

1. Work from a ticket in `ai_docs/tickets/`.
2. Keep the change small and verifiable.
3. Build/test with the Docker-first Android path when code changes.
4. Update `ai_docs/boards/DELIVERY_BOARD.md`, `ai_docs/docs/BUILD_STATUS.md` when builds ran, and `ai_docs/STATE.md` when truth changed.
5. Run `ai_docs/scripts/closeout_check.sh <TICKET_ID>`.

Key process references:

- `ai_docs/docs/HARD_RULES.md`
- `ai_docs/docs/WORKFLOW_CONTRACT.md`
- `ai_docs/docs/HANDBOOK.md`

## GitHub CI

Root workflow: `.github/workflows/android-main-build.yml`

It builds debug APKs on each push to `main` and uploads APK/log artifacts.
