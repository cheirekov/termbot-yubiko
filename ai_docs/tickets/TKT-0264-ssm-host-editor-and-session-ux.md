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
- [ ] Behavior:
  - User can create/edit an SSM host with required fields and connect from host list.
- [ ] Tests (or explicit manual verification):
  - Manual create/edit/connect/disconnect smoke for SSM host entries.
- [ ] Docs:
  - User-facing notes for SSM fields and limitations.
- [ ] Observability (if relevant):
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
