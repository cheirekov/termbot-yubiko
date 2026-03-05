# Ticket: TKT-0203 — Fail-fast auth error + safe log markers

## Context capsule (must be complete)
### Goal
- Ensure security-key auth failures are surfaced quickly to user-visible terminal output and tagged with safe log markers for diagnosis.

### Scope
- In scope:
- Add explicit fail-fast messages in SSH security-key auth path.
- Emit consistent non-sensitive log markers on timeout/race conditions.
- Out of scope:
- Full telemetry pipeline or remote logging changes.

### Constraints
- Platform/runtime constraints:
- Existing logging/output framework in ConnectBot.
- Security/compliance constraints:
- Never emit PINs, digests, private key material, or raw challenges.
- Do NOT break:
- Standard authentication retries for non-security-key methods.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/transport/SSH.java`
- `app/src/main/res/values/strings.xml`
- Interfaces/contracts:
- SSH auth loop behavior and terminal output lines.

### Acceptance criteria
- [ ] Behavior:
- On security-key timeout/error, user sees clear terminal failure line quickly.
- [ ] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds.
- [ ] Docs:
- Ticket + board + build status updated.
- [ ] Observability (if relevant):
- Logcat shows stable markers for timeout/race failures.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Attempt OpenPGP auth and capture logcat with marker filter.
- Expected output(s):
- Terminal message indicates fail-fast security-key error; marker present in logcat.

### Risks / rollout
- Regression areas:
- Message wording may need refinement for localization.
- Rollback plan:
- Revert added output/log-marker lines and new strings.

## Notes
- Links:
- `references/prompt_bundles/test_build_android_master.md`
- Related tickets:
- TKT-0201, TKT-0202
