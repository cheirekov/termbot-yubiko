# TKT-0238 — Theme token and component refresh

## Status: DONE
## Priority: MEDIUM
## Epic: TKT-0226

## Problem
UI still depends on legacy color/style patterns with limited design tokens, making consistency, accessibility tuning, and visual modernization difficult.

## Goal
Introduce a coherent tokenized visual system and refresh high-impact components without rewriting the entire app.

## Scope
- In scope:
  - Define core design tokens (color, spacing, type scales).
  - Refresh list rows, dialogs, bottom sheets, and action affordances.
  - Improve contrast defaults and state clarity.
- Out of scope:
  - Full Compose migration.
  - Backend/business logic changes.

## Acceptance Criteria
- [x] Token set documented and applied to target components.
- [x] Contrast and readability improvements validated on key screens.
- [x] Legacy hardcoded style usage reduced in prioritized layouts.

## Implementation Notes (2026-03-04)
- Added visual tokens in resources:
  - colors (`ui_color_*`) in `res/values/colors.xml`
  - spacing/type (`token_*`) in `res/values/dimens.xml`
- Applied tokenized styles to key components:
  - list row typography/contrast (`ListItemFirstLineText`, `ListItemSecondLineText`)
  - dialog text contrast (`AlertDialogTheme`)
  - key-management bottom sheet surface + icon/text tint (`dia_pubkey_add.xml`, `SecurityKeySheetActionItem`)
  - host list FAB action colors (`act_hostlist.xml`)
- Reduced hardcoded values in list row layouts by switching to dimen tokens in:
  - `item_host.xml`
  - `item_pubkey.xml`
- Token catalog document added:
  - `ai_docs/docs/THEME_TOKENS_2026-03-04.md`

## Verification
- Docker build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 35s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-04T13-39-44+02-00.log`
