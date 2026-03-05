# Android Build & Test — Docker-first (Clean Host)

Date: 2026-02-27

Goal: build and test Android projects without installing Android SDK/Gradle on the host.

## Requirements on host
- Docker Engine (or Docker Desktop)
- adb for device install/logcat (optional but recommended)
  - If you want adb also containerized, see “adb in container” section.

## How it works
- Uses repo's `./gradlew` (Gradle Wrapper)
- Runs inside an Android SDK container image
- Mounts:
  - repo to `/work`
  - Gradle cache to `~/.gradle` (host dir) for speed
  - Android SDK cache to `~/.android` (host dir) for speed

## Default Docker image
We default to a Cirrus Labs Android SDK image (widely used in CI):
- `ghcr.io/cirruslabs/android-sdk:34`

If it fails in your environment, set:
- `ANDROID_DOCKER_IMAGE=<your_image>`

## Build debug APK (recommended first)
From workspace root:
```bash
ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
```

APK is usually at:
- `app/build/outputs/apk/debug/app-debug.apk`

## Run unit tests (if present)
```bash
ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot testDebugUnitTest
```

## Run instrumentation tests (optional, needs emulator)
This pack does NOT auto-start emulators by default (heavy). We treat this as later hardening.

## adb in container (optional)
If you really want zero host tools, you can use an adb container, but USB device forwarding is platform-specific.
Recommended practical approach:
- keep host adb (small) and keep Android SDK in Docker only.

## Logging policy (token efficient)
- Save full build logs to `references/logs/`.
- Only paste a short summary into chat when needed.
