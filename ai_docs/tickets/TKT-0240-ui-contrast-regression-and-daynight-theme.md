# TKT-0240 — UI contrast regression hotfix + Day/Night app theme

## Status: DONE
## Priority: HIGH
## Epic: TKT-0226

## Problem
After TKT-0238 token refresh, host list and overflow menu text became unreadable (white-on-light surfaces) on MagicOS devices, making the app unusable.

## Goal
Restore safe text contrast across key screens and add explicit app theme support (System/Light/Dark) with terminal-friendly dark palette.

## Scope
- In scope:
  - Fix list/menu/dialog text contrast regressions.
  - Introduce DayNight-compatible app theme tokens/resources.
  - Add Settings toggle: System / Light / Dark.
  - Apply theme preference at app startup and immediately when changed in Settings.
- Out of scope:
  - Full visual redesign.
  - Terminal emulator color profile redesign.

## Acceptance Criteria
- [x] Host list, menu popup, and key import dialogs are readable in light mode.
- [x] Dark mode renders with high-contrast text/surfaces.
- [x] User can select app theme in Settings (System/Light/Dark).
- [x] Docker `assembleDebug` succeeds.

## Implementation Notes (2026-03-04)
- Theme/contrast fixes:
  - `AppTheme` switched to `Theme.AppCompat.DayNight`.
  - Explicit text/background tokens wired in `styles.xml`.
  - Overflow popup background themed via `AppPopupMenu` / `AppPopupMenuOverflow`.
  - `values-v11/styles.xml` list item text colors changed from framework attrs to token colors.
  - Empty-state and spinner text colors updated to avoid white-on-light rendering.
- Day/Night support:
  - Added `values-night/colors.xml` (terminal-friendly dark palette).
  - Added preference constants and `AppThemeManager`.
  - `ConnectBotApplication` now applies selected mode on startup.
  - `SettingsFragment` listens for theme preference changes and recreates activity.
  - Added Settings ListPreference and arrays/strings for `app_theme`.

## Verification
- Build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 36s`
- Log:
  - `references/logs/android_build_2026-03-04T14-05-28+02-00.log`
