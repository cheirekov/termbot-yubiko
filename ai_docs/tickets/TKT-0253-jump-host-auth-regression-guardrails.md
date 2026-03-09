# Ticket: TKT-0253 — Jump-host auth regression guardrails

## Context capsule (must be complete)
### Goal
- Prevent reintroduction of jump-host auth state leaks that skip or mis-handle target publickey authentication.

### Scope
- In scope:
  - Strengthen jump-host -> target auth-state transitions in SSH auth path.
  - Add explicit trace markers for jump and target phases without sensitive payloads.
  - Add focused regression verification steps for ProxyJump with security-key auth.
- Out of scope:
  - New proxy protocols or host routing models.

### Constraints
- Platform/runtime constraints:
  - Must continue to support direct-host flows without jump hosts.
- Security/compliance constraints:
  - No secret leakage in auth markers.
- Do NOT break:
  - Existing successful jump-host login behavior fixed in TKT-0241.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSH.java`
  - Related auth helper classes referenced by SSH auth flow.
- Interfaces/contracts:
  - Jump-host to target auth phase handoff.

### Acceptance criteria
- [ ] Behavior:
  - Jump-host sessions consistently attempt target publickey auth when configured.
  - No unexpected fallback to password-only when security key is selected.
- [ ] Tests (or explicit manual verification):
  - Manual ProxyJump security-key smoke on at least one OpenPGP or FIDO2 target path.
- [ ] Docs:
  - Build/board updates recorded.
- [ ] Observability (if relevant):
  - Auth context markers clearly identify jump vs target stage.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Connect A -> X (jump host) where X requires security-key pubkey auth; verify target auth is attempted and completed or fails with clear error.
- Expected output(s):
  - Deterministic auth stage progression and no state leak.

### Risks / rollout
- Regression areas:
  - Auth retries and method negotiation order.
- Rollback plan:
  - Revert jump-host state-transition patch and restore prior known-good commit.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0241-jump-host-security-key-auth-state-leak.md`
- Related tickets:
  - `TKT-0252`, `TKT-0254`
