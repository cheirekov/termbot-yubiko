# Release

## Environments
- dev: local debug builds (`ossDebug`, `googleDebug`)
- staging: internal tester APK distribution (manual install)
- prod: Google Play track (planned)

## Deployment strategy
- staged rollout after internal RC validation:
  - RC build
  - Internal smoke matrix
  - Limited user cohort
  - Broader rollout

## Rollback
- procedure:
  - Revert to previous known-good APK release tag.
  - Disable newly introduced feature flags if available.
  - Re-run auth smoke matrix (OpenPGP/PIV/FIDO2).
- migration considerations:
  - Preserve DB schema compatibility for hosts/keys/backups.
  - Keep encrypted backup format backward-compatible.

## Release Readiness Checklist (2026-03 milestone)
- [x] OpenPGP auth stable on device (`PK_OK` -> `SIGN_REQUESTED` -> `KEY_ACCEPTED`)
- [x] PIV auth stable on device
- [x] FIDO2 auth stable on device
- [x] In-app debug export available from host list
- [x] Docker `assembleDebug` green with pinned image
- [x] Security-key sentinel smoke matrix defined (`ai_docs/docs/QUALITY_PLAN.md`)
- [x] Baseline sentinel dry run recorded (2026-03-05)
- [ ] UX consistency pass complete (PIN policy + touch/USB prompt redesign)
- [ ] Icon/store branding assets prepared
- [ ] Known-issues section reviewed and approved

## RC Candidate Plan
- Candidate id: `RC-2026-03-security-key-stabilization`
- Proposed scope:
  - Include completed security-key stabilization tickets through TKT-0231.
  - Exclude pending UI redesign and icon refresh tickets (TKT-0232/0233/0234).
- Entry criteria:
  - Sentinel smoke matrix pass (`SK-01..SK-08`) on at least one NFC+USB YubiKey setup.
  - No critical auth regressions in exported debug reports.
- Exit criteria:
  - Sign-off from PM + Android Eng + QA representative.

## Per-release smoke gate (required)
1) Execute security-key sentinel matrix in `ai_docs/docs/QUALITY_PLAN.md`.
2) If any case fails, export debug report immediately and attach it to the active ticket.
3) Do not advance RC status until all failed cases are rerun and pass.
