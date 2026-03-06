# Ticket: TKT-0254 — Auth failure taxonomy and user surfacing

## Context capsule (must be complete)
### Goal
- Standardize how security-key authentication failures are classified and surfaced so users get clear, actionable errors instead of ambiguous retries/spinners.

### Scope
- In scope:
  - Define a small taxonomy of auth failure classes (device absent, touch timeout, PIN rejected, transport error, unsupported capability, unknown internal error).
  - Map failure classes to user-visible messages and debug markers.
  - Ensure fail-fast behavior for non-retryable errors.
- Out of scope:
  - Full localization rewrite.
  - Deep redesign of host/session UX.

### Constraints
- Platform/runtime constraints:
  - Must work across OpenPGP/PIV/FIDO2 provider flows.
- Security/compliance constraints:
  - Error text and markers must remain non-secret.
- Do NOT break:
  - Existing successful auth behavior and happy-path latency.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
  - Provider bridge classes under `app/src/main/java/org/connectbot/securitykey/`
  - Relevant string resources in `app/src/main/res/values/strings.xml`
- Interfaces/contracts:
  - Provider error -> app-level classification -> user-facing message mapping.

### Acceptance criteria
- [ ] Behavior:
  - Non-retryable failures stop promptly with clear user guidance.
  - Retryable failures present consistent instructions (re-tap/re-touch/reconnect).
- [ ] Tests (or explicit manual verification):
  - Manual failure-path checks for invalid PIN, disconnected key, and timeout scenario.
- [ ] Docs:
  - Taxonomy summary added to relevant docs/ticket notes.
- [ ] Observability (if relevant):
  - Each failure class has deterministic non-secret marker in debug output.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Intentionally trigger 2–3 failure modes and verify message clarity + exported report marker consistency.
- Expected output(s):
  - User-facing error is explicit and debug marker category matches failure class.

### Risks / rollout
- Regression areas:
  - Over-classification may hide provider-specific nuance if mapping is too coarse.
- Rollback plan:
  - Restore previous direct error handling path and keep only safe message improvements.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0203-fail-fast-auth-error-and-log-markers.md`
  - `ai_docs/tickets/TKT-0230-openpgp-pin-prompt-failfast-and-ui-cleanup.md`
- Related tickets:
  - `TKT-0252`, `TKT-0253`
