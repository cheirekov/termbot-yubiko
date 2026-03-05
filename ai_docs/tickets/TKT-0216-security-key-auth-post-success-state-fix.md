# Ticket: TKT-0216 — Fix post-success auth state mismatch in security-key flow

## Context capsule (must be complete)
### Goal
- Eliminate post-auth false disconnect/state-mismatch after successful security-key publickey auth.

### Scope
- In scope:
- Investigate `SUCCESS_PUBLICKEY_SECURITY_KEY` followed by `IllegalStateException(Cannot open session, connection is not authenticated.)`.
- Fix auth-loop/control-flow ordering so `finishConnection()` only runs on valid authenticated state.
- Add explicit trace marker for session-open success/failure.
- Out of scope:
- New auth methods or protocol extensions.

### Constraints
- Platform/runtime constraints:
- Must stay compatible with Trilead library behavior.
- Security/compliance constraints:
- Keep debug logs secret-safe (no PIN/private/APDU payload).
- Do NOT break:
- Working two-step publickey path (`offer -> pk_ok -> sign`) and existing debug export.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/transport/SSH.java`
- Interfaces/contracts:
- `authenticate()`, `authenticateWithTwoStepPublicKey(...)`, `finishConnection()`.

### Acceptance criteria
- [x] Behavior:
- On successful security-key auth, no immediate auth-loop disconnect/state mismatch appears.
- [x] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds.
- Manual verify with OpenPGP key across repeated connects/disconnects.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Report shows clean success path without contradictory auth-end reasons.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Connect using security key three times, export debug report, verify no post-success mismatch lines.
- Expected output(s):
- `SUCCESS_PUBLICKEY_SECURITY_KEY` followed by stable session open (or clean user disconnect), without `DISCONNECT_DURING_AUTH` mismatch.

### Risks / rollout
- Regression areas:
- Auth retry loop and fallback auth method ordering.
- Rollback plan:
- Revert auth-loop restructuring while retaining debug markers.

## Notes
- Links:
- Validation report: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260301-171237.txt`
- Related tickets:
- TKT-0213

## Implementation progress (2026-03-01)
- Added SSH session-open control-flow guards in `SSH.finishConnection()`:
- Defer session open when `connection.isAuthenticationComplete()` is not yet true.
- Finalize session open once auth loop confirms authenticated state.
- Added observability markers:
- `SSH_SESSION_OPEN_ATTEMPT`
- `SSH_SESSION_OPEN_DEFERRED`
- `SSH_SESSION_OPEN_SUCCESS`
- `SSH_SESSION_OPEN_FAILED`
- Build PASS:
- `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-01T17-32-43+02-00.log`

## Validation evidence (2026-03-01)
- Report: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260301-174558 (1).txt`
- Verified lines:
- `SSH_AUTH_END_REASON: reason=SUCCESS_PUBLICKEY_SECURITY_KEY`
- `SSH_SESSION_OPEN_DEFERRED: connection_not_authenticated`
- `SSH_SESSION_OPEN_ATTEMPT: authenticated=true`
- `SSH_SESSION_OPEN_SUCCESS`
- Not present anymore: `SSH_AUTH_END_REASON: reason=DISCONNECT_DURING_AUTH detail=IllegalStateException(Cannot open session, connection is not authenticated.)`
