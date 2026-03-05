# Ticket: TKT-0241 — P0 jump-host auth state leak skips target publickey auth

## Context capsule (must be complete)
### Goal
- Fix a regression where target host auth via jump host (`ProxyJump` style) falls back to password because publickey auth is skipped after jump-host auth.

### Scope
- In scope:
- Preserve/restore SSH auth-loop state (`pubkeysExhausted`, `interactiveCanContinue`) across temporary jump-host authentication.
- Reset auth-loop state at start of `connect()` to avoid stale carry-over across reconnect attempts.
- Harden target-host auth entry conditions after jump-forward setup:
  - Log per-auth-cycle context (`SSH_AUTH_CONTEXT`) with methods + `pubkeyId`.
  - Attempt explicit host key auth when key is selected even if `publickey` method advertisement is inconsistent.
  - Include security-key fallback in `PUBKEYID_ANY` path (not only in-memory file keys).
- Out of scope:
- New auth methods or jump-host UX redesign.

### Constraints
- Must not break existing OpenPGP/PIV/FIDO2 direct auth or jump-host setup.
- No secrets in logs.
- Keep diff minimal.

### Target areas
- `app/src/main/java/org/connectbot/transport/SSH.java`

### Acceptance criteria
- [x] Behavior:
- For host `B1` using jump host `A1`, publickey auth is attempted on the final target host (no immediate password-only fallback caused by leaked auth flags or method-advertise edge cases).
- [x] Tests (manual):
- Reproduce `B1 -> A1 -> target` with OpenPGP key; verify target also prompts for key/PIN flow and can authenticate via security key.
- [x] Docs:
- Board/state/build status updated.

### Verification
- Docker build:
- `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Build log:
- `references/logs/android_build_2026-03-04T15-18-18+02-00.log`

## Notes
- Root cause: jump-host auth reused the same `SSH` instance and set `pubkeysExhausted=true`, then target-host auth ran with that stale value and skipped publickey attempts.
- Fix (pass 1):
- Save/restore `pubkeysExhausted` and `interactiveCanContinue` in `authenticateJumpHost(...)`.
- Reset both fields at start of `connect()`.
- Fix (pass 2 after device report):
- Add `SSH_AUTH_CONTEXT` markers in `authenticate(...)` to expose target auth methods/selected key in report.
- Allow explicit selected key auth attempt even when `isAuthMethodAvailable(..., "publickey")` is inconsistent after jump-forward setup.
- Force one publickey attempt for jump-host targets (`host.getJumpHostId() > 0`) when method advertisement is inconsistent.
- Extend `PUBKEYID_ANY` fallback to try security-key entries from `pubkeydb` when no in-memory file key succeeds.
- Device confirmation (2026-03-04): user confirmed jump-host path works ("ok now worked").
