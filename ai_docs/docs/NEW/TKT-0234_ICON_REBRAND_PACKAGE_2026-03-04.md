# TKT-0234 Icon Rebrand Package (2026-03-04)

## Summary
- Product direction: YubiKey-focused secure SSH access.
- Selected launcher icon: Concept A (teal gradient card + white key + touch indicator).
- Integration target: Android launcher icon (`@mipmap/icon` + adaptive layers).

## Concepts (Draft)
- Concept A (selected): `ai_docs/docs/NEW/tkt-0234-store-assets/icon-concept-a-selected.png`
- Concept B: `ai_docs/docs/NEW/tkt-0234-store-assets/icon-concept-b.png`
- Concept C: `ai_docs/docs/NEW/tkt-0234-store-assets/icon-concept-c.png`

## Launcher QA Artifacts
- Mask preview (Circle/Rounded/Squircle):
  - `ai_docs/docs/NEW/tkt-0234-store-assets/launcher-mask-preview.png`
- Adaptive layers produced:
  - `app/src/main/res/drawable-nodpi/ic_launcher_foreground_adaptive.png` (432x432)
  - `app/src/main/res/drawable-nodpi/ic_launcher_monochrome_adaptive.png` (432x432)
  - `app/src/main/res/drawable/ic_launcher_background.xml` (gradient background)

## Store Draft Package
- Play icon draft (512x512):
  - `ai_docs/docs/NEW/tkt-0234-store-assets/play-icon-512.png`
- Feature graphic draft (1024x500):
  - `ai_docs/docs/NEW/tkt-0234-store-assets/play-feature-graphic-1024x500.png`

## Branding/Listing Alignment Checklist
- [x] Icon visual language reflects hardware-key direction.
- [x] Launcher icon and store icon share the same core motif.
- [x] Draft feature graphic uses matching palette and message.
- [ ] Final Play listing copy update in release ticket (short/full description).
- [ ] Final screenshot refresh in release ticket.

## License/Attribution Review
- All graphics in this package are generated in-repo by `ai_docs/scripts/generate_tkt0234_icon_assets.py`.
- No third-party logo packs or proprietary icon files are included.
- No additional attribution requirements introduced by this ticket.
