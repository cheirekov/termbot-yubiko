# Ticket: TKT-0227 — Replace hwsecurity SDK with YubiKit SDK

## Context capsule (must be complete)
### Goal
- Remove the `de.cotech.hw` (hwsecurity) SDK and replace all integration points with
  Yubico's official [YubiKit for Android](https://github.com/Yubico/yubikit-android) SDK.

### Background / decision trigger
- hwsecurity is a third-party wrapper; the team has chosen to standardise on the first-party
  YubiKit SDK for long-term maintainability and direct vendor support.
- OpenPGP, PIV, FIDO2, and NFC/USB transport layers are all currently backed by hwsecurity.
- Any UI elements (dialogs, activities, fragments) that reference hwsecurity types must also
  be updated or removed as part of this work.

### Scope
- In scope:
  - Remove `de.cotech:hwsecurity-*` gradle dependencies from `app/build.gradle`.
  - Replace `SecurityKeyManager` / `SecurityKeyManagerConfig` bootstrap in `ConnectBotApplication`.
  - Replace `SecurityKeyDialogFragment` / `SecurityKeyActivity` with YubiKit equivalents or
    custom minimal dialogs that no longer import hwsecurity types.
  - Replace `HwYubiKitSmartCardConnection` and all `de.cotech.hw.*` imports across
    `securitykey/` package.
  - Replace `HwTimber` logging tree with a plain `Timber.Tree` or direct ring-buffer logging.
  - Update `SecurityKeyDebugLog.newHwLoggingTree()` to remove `HwTimber` dependency.
  - Update `PivSecurityKeyAuthenticatorBridge`, `OpenPgpSecurityKeyAuthenticatorBridge`,
    and any FIDO2 bridge that imports hwsecurity types.
  - Retain full feature parity: OpenPGP auth, PIV auth, FIDO2 SSH-SK, NFC + USB transports.
- Out of scope:
  - New hardware support beyond what hwsecurity currently provides.
  - UI redesign beyond the minimum needed to remove hwsecurity dialog types.

### Constraints
- Platform/runtime constraints:
  - Must work in both `ossDebug` and `googleDebug` flavours.
  - minSdk and targetSdk constraints unchanged.
- Security/compliance constraints:
  - PIN/APDU redaction rules from TKT-0221 must be preserved or improved.
  - No private key material in logs.
- Do NOT break:
  - OpenPGP SSH auth (primary daily-driver path).
  - PIV SSH auth (confirmed working, TKT-0222).
  - FIDO2 SSH-SK flow (TKT-0223).
  - ProxyJump / saved-password flows.
  - Encrypted backup/restore (TKT-0218).
  - Debug report export and ring-buffer logging.

### Target areas
- Files/modules:
  - `app/build.gradle` — dependency swap
  - `app/src/main/java/org/connectbot/ConnectBotApplication.java`
  - `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
  - `app/src/main/java/org/connectbot/ui/SecurityKeyDialogFragment.java` (and related)
  - `app/src/main/java/org/connectbot/securitykey/*` — all bridge/transport files
  - `app/src/main/java/org/connectbot/util/SecurityKeyDebugLog.java`
  - `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
  - Any layout XML, `strings.xml`, or menu resource that references hwsecurity UI labels.
- Interfaces/contracts:
  - Replace `HwTimber.Tree` with a plain logging abstraction.
  - Replace `SecurityKeyManagerConfig.Builder` with YubiKit `YubiKitManager` init.

### Acceptance criteria
- [ ] Behavior:
  - Zero `de.cotech` import statements remain in the codebase after migration.
  - All three auth paths (OpenPGP, PIV, FIDO2) pass manual device verification.
- [ ] Build:
  - `assembleDebug` passes with no new errors in ossDebug and googleDebug.
- [ ] UI:
  - No hwsecurity dialog or activity types remain in layouts or Java sources.
- [ ] Docs:
  - Ticket + board + build status updated.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Grep for remaining hwsecurity references: `grep -r "de.cotech" repos/termbot-termbot/app/src`
  - Expected: no output.
- Manual device tests:
  - OpenPGP NFC auth — tap YubiKey, enter PIN, SSH connects.
  - PIV NFC auth — tap YubiKey, SSH connects.
  - FIDO2 SSH-SK — tap YubiKey, SSH connects.

### Risks / rollout
- Risk: YubiKit API surface differs from hwsecurity — APDU layer, transport callbacks,
  and PIN-entry dialog conventions will need adaptation.
- Risk: hwsecurity provided a USB intent-filter helper; YubiKit has its own USB dispatch
  mechanism — AndroidManifest intent filters may need updating.
- Rollback plan:
  - Spike branch — keep hwsecurity in place until full parity is confirmed in a build branch.
  - Can revert `app/build.gradle` dependency swap as a fast rollback point.

### Recommended approach (spike first)
1. Add YubiKit dependencies alongside hwsecurity (parallel for one sprint).
2. Implement transport adapter (`YubiKitSmartCardConnection`) implementing same interface.
3. Swap auth bridges one at a time (OpenPGP → PIV → FIDO2), verifying build after each.
4. Remove hwsecurity UI (dialogs/activities) last, after transport+auth are verified.
5. Remove hwsecurity gradle dependencies and grep-verify zero `de.cotech` references.

## Notes
- Related tickets:
  - TKT-0217 (SDK migration spike — original decision, ticket file missing)
  - TKT-0220, TKT-0221, TKT-0222, TKT-0223
- YubiKit Android: https://github.com/Yubico/yubikit-android
- hwsecurity: https://hwsecurity.dev