# Ticket: TKT-0267 — SSM port-forwarding + remote-host tunnel baseline

## Context capsule (must be complete)
### Goal
- Add AWS Systems Manager Session Manager tunnel sessions so TermBot can reach a port on the managed node itself or a remote private host behind that managed node, enabling real bastion-style DB/private-service access without SSHing to the bastion.

### Scope
- In scope:
  - Support Session Manager port-forward session startup using AWS session documents:
    - `AWS-StartPortForwardingSession`
    - `AWS-StartPortForwardingSessionToRemoteHost`
  - Extend the SSM API/session layer to start non-shell sessions with document parameters.
  - Add a tunnel/session transport abstraction suitable for byte-stream forwarding instead of terminal rendering.
  - Preserve the existing SSM credential path, including temporary session credentials and assume-role baseline.
  - Add non-secret tunnel lifecycle markers.
- Out of scope:
  - SSH-specific host editor and auth UX on top of the tunnel.
  - Generic SOCKS/dynamic proxy support.
  - Shared AWS profile manager UI.
  - IAM Identity Center.

### Constraints
- Platform/runtime constraints:
  - Must fit the current Android/Java stack and existing TermBot service lifecycle.
- Security/compliance constraints:
  - Never log credentials, session tokens, or sensitive remote-host details.
- Do NOT break:
  - Existing SSH/Telnet/Local host flows.
  - Existing direct SSM shell sessions.
  - Existing TKT-0266 assume-role behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/aws/`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/service/`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
- Interfaces/contracts:
  - SSM `StartSession` payload construction for document-based sessions
  - Tunnel stream bridge contract for non-terminal byte forwarding

### Acceptance criteria
- [x] Behavior:
  - Start a Session Manager local port-forward session to a port on the managed node.
  - Start a Session Manager remote-host port-forward session to a private host reachable from the managed node.
  - Existing shell-oriented SSM sessions remain unchanged.
- [x] Tests (or explicit manual verification):
  - Managed-node-local tunnel smoke is operator-confirmed (`15432 -> localhost:22`, then SSH to `127.0.0.1:15432`).
  - Remote-host/private-DB smoke is operator-confirmed through a DB client using localhost against the forwarded port.
  - Unreachable remote host/port surfacing is implemented but not yet operator-smoked; retained as a residual validation gap, not a blocker for closing the baseline ticket.
- [x] Docs:
  - Session Manager tunnel documents and supported scenario are explicitly documented.
- [x] Observability (if relevant):
  - Non-secret markers identify tunnel mode and lifecycle without leaking credentials or sensitive hostnames.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Configure a managed node with network access to a private DB or TCP service, then start a tunnel session and verify the target is reachable through the tunnel.
- Expected output(s):
  - Tunnel session established through the managed node without requiring SSH to the bastion itself.

### Risks / rollout
- Regression areas:
  - SSM stream handling for non-terminal sessions.
  - Android socket/service lifecycle while a tunnel is active.
  - Session document parameter handling.
- Rollback plan:
  - Keep tunnel support isolated from shell SSM and disable the new path without affecting direct shell sessions.

## Notes
- Links:
  - AWS Session Manager start-session docs: `https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html`
  - AWS bastion pattern via Session Manager: `https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/access-a-bastion-host-by-using-session-manager-and-amazon-ec2-instance-connect.html`
- Related tickets:
  - `TKT-0260`, `TKT-0263`, `TKT-0266`

## Progress
- 2026-03-08 — Re-scoped after AWS docs review:
  - Withdrew the unsmoked `AWS source host` prototype because it did not match the real AWS bastion/tunnel model.
  - Re-defined `TKT-0267` around Session Manager port forwarding and remote-host tunneling, which is the actual next step for private DB/private-service access through a managed SSM node.
  - No application implementation has started yet under the corrected scope.
- 2026-03-08 — Slice 1 implemented and build-verified:
  - Extended `SsmApiClient` so `StartSession` can send `DocumentName` and `Parameters` for Session Manager tunnel documents.
  - Extended `SsmStreamClient` with raw port-session input path so tunnel bytes are not altered by terminal newline normalization.
  - Added SSM transport local-port-forward support on top of the existing port-forward UI:
    - only `local` forwards are exposed for SSM in this slice
    - destination `localhost:port` maps to `AWS-StartPortForwardingSession`
    - destination `host:port` maps to `AWS-StartPortForwardingSessionToRemoteHost`
  - Added non-secret tunnel lifecycle markers and operator-facing tunnel failure surfacing in the SSM console.
  - Build verification passed: `references/logs/android_build_2026-03-08T12-17-01+02-00.log`
  - Manual device smoke is still pending before the ticket can be marked done.
- 2026-03-08 — Follow-up tunnel-stream fix after first operator smoke:
  - Operator smoke used an SSM host-local forward (`15432 -> localhost:22`) and then a separate SSH host to `127.0.0.1:15432`.
  - The smoke failed during SSH key exchange with `Illegal packet size`, which is consistent with non-SSH bytes being injected into the forwarded socket stream.
  - Root cause found in the Session Manager stream bridge:
    - `PAYLOAD_OUTPUT` and `PAYLOAD_STDERR` were both reaching the forwarded TCP socket path
    - that is acceptable for shell text rendering, but not for binary tunnel traffic
  - Patched the bridge so:
    - `SsmStreamClient.Callback` now distinguishes `onStdout(...)` vs `onStderr(...)`
    - shell sessions still surface both channels in the terminal
    - tunnel sessions write only stdout payload bytes to the local socket and trace stderr as a non-secret marker
  - Follow-up build verification passed: `references/logs/android_build_2026-03-08T15-04-35+02-00.log`
  - Operator re-smoke on that build still failed with the same SSH key-exchange corruption.
- 2026-03-08 — Modern-agent `smux` follow-up implemented after official AWS plugin review:
  - Pulled the official AWS Session Manager plugin source and verified that `LocalPortForwarding` uses `MuxPortForwarding` backed by `xtaci/smux` when the SSM Agent version is greater than `3.0.196.0`; only older agents stay on raw/basic port forwarding.
  - Replaced the modern-agent local tunnel path in TermBot with a minimal client-side `smux` v1 bridge:
    - wait for the Session Manager handshake and capture `AgentVersion`
    - select `raw` forwarding for older agents
    - select `smux_v1` forwarding for modern agents and open a client stream before local SSH starts
    - parse remote `smux` frames and forward only `PSH` payloads to the local socket
  - Added SSM flag-payload handling so `ConnectToPortError` can surface as a tunnel failure instead of opaque socket corruption.
  - Build verification passed: `references/logs/android_build_2026-03-08T15-22-04+02-00.log`
  - Operator re-smoke is required on the new APK before the ticket can be marked done.
- 2026-03-08 — Follow-up after operator report from the `15:22` build:
  - Operator report `termbot-report-20260308-153951.txt` proved the new path was active on device:
    - `protocol=smux_v1`
    - `agent_version=3.3.2299.0`
    - failure moved to `Unexpected SSM smux version 83`
  - That failure means the app was no longer treating the stream as raw TCP, but it was still assuming the first forwarded bytes would always be the first `smux` frame header.
  - Follow-up patch tightened the tunnel protocol bridge:
    - capture `SessionType` and nested `Properties.Type` from the SSM handshake
    - select `smux_v1` only when the port-session type is explicitly `LocalPortForwarding`
    - keep raw fallback for non-mux sessions and older agents
    - tolerate one printable preamble line before the first valid `smux` frame instead of aborting immediately
    - accept inbound `smux` v2 frame markers and `UPD` control frames seen on newer agents
  - Follow-up build verification passed: `references/logs/android_build_2026-03-08T15-47-04+02-00.log`
  - Operator re-smoke is now required on the `15:47` APK before the ticket can move out of Review.
- 2026-03-08 — Operator re-smoke on the `15:47` build:
  - Managed-node SSH tunnel smoke passed with the same scenario that had previously failed:
    - SSM host connected
    - local forward `15432 -> localhost:22`
    - separate SSH host to `127.0.0.1:15432`
  - That confirms the modern-agent mux path is now usable for the managed-node-local tunnel case.
  - Operator also confirmed the remote-host/private-DB scenario by creating a forward to the DB host/port and successfully connecting from the DB client to localhost on the phone.
  - Residual risk kept explicit:
    - unreachable remote host / port surfacing is implemented but not yet operator-smoked
    - app backgrounding currently closes live sessions; captured separately in `TKT-0269`
