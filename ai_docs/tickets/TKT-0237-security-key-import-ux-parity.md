# TKT-0237 — Security-key import UX parity and waiting-state redesign

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Problem
Security-key authentication now has a persistent waiting dialog, but import flows still rely on short toasts. This creates inconsistent and less reliable guidance during NFC/USB interactions.

## Goal
Unify import and auth UX for OpenPGP/PIV/FIDO2 with the same prominent waiting state model.

## Scope
- In scope:
  - Replace toast-based import prompts with persistent waiting UI.
  - Add provider-specific subtitles and iconography for import states.
  - Keep existing import logic unchanged.
  - Add non-secret flow markers for wait state transitions.
- Out of scope:
  - Cryptographic/provider backend changes.
  - Host grouping or backup redesign.

## Acceptance Criteria
- [x] OpenPGP/PIV/FIDO2 import all show persistent waiting UI.
- [x] User can cancel import wait state explicitly.
- [x] No secret leakage in new logs.
- [x] Docker `assembleDebug` succeeds.

## Implementation Notes (2026-03-04)
- `PubkeyListActivity` now uses a shared persistent wait dialog for import flows with provider-specific messaging:
  - OpenPGP: `pubkey_add_openpgp_tap_key`
  - PIV: `pubkey_add_piv_wait_for_key`
  - FIDO2: `pubkey_add_fido2_wait_for_key`
- Added explicit cancel handling from the wait dialog for each provider path:
  - stops NFC/USB discovery
  - clears pending FIDO2 PIN/application state
  - resets per-provider import active/handled flags
- Added non-secret import wait markers to in-app debug log:
  - `PUBKEY_IMPORT_WAIT_UI_SHOW`
  - `PUBKEY_IMPORT_WAIT_UI_HIDE`
  - `PUBKEY_IMPORT_WAIT_UI_CANCELLED`
- Added OpenPGP import lifecycle markers:
  - `PUBKEY_OPENPGP_IMPORT_START`
  - `PUBKEY_OPENPGP_IMPORT_DISCOVERED`
  - `PUBKEY_OPENPGP_IMPORT_SUCCESS`
  - `PUBKEY_OPENPGP_IMPORT_FAILED`

## Verification
- Docker build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 32s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-04T12-51-16+02-00.log`
