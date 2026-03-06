# Ticket: TKT-0261 — SSM transport feasibility spike (protocol, deps, credential flow)

## Context capsule (must be complete)
### Goal
- Produce an implementation-ready feasibility decision for SSM transport integration that is grounded in current TermBot architecture and constraints.

### Scope
- In scope:
  - Map existing transport/runtime touchpoints required for SSM support.
  - Compare feasible implementation approaches for StartSession + stream channel handling.
  - Define dependency strategy for SigV4 signing + WebSocket on Android minSdk 19.
  - Produce a phased implementation contract for TKT-0262..0265.
- Out of scope:
  - Full SSM transport implementation.
  - Full credential UX and persistence implementation.

### Constraints
- Platform/runtime constraints:
  - Android app, `minSdkVersion 19`, AGP 4.2.2, Java 8 source compatibility.
- Security/compliance constraints:
  - Never log AWS credentials, session tokens, or signed headers.
- Do NOT break:
  - Existing SSH/Telnet/Local transports.
  - Existing security-key auth/import behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/service/TerminalBridge.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/HostEditorFragment.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/HostDatabase.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`
  - `repos/termbot-termbot/app/build.gradle`
- Interfaces/contracts:
  - `AbsTransport` lifecycle contract and `TransportFactory` protocol registration.

### Acceptance criteria
- [ ] Behavior:
  - Feasibility outcome picks one recommended implementation path.
- [ ] Tests (or explicit manual verification):
  - N/A (spike/docs-only slice).
- [ ] Docs:
  - Feasibility doc added with architecture impact and phased plan.
  - ADR added for the proposed SSM transport direction.
  - Open unknowns appended to `ai_docs/docs/QUESTIONS.md`.
- [ ] Observability (if relevant):
  - Initial non-secret marker policy for SSM path is drafted.

### Verification (token-efficient)
- Docker command(s) to run:
  - N/A (docs-only spike).
- Manual script(s) the user can run:
  - Review feasibility/ADR outputs and approve path for TKT-0262.
- Expected output(s):
  - Decision-ready phased plan with explicit unresolved external-interface questions.

### Risks / rollout
- Regression areas:
  - Underestimating SSM stream protocol complexity.
- Rollback plan:
  - Keep spike outputs as docs-only; defer runtime changes until uncertainties are resolved.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0260-aws-ssm-session-manager-support-epic.md`
- Related tickets:
  - `TKT-0262`, `TKT-0263`, `TKT-0264`, `TKT-0265`
