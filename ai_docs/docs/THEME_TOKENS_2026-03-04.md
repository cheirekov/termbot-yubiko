# Theme Tokens (TKT-0238)

Date: 2026-03-04

## Goal
Introduce reusable visual tokens and apply them to high-impact UI components without changing behavior.

## Color Tokens
- `ui_color_brand_primary`: primary brand/action color
- `ui_color_brand_primary_dark`: status bar/darker brand shade
- `ui_color_brand_accent`: interaction accent/ripple
- `ui_color_surface`: default app surface
- `ui_color_surface_elevated`: elevated panel/bottom-sheet surface
- `ui_color_text_primary`: high-emphasis text on light surfaces
- `ui_color_text_secondary`: supporting text on light surfaces
- `ui_color_icon_on_surface`: icon tint for light surfaces
- `ui_color_action_primary`: floating action button fill
- `ui_color_action_ripple`: floating action button ripple

## Spacing + Type Tokens
- `token_space_xs`: 8dp
- `token_space_sm`: 12dp
- `token_space_md`: 16dp
- `token_space_lg`: 20dp
- `token_type_title`: 16sp
- `token_type_body`: 14sp

## Applied Components (This Slice)
- List rows:
  - host row (`item_host.xml`)
  - pubkey row (`item_pubkey.xml`)
  - list text color styles updated via `ListItemFirstLineText` and `ListItemSecondLineText`
- Dialogs:
  - `AlertDialogTheme` now uses primary/secondary text tokens
- Bottom sheet:
  - security-key add sheet (`dia_pubkey_add.xml`) moved from full brand fill to elevated surface token
  - icon + spinner tint aligned with on-surface tokens
- Action affordance:
  - host list FAB (`act_hostlist.xml`) uses action primary/ripple tokens

## Notes
- No cryptography, auth flow, storage, or backup logic changes.
- Existing resource names (`primary`, `accent`, etc.) remain, now mapped to token names.
