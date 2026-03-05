# Existing Project Intake — Termbot (Android SSH client) + hwsecurity SDK

## Business context
- System: Android SSH client (termbot / ConnectBot fork) using **hardware-backed SSH authentication** with **YubiKey OpenPGP (GPG)**.
- Why: I need secure SSH access from Android with YubiKey-backed keys.
- Current state: termbot repo is old/archived and fails on my new Android device.

## Users / Personas
- Primary user: DevOps/Sysadmin using Android SSH and requiring YubiKey OpenPGP auth.
- Security expectation: PIN entry + hardware signing works reliably; no secrets leak.

## Primary goal (what we want)
- Make **OpenPGP/GPG SSH authentication using YubiKey 5 NFC** work on:
  - Device: **Honor Magic V3**
  - OS: **MagicOS 10 / Android 16**
- Symptom:
  - App prompts for YubiKey PIN
  - After PIN entry, spinner/loop persists and auth never completes (hang)

## Out of scope (for now)
- FIDO2 OpenSSH `sk-ssh-*` keys (future feature).
- Major rewrite/refactor; keep changes minimal.

## Runtime / Environment
- Runtime: Android app on device.
- Hardware key: **YubiKey 5 NFC**
- Transport: **NFC** (USB unknown; confirm later).
- Host environment: Linux workstation.
- Preference: **Docker-first builds/tests** (clean host; minimal local installs).
- Testing method: agent runs builds in Docker; user installs APK + provides logcat if needed.

## Constraints / Non-negotiables
- Minimal invasive fix (small diff, limited files).
- Must not break normal SSH auth (non-hardware key).
- No secrets in logs (PIN, private material).
- No infinite spinner: must timeout and show clear error (even if still failing).
- Process must be hard-ruled: docs/board updated + closeout gate runs.

## Success criteria (today / test build)
- Build a debug APK that installs and runs on Android 16.
- Hardware key auth path:
  - no infinite spinner
  - success OR clear, surfaced error
- Capture evidence:
  - build log saved to references/logs/
  - logcat command provided (safe filters)

## Success criteria (next iteration)
- Root cause proven with code path + logs.
- Fix verified repeatedly on device.
- Add bounded timeouts + cancellation cleanup.
- Add a small manual regression matrix checklist (OpenPGP path).