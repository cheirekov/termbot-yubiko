# TermBot

TermBot is an Android SSH client focused on hardware-backed authentication with YubiKey over NFC and USB.

It is based on ConnectBot and has been upgraded for modern Android while adding in-app diagnostics and stronger day-to-day UX for security-key workflows.

## Current Direction

- YubiKey-first authentication path (YubiKit SDK).
- Supported hardware key providers: OpenPGP, PIV, FIDO2 (SSH security keys).
- Supported transports: NFC and USB (including desktop-mode and USB host scenarios).

## Major Upgrades Implemented

- Replaced legacy hwsecurity runtime path with YubiKit-based flows.
- Added in-app debug ring buffer and `Export Debug Report` (shareable file, no adb required).
- Added verbose SSH auth tracing for hardware-key auth (offer, pk_ok, sign requested, auth end reason).
- Added key metadata in reports (`OFFERED_KEY_OPENSSH`, fingerprint, slot/provider details).
- Fixed OpenPGP PIN/auth flow and two-step auth instrumentation.
- Added PIV and FIDO2 signing support for SSH.
- Added saved password support (per host, optional).
- Added ProxyJump (jump host) support.
- Added encrypted backup export/import.
- Added host grouping with backup compatibility.
- Added in-app key generation support.
- Added Day/Night app theme and UI contrast fixes.
- Improved desktop-mode UX for key actions and keyboard behavior.

## Security Notes

- Debug logs are designed to avoid secrets.
- No PIN values.
- No private key material.
- No raw APDU payload dumps.

## Build (Docker-first)

Use the project Docker build helper from the workspace root:

```bash
ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local \
ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
```

Debug APK outputs:

- `app/build/outputs/apk/oss/debug/app-oss-debug.apk`
- `app/build/outputs/apk/google/debug/app-google-debug.apk`

## Project Status

- Active development with focus on reliability and usability for YubiKey SSH auth flows.
- hwsecurity repository/runtime is no longer required for current app direction.
