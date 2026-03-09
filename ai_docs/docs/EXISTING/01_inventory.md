# Repo Inventory

Date: 2026-03-06
Scope: `./repos/termbot-termbot` (configured in `ai_docs/config/repos.list`)

## Overview
- Languages: Java (primary app code), Groovy (Gradle build logic), XML (Android resources + manifest), C/C++ (native terminal bits under `app/src/main/cpp`)
- Build tooling: Gradle 6.7.1 + Android Gradle Plugin 4.2.2 (`app/build.gradle`, `settings.gradle`)
- Runtime: Android application (`minSdkVersion 19`, `targetSdkVersion 34`, `compileSdkVersion 34`)

## Components
| Component | Type | Path | Responsibility | Notes |
|---|---|---|---|---|
| Android app module | Mobile app | `repos/termbot-termbot/app` | SSH client UI, session management, key management, auth flows | Main shipping artifact (`app-oss-debug.apk`, `app-google-debug.apk`) |
| Security key bridges | Domain/auth integration | `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey` | Provider-specific signing/auth operations (OpenPGP/PIV/FIDO2) via YubiKit | Includes `YubiKitOpenPgpAuthenticatorBridge`, `PivSecurityKeyAuthenticatorBridge`, `Fido2SecurityKeyAuthenticatorBridge` |
| Security key service | Android Service | `repos/termbot-termbot/app/src/main/java/org/connectbot/service/SecurityKeyService.java` | Background lifecycle and binding surface for hardware-key operations | Declared in manifest |
| SSH transport layer | Networking core | `repos/termbot-termbot/app/src/main/java/org/connectbot/transport` | SSH/Telnet/local transports and transport factory | `SSH.java` is critical auth runtime |
| Persistence/util layer | Data + utility | `repos/termbot-termbot/app/src/main/java/org/connectbot/util` | Host DB, pubkey DB, encrypted backup, saved password, debug report export | Contains `HostDatabase`, `EncryptedBackupManager`, `SecurityKeyDebugReportExporter` |
| Translations module | Auxiliary Gradle module | `repos/termbot-termbot/translations` | Imports and sync workflow for localization assets | Included via `settings.gradle` |
| Automation metadata | CI/ops configs | `repos/termbot-termbot/.github/workflows`, `Jenkinsfile`, `.travis.yml` | Translation automation + legacy CI definitions | Multiple CI systems coexist |

## Entrypoints
- Launcher UI: `HostListActivity` (`app/src/main/AndroidManifest.xml`)
- Deep-link/console session UI: `ConsoleActivity` (`AndroidManifest.xml`)
- Security-key operation UI: `SecurityKeyActivity`, `PubkeyListActivity`
- Background services:
  - `org.connectbot.service.TerminalManager`
  - `org.connectbot.service.SecurityKeyService`
- Application bootstrap: `ConnectBotApplication`

## Config surfaces
- Build config:
  - `repos/termbot-termbot/settings.gradle`
  - `repos/termbot-termbot/build.gradle`
  - `repos/termbot-termbot/app/build.gradle`
- Runtime config:
  - `repos/termbot-termbot/app/src/main/AndroidManifest.xml`
  - `repos/termbot-termbot/app/src/main/res/values*/` (themes, strings, styling)
- Environment toggles in build:
  - `MAVEN_REPO_CACHE`
  - `GRADLE_BUILD_CACHE`
  - `TRANSLATIONS_ONLY`
  - `ANDROID_ADB_SERVER_ADDRESS` / `ANDROID_ADB_SERVER_PORT`
- Workspace-level Docker-first wrappers:
  - `ai_docs/scripts/android_docker_build.sh`

## External dependencies
- YubiKit SDK family (`com.yubico.yubikit:*:2.4.0`) for OpenPGP/PIV/FIDO2
- SSH library: `com.github.connectbot:sshlib:2.2.15`
- Crypto support: `org.connectbot:jbcrypt:1.0.2`, `org.conscrypt:conscrypt-android:2.5.1` (oss flavor)
- UI/support libraries: AndroidX + Material
- Optional Google flavor dependency: `com.google.android.gms:play-services-basement:17.5.0`
- Translation import external source: Launchpad Bazaar mirror (`translations-import.yml`)

## Unknowns
- Is `hwsecurity|./repos/hwsecurity|sdk` in `ai_docs/config/repos.list` intentionally retained for historical context, or should it be removed to match current runtime truth (YubiKit-only)?
- Should Jenkins/Travis remain maintained as active CI targets, or treated as historical-only while GitHub Actions becomes canonical?
