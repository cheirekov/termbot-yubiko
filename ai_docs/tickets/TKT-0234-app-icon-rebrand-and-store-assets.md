# TKT-0234 — App icon rebrand and store asset package (YubiKey-focused)

## Status: DONE
## Priority: MEDIUM
## Epic: TKT-0226

## Problem
Branding no longer reflects the product direction (YubiKey-focused security-key support).

## Goal
Create and integrate a refreshed app icon + launcher assets, and prepare store graphics.

## Scope
- In scope:
  - 2–3 icon concepts aligned to current product direction.
  - Adaptive icon assets for Android (foreground/background/monochrome if applicable).
  - Branding checklist for Google Play assets (icon/screenshots/short description alignment).
  - License/attribution review for all visual assets.
- Out of scope:
  - Publishing to Play Store in this ticket.
  - Paid marketing creatives.

## Acceptance Criteria
- [x] New icon assets integrated and buildable.
- [x] Visual QA on common launchers complete.
- [x] Store asset draft package prepared for release ticket.

## Implementation Notes (2026-03-04)
- Rebranded launcher assets integrated:
  - Updated app icon wiring to `@mipmap/icon` + `@mipmap/icon_round` in `AndroidManifest.xml`.
  - Replaced legacy launcher PNGs in `mipmap-*dpi` and fallback `drawable-*dpi`.
  - Updated adaptive icon XML to use generated foreground + monochrome layers.
- Visual QA artifact added:
  - `ai_docs/docs/NEW/tkt-0234-store-assets/launcher-mask-preview.png` (circle/rounded/squircle mask simulation).
- Store draft package added:
  - `ai_docs/docs/NEW/tkt-0234-store-assets/play-icon-512.png`
  - `ai_docs/docs/NEW/tkt-0234-store-assets/play-feature-graphic-1024x500.png`
  - Plus concept set A/B/C.
- Reproducibility script:
  - `ai_docs/scripts/generate_tkt0234_icon_assets.py`

## Verification
- Docker build:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
