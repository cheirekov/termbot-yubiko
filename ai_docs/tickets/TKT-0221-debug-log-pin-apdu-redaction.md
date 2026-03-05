# Ticket: TKT-0221 — Debug log PIN/APDU redaction hardening

## Context capsule (must be complete)
### Goal
- Ensure exported in-app debug reports never expose PIN or sensitive APDU payload bytes.

### Scope
- In scope:
- Add strict redaction for sensitive hwsecurity/YubiKey log lines before ring-buffer write/export.
- Add explicit safe markers to preserve troubleshooting value without raw secrets.
- Out of scope:
- Functional changes to SSH auth flow.

### Constraints
- Platform/runtime constraints:
- Must work in both `ossDebug` and `googleDebug`.
- Security/compliance constraints:
- Never log PIN, private key material, or full APDU payloads.
- Do NOT break:
- Existing debug-report export/share flow.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugLog.java`
- `app/src/main/java/org/connectbot/ConnectBotApplication.java`
- `app/src/main/java/org/connectbot/util/SecurityKeyDebugReportExporter.java`
- Interfaces/contracts:
- `SecurityKeyManagerConfig.Builder().setLoggingTree(...)` output normalization.

### Acceptance criteria
- [ ] Behavior:
- Debug report contains redacted markers for sensitive APDU/PIN-related lines.
- [ ] Tests (or explicit manual verification):
- Manual run confirms no raw PIN/APDU payload appears in exported report.
- [ ] Docs:
- Ticket + board + build status updated.
- [ ] Observability (if relevant):
- Redaction markers make it clear which sensitive category was redacted.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Perform security-key auth attempt, export debug report, inspect for raw PIN/APDU payload.
- Expected output(s):
- No PIN or raw APDU payload bytes present in export.

### Risks / rollout
- Regression areas:
- Over-redaction reducing debugging value.
- Rollback plan:
- Keep previous formatter path behind a quick toggle while retaining hard safety defaults.

## Implementation (2026-03-02)
- `SecurityKeyDebugLog.java`:
  - `HEX_PAYLOAD_PATTERN` threshold lowered from `{16,}` to `{4,}` pairs.
    - Catches all APDU command headers ≥4 bytes (covers VERIFY PIN, SELECT AID, long CTAP payloads).
    - Short ISO 7816 status words (≤3 pairs, e.g. `6d00`, `sw1=90`) intentionally remain visible — public protocol codes.
  - Added `RESPONSE_APDU_DATA_PATTERN` → redacts `ResponseApdu{data=<hex>` to `ResponseApdu{data=<apdu-data-redacted>`.
  - Added `NFC_TAG_UID_PATTERN` → redacts `Discovered NFC tag (<uid>)` to `Discovered NFC tag (<tag-uid-redacted>)`.
  - Both new patterns wired into `sanitizeForDebugReport()` before general hex pass.
- Build result: `BUILD SUCCESSFUL in 26s` — ossDebug + googleDebug, 27 warnings (pre-existing), zero errors.

## Notes
- Links:
- Evidence report: `/home/yc/work/ai-projects-templates/workspace/termbot-report-20260301-221052.txt`
- Related tickets:
- TKT-0211, TKT-0213, TKT-0220
