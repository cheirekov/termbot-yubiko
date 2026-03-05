# Ticket: TKT-0217 — YubiKey-only policy + SDK migration spike (hwsecurity -> Yubico optional)

## Context capsule (must be complete)
### Goal
- Lock hardware-key support to YubiKey devices and prepare a safe migration path from `hwsecurity` SDK to official Yubico SDK if needed for future Android/SDK upgrades.

### Scope
- In scope:
- Enforce YubiKey-only acceptance in security-key add/setup/auth flows.
- Add clear user-visible error for unsupported hardware key devices.
- Produce compatibility/migration decision notes for `hwsecurity` vs Yubico SDK.
- Out of scope:
- Full replacement with Yubico SDK in this ticket.
- Removal of all `hwsecurity` dependencies in this ticket.

### Constraints
- Platform/runtime constraints:
- Must preserve working YubiKey OpenPGP flow and in-app debug export.
- Security/compliance constraints:
- No logging of PIN/private/APDU payloads.
- Do NOT break:
- Current working two-step SSH publickey flow (`offer -> pk_ok -> sign`) and report export.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `app/src/main/java/org/connectbot/util/SecurityKeySupportPolicy.java`
- `app/src/main/res/values/strings.xml`
- Interfaces/contracts:
- Security key discovery callback and authenticator handoff.

### Acceptance criteria
- [x] Behavior:
- Non-YubiKey devices are rejected with clear message in add/setup/auth flows.
- [x] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds and manual YubiKey flow still works.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Debug logs include unsupported-device marker without sensitive data.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Attempt hardware-key flow with YubiKey and (if available) non-YubiKey device.
- Expected output(s):
- YubiKey path succeeds as before; non-YubiKey path is blocked with explicit error.

### Risks / rollout
- Regression areas:
- False negatives if a YubiKey reports an unexpected product name string.
- Rollback plan:
- Disable policy checks and restore prior generic device acceptance.

## Notes
- Links:
- Validation baseline: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260301-171237.txt`
- Related tickets:
- TKT-0213, TKT-0216

## Implementation progress (2026-03-01)
- Added `SecurityKeySupportPolicy` and enforced YubiKey-only checks in:
- `PubkeyListActivity` add/setup flows
- `SecurityKeyActivity` auth flow
- Added explicit unsupported-device FLOW marker in add/setup path:
- `PUBKEY_UNSUPPORTED_SECURITY_KEY_DEVICE` via `SecurityKeyDebugLog.logFlow(...)`
- Added SDK abstraction seam for authenticator handoff:
- `SecurityKeyAuthenticatorBridge` interface
- `HwSecurityAuthenticatorBridge` adapter
- Refactored `SecurityKeyActivity`, `SecurityKeyService`, and `SecurityKeySignatureProxy` to use bridge type instead of direct hwsecurity authenticator/dialog objects in service/proxy layers.
- Build PASS:
- `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-01T17-49-49+02-00.log`

## Migration notes (spike output)
- Current baseline stack is old (`compileSdk/targetSdk 29`, AGP `4.0.1`, Gradle `6.1.1`) while `hwsecurity` is pinned to `4.4.0`.
- For lowest-risk delivery, keep current hwsecurity runtime path for now and continue through the new bridge seam to isolate service/proxy/auth flow from vendor SDK types.
- Next migration step should evaluate Android/AGP/Gradle uplift first, then swap bridge implementation from `HwSecurityAuthenticatorBridge` to a Yubico-backed bridge in a dedicated ticket.
