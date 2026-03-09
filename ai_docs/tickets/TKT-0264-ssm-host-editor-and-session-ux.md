# Ticket: TKT-0264 — SSM host editor UX + session UI integration

## Context capsule (must be complete)
### Goal
- Expose SSM as a first-class host type with clear configuration and session-state UX.

### Scope
- In scope:
  - Add SSM protocol selection and protocol-specific host fields in host editor.
  - Validate required SSM fields before save/connect.
  - Surface SSM-specific session statuses/errors in terminal/activity UI.
- Out of scope:
  - Large redesign of host editor architecture.

### Constraints
- Platform/runtime constraints:
  - Must preserve existing HostEditor behavior for SSH/Telnet/Local.
- Security/compliance constraints:
  - No credential values in field previews, error text, or logs.
- Do NOT break:
  - Existing quick-connect and saved-host edit flows.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/HostEditorFragment.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/EditHostActivity.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/ConsoleActivity.java`
  - `repos/termbot-termbot/app/src/main/res/layout/`
  - `repos/termbot-termbot/app/src/main/res/values/strings.xml`
- Interfaces/contracts:
  - Host editor protocol-specific field/validation contract.

### Acceptance criteria
- [x] Behavior:
  - User can create/edit an SSM host with required fields and connect from host list.
- [x] Tests (or explicit manual verification):
  - Manual create/edit/connect/disconnect smoke for SSM host entries.
- [x] Docs:
  - User-facing notes for SSM fields and limitations.
- [x] Observability (if relevant):
  - Session states include SSM stage markers (non-secret).

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Add SSM host in UI and verify required-field validation + successful launch path.
- Expected output(s):
  - SSM host appears and behaves like other first-class host types.

### Risks / rollout
- Regression areas:
  - Protocol branching in host editor affecting existing protocols.
- Rollback plan:
  - Keep SSM option hidden by feature flag if validation is unstable.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0262-ssm-websocket-transport-implementation.md`
- Related tickets:
  - `TKT-0265`
- Operator feedback (2026-03-06):
  - Interactive SSM session path is working, but host-add/edit flow has minor UI bugs/papercuts.
  - This ticket is the next cleanup lane after `TKT-0262` completion.
- Slice progress (2026-03-06, first fix):
  - Fixed quick-connect SSM parsing for access-key-only paste input:
    - before: `AKI...` in main address field was interpreted as Region (`hostname`)
    - now: it maps to AWS Access Key ID (`username`) when no `@`/`/` is present and input looks like an access key ID.
  - Added SSM host completeness validation in editor before save-enable path (requires access key ID + region + target).
  - Verification: Docker `assembleDebug` passed; log `references/logs/android_build_2026-03-06T17-26-52+02-00.log`.
- Slice progress (2026-03-06, second fix):
  - Fixed first-connect SSM secret-key prompt race in prompt lifecycle:
    - before: first connect could show only `Connecting...`; password prompt appeared only after backing out and re-entering host.
    - now: if a prompt is already pending when Console UI handler attaches, it is surfaced immediately.
  - Implementation: `PromptHelper.setHandler(...)` now emits a prompt message when `promptRequested != null`.
  - Verification: Docker `assembleDebug` passed; log `references/logs/android_build_2026-03-06T17-30-56+02-00.log`.
- Slice progress (2026-03-06, third fix):
  - Fixed SSM quick-connect top-field typing stability when entering full compact address (`AKIA...@region/i-target`):
    - before: as user typed, top field could be overwritten/cleared due recursive sync from SSM target field watcher.
    - now: quick-connect parsing + URI-part field sync runs under one guarded edit transaction, preventing recursive overwrite.
  - Also clears stale SSM target when quick-connect input becomes invalid.
  - Verification: Docker `assembleDebug` passed; log `references/logs/android_build_2026-03-06T17-48-48+02-00.log`.
- Slice progress (2026-03-06, fourth fix):
  - Moved the SSM target editor from the lower page section into the expanded URI section (near Access Key / Region / Port) for faster SSM host setup.
  - Kept a single shared field binding (`postLogin`) to preserve behavior for non-SSM protocols while improving SSM placement.
  - Verification: Docker `assembleDebug` passed; log `references/logs/android_build_2026-03-06T17-58-42+02-00.log`.
- Slice progress (2026-03-06, fifth fix):
  - Applied strict SSM-only visibility for the target block in host editor:
    - visible only when protocol is `ssm`
    - hidden for `ssh`/`telnet`/`local`
  - Implementation: wrapped target widgets in `postlogin_section_container` and toggled visibility in `updatePostLoginLabels(...)`.
  - Verification: Docker `assembleDebug` passed; log `references/logs/android_build_2026-03-06T19-33-08+02-00.log`.
- Operator smoke confirmation (2026-03-07):
  - Confirmed: SSM target movement is correct in expanded URI section.
  - Confirmed: strict visibility works (`SSM` shows target block, `SSH`/`Telnet` do not).
