# Ticket: TKT-0202 — Lifecycle-safe service/authenticator delivery

## Context capsule (must be complete)
### Goal
- Remove crash/race-prone handoff between `SecurityKeyActivity` and `SecurityKeyService` so authenticator/cancel delivery is lifecycle-safe.

### Scope
- In scope:
- Guard against null or not-yet-bound service during dialog callbacks.
- Queue pending authenticator/cancel actions until service bind completes.
- Add safe null checks in service relay methods.
- Out of scope:
- Large lifecycle refactor or service model redesign.

### Constraints
- Platform/runtime constraints:
- Must work with existing Activity/Service architecture.
- Security/compliance constraints:
- No logging of sensitive auth data.
- Do NOT break:
- Existing security-key flow when service binding is fast and successful.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `app/src/main/java/org/connectbot/service/SecurityKeyService.java`
- Interfaces/contracts:
- `SecurityKeyService#setAuthenticator(...)` and `cancel()`.

### Acceptance criteria
- [ ] Behavior:
- Dialog callbacks no longer assume service is immediately available.
- [ ] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds.
- [ ] Docs:
- Ticket + board + build status updated.
- [ ] Observability (if relevant):
- Race-drop scenarios log safe markers.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Trigger OpenPGP auth and cancel/discover quickly after dialog appears; app should fail cleanly without crash/hang.
- Expected output(s):
- No NPE in service relay path; auth either proceeds or fails fast safely.

### Risks / rollout
- Regression areas:
- Delayed service bind could still cause early cancellation path.
- Rollback plan:
- Revert queued-delivery/null-guard additions in activity/service.

## Notes
- Links:
- `references/prompt_bundles/test_build_android_master.md`
- Related tickets:
- TKT-0201, TKT-0203
