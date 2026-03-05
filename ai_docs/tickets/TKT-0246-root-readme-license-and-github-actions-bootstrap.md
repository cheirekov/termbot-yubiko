# TKT-0246 - Root README/LICENSE and GitHub Actions bootstrap

## Status: DONE
## Priority: HIGH
## Epic: TKT-0245

## Context capsule (must be complete)
### Goal
- Prepare the workspace root for first public GitHub publication with clear project/process docs and a default Android build workflow.

### Scope
- In scope:
- Add root-level `README.md` describing the app, repo layout, and `ai_docs` AI process.
- Add root-level `LICENSE.md` with component licensing boundaries.
- Add root-level GitHub Actions workflow to build debug APKs on `main` pushes.
- Out of scope:
- Store publishing/signing automation.
- Release tagging/versioning policy changes.

### Constraints
- Keep README concise and public-facing.
- Keep licensing text explicit about component boundaries.
- CI should be Docker-first and match local build practice.

### Target areas
- `README.md`
- `LICENSE.md`
- `.github/workflows/android-main-build.yml`

### Acceptance criteria
- [x] Behavior (implementation):
- Root `README.md` exists and documents app purpose + `ai_docs` process.
- Root `LICENSE.md` exists with explicit component licensing references.
- GitHub workflow builds `assembleDebug` on push to `main` and uploads APK artifacts.
- [x] Tests (manual):
- User validates README wording and repo-publication intent.
- [x] Docs:
- Ticket + board + state updated.

### Verification
- Not executed locally (workflow definition only).
- User confirmation (2026-03-05):
  - Public repo is live with current root README/LICENSE.
  - GitHub Actions is green on current code.

## Delivered Artifacts
- `README.md`
- `LICENSE.md`
- `.github/workflows/android-main-build.yml`
