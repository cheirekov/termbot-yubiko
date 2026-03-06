# SSM Feasibility Spike (TKT-0261)

Date: 2026-03-06
Ticket: `TKT-0261`
Scope: AWS SSM Session Manager integration feasibility on current TermBot architecture

## Evidence baseline (workspace-only)
- Transport plug-in model is centralized in `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/TransportFactory.java` and `AbsTransport.java`.
- Terminal runtime depends on `AbsTransport.connect/read/write/close` contract via `TerminalBridge.startConnection()` in `repos/termbot-termbot/app/src/main/java/org/connectbot/service/TerminalBridge.java`.
- Host model and persistence currently include protocol-agnostic core fields only (`protocol`, `hostname`, `port`, etc.) in:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/HostDatabase.java`
- Host editor protocol-specific UX is hardcoded by protocol branch in `HostEditorFragment.setTransportType(...)` in `repos/termbot-termbot/app/src/main/java/org/connectbot/HostEditorFragment.java`.
- Backup/export serializes host core fields only in `repos/termbot-termbot/app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`.
- Build constraints from `repos/termbot-termbot/app/build.gradle`:
  - `minSdkVersion 19`
  - Java 8 source/target
  - No existing AWS/HTTP WebSocket client dependency in app dependencies.

## Feasibility outcome
SSM support is feasible without rewriting terminal internals, but requires a phased path because the current architecture assumes protocol-specific transport + host DB fields and does not yet contain an SSM protocol/client stack.

## Recommended implementation path
1) Add a dedicated `SSM` transport that conforms to `AbsTransport` and is created by `TransportFactory`.
2) Add a minimal `org.connectbot.aws` client layer for:
- SigV4 request signing
- StartSession request handling
- stream channel lifecycle adapter used by `SSM` transport
3) Defer optional YubiKey MFA specifics until the base credential path works.
4) Add SSM host fields additively in DB/model and include them in encrypted backup JSON.

## Option comparison
### Option A — Full AWS SDK integration in app
- Pros:
  - Faster feature completeness if SDK compatibility is clean.
- Cons:
  - Risk of dependency weight and Android compatibility complexity on current stack.
  - Higher APK growth risk.
- Result: not chosen for first slice.

### Option B — Thin in-app SSM client (SigV4 + session stream adapter)
- Pros:
  - Better control over dependency footprint.
  - Easier to align with existing transport abstraction.
- Cons:
  - Requires careful protocol validation and stronger manual smoke.
- Result: selected for TKT-0262 baseline direction.

### Option C — External proxy/service mediating SSM
- Pros:
  - Offloads protocol complexity from app.
- Cons:
  - Introduces extra deployment component and trust boundary.
  - Not aligned with current single-app product scope.
- Result: not selected for this epic.

## Minimum slice map
### Slice 1 (current): TKT-0261 spike
- Deliverables:
  - this feasibility doc
  - ADR with phased decision
  - open unknowns captured in `QUESTIONS.md`

### Slice 2: TKT-0262 transport backbone
- Add `transport/SSM.java` + `aws` client package skeleton.
- Register protocol in `TransportFactory` behind controlled rollout.
- Achieve one manual connect/disconnect happy path.

### Slice 3: TKT-0263 credentials
- Add initial credential mode(s) with redaction-safe handling.
- Add error taxonomy for auth failures.

### Slice 4: TKT-0264 host editor/session UX
- Add protocol-specific editor fields and validation.
- Improve SSM session state messaging.

### Slice 5: TKT-0265 persistence and backup
- DB/model migration (additive).
- Backup/export/import SSM round-trip support.

## Initial technical constraints to carry forward
- Keep diffs small; avoid mixing schema + UI + transport in one patch.
- Require non-secret markers only (credential type category allowed; values forbidden).
- Preserve existing SSH/Telnet/Local behavior unchanged during SSM onboarding.

## Exit criteria for TKT-0261
- Sub-ticket decomposition exists (`TKT-0262..TKT-0265`).
- Delivery board reflects active work on `TKT-0261`.
- ADR drafted and linked.
- Open protocol/credential unknowns recorded as explicit questions.

## Kickoff code artifact (started)
- Added non-UI-exposed SSM transport scaffold:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/TransportFactory.java` (`ssm` handling only; transport not yet listed in `transportNames`)
- Verified compile via Docker `assembleDebug` (log: `references/logs/android_build_2026-03-06T16-03-17+02-00.log`).
