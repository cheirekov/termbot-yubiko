# Ticket: TKT-0201 — Security key timeout guard in auth handoff/sign flow

## Context capsule (must be complete)
### Goal
- Prevent infinite waits during OpenPGP hardware-key authentication by adding bounded timeouts to blocking waits.

### Scope
- In scope:
- Add timeout guards in security-key signature flow where waits are currently unbounded.
- Surface timeout as a safe, actionable authentication failure.
- Out of scope:
- Refactoring auth architecture or replacing hwsecurity SDK.

### Constraints
- Platform/runtime constraints:
- Android app runtime; must compile with existing Gradle/AGP setup.
- Security/compliance constraints:
- Do not log PINs, secrets, key material, or challenge payloads.
- Do NOT break:
- Non-security-key SSH auth paths.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/SecurityKeySignatureProxy.java`
- Interfaces/contracts:
- Trilead `SignatureProxy#sign(...)` behavior (return signature or throw `IOException`).

### Acceptance criteria
- [ ] Behavior:
- Security-key auth no longer blocks forever waiting for UI/service handoff.
- [ ] Tests (or explicit manual verification):
- `assembleDebug` succeeds in Docker.
- [ ] Docs:
- Ticket + board + build status updated.
- [ ] Observability (if relevant):
- Timeout emits a log marker without secrets.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Install debug APK and attempt OpenPGP auth; verify failure occurs with bounded time instead of endless spinner.
- Expected output(s):
- Build PASS and timeout error shown/recorded instead of indefinite wait.

### Risks / rollout
- Regression areas:
- Slower hardware interactions may hit timeout too early.
- Rollback plan:
- Revert timeout constants/logic in `SecurityKeySignatureProxy`.

## Notes
- Links:
- `references/prompt_bundles/test_build_android_master.md`
- Related tickets:
- TKT-0202, TKT-0203
