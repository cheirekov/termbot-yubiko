# Quality Plan (Token-efficient)

## Principle
AI proposes commands; humans run them; humans paste outputs back for grounded iteration.

## Testing pyramid
- Unit:
- Integration:
- E2E (only where it pays off):

## Quality gates (CI)
- Formatting/lint
- Typecheck (if applicable)
- Unit tests
- Dependency scan (where practical)

## Security-key regression sentinel matrix (manual, high-signal)

### Purpose
- Catch high-impact regressions fast across provider + transport combinations used in production-like device flows.

### Preconditions
- Debug APK installed from latest successful Docker build.
- At least one known-good host per provider path (OpenPGP/PIV/FIDO2).
- YubiKey available over NFC and USB (where supported by case).

### Failure capture rule (mandatory)
- If any case fails, immediately export in-app debug report from Host List menu.
- Attach report file from:
  - `Android/data/com.sonelli.juicessh/files/debug/termbot-report-<timestamp>.txt`
- Do not include PIN/private key/APDU payloads in notes.

### Core cases
| ID | Provider / Flow | Transport | Pass condition |
|---|---|---|---|
| SK-01 | OpenPGP key import | NFC | Key import completes and appears in key list |
| SK-02 | OpenPGP SSH auth | NFC | Host login succeeds with key auth (no fallback to password) |
| SK-03 | PIV key import | NFC | Key import completes and appears in key list |
| SK-04 | PIV SSH auth | NFC or USB | Host login succeeds with key auth |
| SK-05 | FIDO2 key import | USB | FIDO2 key import completes with configured slot and optional public key hint |
| SK-06 | FIDO2 SSH auth | USB | Host login succeeds after PIN/touch flow where required |
| SK-07 | FIDO2 SSH auth | NFC | Host login succeeds via NFC transport |
| SK-08 | ProxyJump target auth | NFC or USB | `A -> X` path succeeds and target host still performs key auth/sign |

### Execution checklist (quick run)
1. Run `SK-01` through `SK-08` in order.
2. For each case, mark `PASS`/`FAIL` and one-line reason.
3. On first failure, export debug report and include case ID in report filename/comment.
4. If a failure is fixed, re-run all previously passed cases to detect regressions.

### Baseline dry run record (2026-03-05)
- Build under test:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
  - Log: `references/logs/android_build_2026-03-04T17-30-28+02-00.log`
- Baseline result source:
  - User-confirmed device smoke outcomes across completed tickets (`TKT-0241`, `TKT-0242`, `TKT-0244`) and follow-up confirmations in the working session.
- Baseline status:
  - `SK-01` PASS
  - `SK-02` PASS
  - `SK-03` PASS
  - `SK-04` PASS
  - `SK-05` PASS
  - `SK-06` PASS
  - `SK-07` PASS
  - `SK-08` PASS
