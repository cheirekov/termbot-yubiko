# Ticket: TKT-0252 — Security-key lifecycle state machine hardening

## Context capsule (must be complete)
### Goal
- Reduce regressions caused by lifecycle races between `SecurityKeyActivity`, `SecurityKeyService`, and provider bridges during auth/import operations.

### Scope
- In scope:
  - Define and enforce explicit lifecycle/auth state invariants for security-key operations.
  - Add focused guardrails where stale or cross-session callbacks can affect active flow.
  - Improve non-secret debug markers around state transitions.
- Out of scope:
  - New provider features.
  - Broad refactor across unrelated UI layers.

### Constraints
- Platform/runtime constraints:
  - Android app lifecycle changes (rotate/background/foreground) must remain supported.
- Security/compliance constraints:
  - Never log PIN, private key material, or raw APDU payloads.
- Do NOT break:
  - Existing OpenPGP/PIV/FIDO2 auth/import flows on NFC and USB.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/service/SecurityKeyService.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/securitykey/*.java`
- Interfaces/contracts:
  - Auth callback sequencing between activity/service/provider bridge layers.

### Acceptance criteria
- [ ] Behavior:
  - No stale callback can complete a newer auth request.
  - Lifecycle transitions do not leave waiting UI in stuck state.
- [ ] Tests (or explicit manual verification):
  - Manual smoke for rotate/background/resume during security-key auth/import.
  - Sentinel matrix subset (OpenPGP + PIV + FIDO2 one pass each) remains green.
- [ ] Docs:
  - Ticket notes and board/build status updates completed.
- [ ] Observability (if relevant):
  - New/updated non-secret state markers documented.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Trigger each provider flow; rotate screen and briefly background app before key touch/tap; confirm either success or clear surfaced error.
- Expected output(s):
  - No indefinite spinner and no cross-flow state leakage.

### Risks / rollout
- Regression areas:
  - Callback timing and cancellation paths for NFC/USB.
- Rollback plan:
  - Revert lifecycle-guard patch set and restore previous callback gating.

## Notes
- Links:
  - `ai_docs/docs/EXISTING/04_risk_register.md`
- Related tickets:
  - `TKT-0253`, `TKT-0254`
