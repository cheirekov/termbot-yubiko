# UI Variants — 2026-03-04

## Variant A — Terminal Ops

### Direction
- Operational/technical look.
- Dense but clear information surfaces for power users.

### Design Tokens
- Primary: `#23403A`
- Accent: `#5DBB63`
- Warn: `#F2A93B`
- Typography:
  - Heading: `IBM Plex Sans Semibold`
  - Body/metadata: `IBM Plex Sans`
  - Terminal accents: `JetBrains Mono`

### Component Notes
- Host list:
  - Compact cards with status chip (`Connected`, `Disconnected`, `Auth pending`).
  - Optional group header row (future TKT-0225).
- Add Key sheet:
  - Split into `Software Keys` and `YubiKey`.
  - Add one-line helper text under each key type.

### Security-Key Waiting State
- Large centered key glyph.
- Two-path instruction row:
  - `Tap via NFC`
  - `Insert via USB`
- Optional retry/cancel actions at bottom.

## Variant B — Credential Ledger

### Direction
- Calm, form-centric, lower visual noise.
- Better for mixed experience users.

### Design Tokens
- Primary: `#1F3552`
- Accent: `#3A7CA5`
- Surface: `#F6F8FB`
- Typography:
  - Heading: `Source Sans 3 Semibold`
  - Body: `Source Sans 3`

### Component Notes
- Host list:
  - Larger tap targets and row spacing.
  - Metadata badges for key type (`OpenPGP`, `PIV`, `FIDO2`).
- Backup flow:
  - Inline validation text in dialogs (not toast-only).
  - Explicit progress state when import/export runs.

### Security-Key Waiting State
- Card with illustration + stepper:
  - `1. Hold key near phone`
  - `2. Enter PIN when prompted`
  - `3. Touch key`
- Supports OpenPGP/PIV/FIDO2 subtitle per context.

## Variant C — Field Toolkit

### Direction
- High-contrast, glanceable, rugged mobile-first UI.
- Optimized for fast use in noisy/bright environments.

### Design Tokens
- Primary: `#15202B`
- Accent: `#2AA876`
- Alert: `#E76F51`
- Typography:
  - Heading: `Barlow Condensed Semibold`
  - Body: `Barlow`

### Component Notes
- Host list:
  - Bold hostname + compact metadata strip.
  - Sticky filter/search bar.
- Add/Edit host:
  - Chunked sections with explicit labels and dividers.

### Security-Key Waiting State
- Full-width bottom sheet instead of small dialog.
- Big NFC + USB pictograms with animated pulse.
- Primary CTA: `I touched/inserted the key`, secondary `Cancel`.

## Recommendation
1. Start implementation with Variant B baseline (safest migration path).
2. Borrow Variant A operational density for terminal/advanced screens.
3. Borrow Variant C waiting-state affordances for security-key flows.

## Rollout Strategy
1. Phase 1: security-key import/auth states + dialog patterns.
2. Phase 2: Host List and Add/Edit Host structure refresh.
3. Phase 3: global token and icon refresh.
