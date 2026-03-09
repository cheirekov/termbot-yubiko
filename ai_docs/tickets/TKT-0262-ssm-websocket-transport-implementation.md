# Ticket: TKT-0262 — SSM WebSocket transport implementation

## Context capsule (must be complete)
### Goal
- Implement a first working SSM transport that can open an interactive session stream and connect it to the terminal bridge.

### Scope
- In scope:
  - Add `SSM` transport implementing `AbsTransport`.
  - Implement minimal SSM StartSession request path and stream channel lifecycle.
  - Register transport in `TransportFactory` without regressing existing protocols.
  - Add non-secret lifecycle markers for connect/open/close/error.
- Out of scope:
  - Full credential UX/storage (TKT-0263).
  - Host editor UX and protocol-specific fields (TKT-0264).

### Constraints
- Platform/runtime constraints:
  - Must work on Android minSdk 19 and current Gradle/AGP stack.
- Security/compliance constraints:
  - No credentials/session tokens in logs.
- Do NOT break:
  - SSH/Telnet/Local connection behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java` (new)
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/TransportFactory.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/service/TerminalBridge.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/aws/` (new package)
- Interfaces/contracts:
  - `AbsTransport.connect/read/write/close/isConnected/isSessionOpen` contract.

### Acceptance criteria
- [x] Behavior:
  - Transport can connect/disconnect and exchange terminal stream data for at least one SSM target.
- [x] Tests (or explicit manual verification):
  - [x] Docker build is green.
  - [x] Device/manual smoke proves connect + command I/O + disconnect path.
- [x] Docs:
  - Build status and board updated.
- [x] Observability (if relevant):
  - SSM non-secret markers exist for start/open/close/error.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Create SSM host entry (temporary config path), connect, run `uname -a`, disconnect.
- Expected output(s):
  - Interactive session output appears in terminal, and disconnect is clean.

### Risks / rollout
- Regression areas:
  - Transport thread lifecycle and read/write buffering behavior.
- Rollback plan:
  - Feature-gate SSM transport registration; preserve existing transport set.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0261-ssm-transport-feasibility-spike.md`
- Related tickets:
  - `TKT-0263`, `TKT-0264`
- Slice progress (2026-03-06):
  - Added StartSession bootstrap with SigV4 signing and safe error surfacing.
  - Current temporary field mapping in host editor: `Username -> access key ID`, `Host -> AWS region`, `Post-login field -> SSM target`, `Remember password -> encrypted secret access key storage`.
  - Added stream URL/token websocket bridge using SigV4-authenticated `ssmmessages` channel, SSM binary frame handling (input/output/ack/channel closed), handshake request/response support, and terminal read/write integration through `AbsTransport`.
  - Operator smoke (2026-03-06) confirmed: interactive connect works, `uname -a` output received, `whoami` returns `ssm-user`, and `exit` cleanly disconnects.
  - Marking `TKT-0262` done; host-editor UX papercuts roll into `TKT-0264`.
