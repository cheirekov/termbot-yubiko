# Ticket: TKT-0211 — In-app debug report export for NFC/OpenPGP PIN-path diagnosis

## Context capsule (must be complete)
### Goal
- Add an in-app, shareable debug report so NFC/OpenPGP failures can be diagnosed on devices without `adb`/`logcat` access.

### Scope
- In scope:
- Persistent in-app debug ring buffer.
- Capture hwsecurity logs via custom `SecurityKeyManagerConfig.Builder().setLoggingTree(...)` in debug builds.
- Capture Termbot security-key flow markers across activity/service/proxy/sign start/end/timeout.
- Export report from app UI and share through Android share sheet.
- Out of scope:
- Remote telemetry backends.
- Release-channel analytics or network upload.

### Constraints
- Platform/runtime constraints:
- Android app minSdk 14; must work without host `adb`.
- Security/compliance constraints:
- Never include PIN values, private key material, or full APDU payloads.
- Do NOT break:
- Existing security-key authentication behavior and lifecycle-safe handoff from TKT-0201..0203.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/ConnectBotApplication.java`
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `app/src/main/java/org/connectbot/service/SecurityKeyService.java`
- `app/src/main/java/org/connectbot/SecurityKeySignatureProxy.java`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugLog.java`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/menu/security_key_activity_menu.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/res/values/strings.xml`
- `app/build.gradle`
- Interfaces/contracts:
- Security-key auth UI and service/proxy handoff.
- Android FileProvider + ACTION_SEND sharing contract.

### Acceptance criteria
- [x] Behavior:
- Persistent ring buffer captures hwsecurity debug logs (debug build) and security-key flow markers.
- [x] Tests (or explicit manual verification):
- Docker `assembleDebug` succeeds.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Exported report includes device/app/hwsecurity/NFC state and last 200 timestamped log lines.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Open security-key flow, trigger NFC/OpenPGP attempt, then in `SecurityKeyActivity` choose `Export Debug Report` and share the generated text file.
- Expected output(s):
- File generated at `getExternalFilesDir("debug")/termbot-report-<timestamp>.txt` and share chooser opens.

### Risks / rollout
- Regression areas:
- File sharing/URI permissions on older Android variants.
- Debug log sanitization may hide some low-level details while avoiding sensitive data.
- Rollback plan:
- Revert new debug logging/export files and related wiring/menu/provider entries.

## Notes
- Links:
- `references/prompt_bundles/test_build_android_master.md`
- Related tickets:
- TKT-0201, TKT-0202, TKT-0203
