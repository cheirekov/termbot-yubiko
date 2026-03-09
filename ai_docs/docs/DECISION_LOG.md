# Decision Log

## 2026-02-27 — Pack installed / initialized
- Initialize local AI operating pack (ai_docs/ + references/).

## 2026-03-05 — Process reliability guardrails for long sessions
- Add a recurring AI-process retro checkpoint to control context drift.
- Treat review-column WIP limits and session handoff capsules as first-class process controls.

## 2026-03-05 — Review queue policy activated
- Enforce `Review` WIP limit of 2 tickets with oldest-first closure.
- Apply transition freeze when historical `Review` queue is already above the limit.

## 2026-03-05 — Standardized session handoff capsule adopted
- Add a 2-minute handoff template in handbook with required fields: last good build, active risks, open review items, and next actions.

## 2026-03-05 — Security-key sentinel smoke matrix adopted
- Treat `SK-01..SK-08` matrix as mandatory release smoke gate for OpenPGP/PIV/FIDO2 and ProxyJump key-auth paths.
- Require immediate in-app debug report export on first smoke failure.

## 2026-03-06 — SSM epic phased rollout path proposed
- Start SSM work with feasibility spike (`TKT-0261`) and explicit sub-ticket decomposition (`TKT-0262..TKT-0265`).
- Prefer thin in-app SSM client approach over immediate full AWS SDK bundling, pending open scope decisions.

## 2026-03-06 — SSM scope decisions confirmed by user
- Initial credential mode: AWS access key ID + secret access key.
- IAM Identity Center is explicitly deferred to backlog.
- Initial ship can proceed without YubiKey MFA integration.
- Start with StartSession shell-oriented bootstrap before role-chain/jump scenarios.

## 2026-03-06 — TKT-0262 implementation phase moved to real stream bridge
- Keep `TKT-0262` focused on shell-session websocket transport first (handshake + output/input/ack + disconnect), before advanced role-chain and multi-account jump flows.
- Keep KMSEncryption handshake action unsupported in this slice; treat encryption-specific handling as follow-up hardening after first interactive smoke.

## 2026-03-06 — TKT-0262 accepted after operator smoke
- Operator validated interactive SSM flow on device: connect, command output (`uname -a`, `whoami`), and clean `exit` disconnect.
- Move focus to `TKT-0264` for host-editor/session UX bug cleanup, then `TKT-0263` for credential/role expansion.

## 2026-03-06 — TKT-0264 quick-connect parsing rule for access-key-only input
- In SSM host editor quick-connect parsing, when input looks like an AWS access key ID and contains no `@`/`/`, treat it as `AWS Access Key ID` (`username`) rather than `Region` (`hostname`).
- Enforce SSM required fields (access key ID + region + target) before the host is considered valid for save.

## 2026-03-06 — TKT-0264 prompt lifecycle handoff policy
- When a session prompt is raised before `ConsoleActivity` prompt handler attachment, keep it visible by replaying the prompt signal during `PromptHelper.setHandler(...)` if `promptRequested` is already set.
- This prevents first-connect SSM password prompt loss caused by UI/service handler attach timing.

## 2026-03-06 — TKT-0264 quick-connect editing transaction for SSM
- Treat quick-connect parse + URI-parts synchronization as one guarded transaction (`mUriFieldEditInProgress`) to prevent recursive quick-field overwrite while typing SSM full compact address.
- On invalid quick-connect parse, clear stale SSM target (`postLogin`) to keep derived fields consistent.

## 2026-03-06 — TKT-0264 SSM target placement in editor
- Move `postLogin` editor block into the expanded URI section so SSM target is colocated with AWS access key ID, region, and port.
- Keep one shared `postLogin` field binding across protocols to avoid save-schema or behavior divergence.

## 2026-03-06 — TKT-0264 strict SSM-only target visibility
- Constrain target block visibility to `ssm` protocol only (`postlogin_section_container` hidden for other protocols), per operator UX preference.
- Keep SSM-specific title/summary/hint logic in `updatePostLoginLabels(...)` as the single control point for protocol-specific post-login UI state.

## 2026-03-07 — TKT-0264 accepted after operator host-editor smoke
- Operator confirmed final host-editor UX fixes, including strict SSM-only target visibility.
- Move active implementation focus to `TKT-0263` (credential expansion toward roles/jump scenarios).

## 2026-03-07 — TKT-0263 gated by PM/BA + UX prioritization
- Treat `TKT-0263` as broader MFA + role/jump readiness scope (not only YubiKey-specific follow-up).
- Pause implementation sequencing until PM/BA and UX clarify where role/account-jump inputs should live and what MFA modes are phase-1.

## 2026-03-07 — TKT-0263 Slice A storage and contract decision
- Keep Slice A backend-first: add credential-resolution contract and scoped secure-storage for SSM secrets/session tokens without shipping role/account-jump UI fields yet.
- Use marker-only observability in SSM flow (`credential_mode`, `secret_source`, `mfa_prompted`) and prohibit logging credential values.
- Preserve backward compatibility by reading legacy saved-password slot for SSM once and migrating to scoped SSM storage on successful connect.

## 2026-03-07 — TKT-0265 persistence decision for current SSM shape
- Do not bump `HostDatabase.DB_VERSION` for this slice: current SSM host metadata already fits existing host columns.
- Treat backup compatibility as an additive encrypted-backup JSON change instead:
  - export/import `saved_scoped_secrets` for persisted SSM scoped secrets
  - include SSM target (`postlogin`) in restore host matching for SSM entries
- Keep backup markers non-secret and include SSM-specific counts only.

## 2026-03-07 — TKT-0265 accepted after operator backup/import smoke
- Operator confirmed encrypted backup export/import restores the SSM host target and saved secret without re-entry.
- Keep the same access key/region with multiple distinct SSM targets as a later validation-matrix smoke, not a blocker for this ticket close.
- Return active sequencing focus to `TKT-0263` Slice B/C once PM/BA and UX decisions are available.

## 2026-03-07 — TKT-0263 PM/BA sequencing resolved
- Do not start with assume-role or multi-account jump UI.
- Ship temporary session credentials first on the existing SSM host flow:
  - detect temporary access keys by `ASIA...`
  - prompt for session token when needed
  - persist session token only in scoped secure storage when `remember password` is enabled
- Keep phase-1 MFA as external session-token issuance, not in-app MFA prompt UX.
- Future role/account-jump UX will use a mixed model: per-host direct session fields stay in the SSM editor, while multi-account role/jump chains move to a shared AWS profile abstraction in follow-up work.

## 2026-03-07 — TKT-0266 assume-role baseline extracted from broader credential scope
- Keep the first app-driven role flow intentionally small:
  - one optional SSM-only per-host role ARN field
  - one STS `AssumeRole` hop from base AWS credentials
  - no shared profile manager, role chain graph, or in-app MFA UX yet
- Persist only the operator-supplied base credentials; never persist assumed-role temporary credentials returned by STS.
- Log only non-secret booleans/categories for the role path (`assume_role_configured`, `credential_enhanced`).

## 2026-03-07 — TKT-0266 accepted after operator assume-role smoke
- Operator confirmed the app-driven assume-role path works on device and explicitly approved marking `TKT-0266` done.
- Invalid/denied role-path surfacing remains a residual validation case, but it is not keeping the baseline ticket open after successful happy-path smoke.

## 2026-03-08 — SSM jump/bastion model corrected after AWS docs review
- Do not model one SSM host as another host's credential source or “jump host” abstraction.
- For private-resource access behind a managed node, the real AWS pattern is Session Manager tunneling:
  - `AWS-StartPortForwardingSession`
  - `AWS-StartPortForwardingSessionToRemoteHost`
- For private SSH hosts behind a managed node, run SSH over the SSM tunnel rather than treating the managed node as a second logical host in the credential model.
- Withdraw the unsmoked `AWS source host` prototype from code and board state, and restore the verified TKT-0266 baseline.

## 2026-03-08 — TKT-0267 / TKT-0268 decomposition for real SSM bastion scenarios
- `TKT-0267` now covers Session Manager port forwarding and remote-host tunnel baseline.
- `TKT-0268` depends on `TKT-0267` and covers SSH-over-SSM integration for private hosts behind the managed node.
- Keep AWS account/profile/MFA work separate from the tunnel transport model so credential concerns do not leak into host/jump UX again.

## 2026-03-08 — TKT-0267 Slice 1 uses existing port-forward UI with SSM local-only guardrails
- Reuse the existing saved port-forward model and `PortForwardListActivity` instead of introducing a second SSM-specific tunnel UI in this slice.
- Limit SSM hosts to `local` forwards only for now:
  - destination `localhost:port` means a managed-node port forward
  - destination `host:port` means a remote-host tunnel reachable from the managed node
- Leave SSH-over-SSM host routing and editor UX to `TKT-0268` so `TKT-0267` stays transport-focused and reviewable.

## 2026-03-08 — TKT-0267 tunnel sessions must keep stderr out of forwarded socket payloads
- First operator smoke of the managed-node SSH test (`15432 -> localhost:22`, then SSH to `127.0.0.1:15432`) failed with SSH key-exchange corruption (`Illegal packet size`).
- Treat Session Manager `PAYLOAD_STDERR` as a separate channel:
  - shell sessions may still render stderr in the terminal
  - port-forward sessions must never inject stderr bytes into the forwarded TCP socket
- Keep tunnel stderr observability as non-secret markers only, so binary protocols (SSH, DB) are not corrupted by diagnostic/control text.

## 2026-03-08 — TKT-0267 modern local forwarding must follow AWS `smux` behavior
- Official AWS Session Manager plugin review showed that `LocalPortForwarding` uses `MuxPortForwarding` backed by `xtaci/smux` when the target SSM Agent version is greater than `3.0.196.0`; raw/basic forwarding is only the older-agent fallback.
- Treat modern-agent local port-forward sessions as a framed `smux` byte stream over the SSM data channel, not as direct TCP payloads.
- Capture `AgentVersion` from the Session Manager handshake and select protocol mode per connection:
  - `raw` for older agents
  - `smux_v1` for modern agents
- Surface SSM flag payload `ConnectToPortError` as a tunnel failure instead of letting it look like opaque SSH corruption.

## 2026-03-08 — TKT-0267 mux selection must also use handshake session metadata
- Operator report from the `15:22` build showed the device reached `smux_v1` on agent `3.3.2299.0`, but the first inbound bytes still were not a valid mux header (`Unexpected SSM smux version 83`).
- Do not choose mux mode from agent version alone.
- Capture `SessionType` and nested session `Properties.Type` from the SSM handshake, and only enable the client-side mux bridge when the port-session type is explicitly `LocalPortForwarding`.
- For newer agents, tolerate one printable preamble line before the first valid mux frame and accept inbound mux control markers beyond the minimal v1 subset, so the tunnel does not abort on startup text/control frames.

## 2026-03-08 — Prioritize background/power-management resilience before SSH-over-SSM UX
- Operator smoke now confirms `TKT-0267` baseline success for both managed-node SSH tunneling and remote-host/private-DB forwarding.
- A new operator issue was observed immediately after that success: active sessions close when the app backgrounds.
- Prioritize `TKT-0269` ahead of `TKT-0268` because session longevity under background/power-management conditions is a more fundamental reliability requirement than adding more SSH-over-SSM UX on top.

## 2026-03-08 — PM/BA re-prioritization: keep focus on SSM work, then handle background resilience
- Operator clarified the priority concern is keeping focus on SSM work, not shifting attention away from the SSM epic.
- Re-prioritize `TKT-0268` ahead of `TKT-0269` because it is the next operator-facing SSM workflow completion step:
  - SSH host selects an existing SSM host as its route/tunnel host
  - the app auto-creates and tears down the localhost tunnel
  - the operator should not need manual SSM port-forward setup for SSH-over-SSM
- Keep `TKT-0269` visible immediately behind it as the next reliability hardening slice.

## 2026-03-08 — TKT-0268 keeps SSH jump hosts and SSM route hosts separate, with explicit ordering
- `TKT-0268` must not overload SSH `Jump via host` with Session Manager semantics.
- Keep two distinct SSH-only controls:
  - `Jump via host` for SSH `ProxyJump`
  - `Route via SSM host` for Session Manager tunneling
- When both are configured, route the SSH jump host through SSM first and then apply the existing SSH jump-host forward to the final target.
- Decision captured in ADR `ai_docs/docs/adr/0002_ssh_over_ssm_routing_order.md`.

## 2026-03-08 — TKT-0269 re-enters with observability before more lifecycle changes
- The operator confirmed the `17:53` diagnostic launcher build opens again, so the active unresolved problem is the original background session drop, not app startup.
- Do not ship another power-management or lifecycle behavior change until a reproduced failure is captured with stronger exported debug markers.
- The next `TKT-0269` slice is observability-only:
  - `ConsoleActivity`, `TerminalManager`, and `ConnectivityReceiver` lifecycle markers
  - explicit SSH/SSM close/disconnect-dispatch markers
- Reason: this isolates whether the initiating layer is activity backgrounding, service teardown, connectivity change, reconnect handling, or transport close, while avoiding another startup regression.

## 2026-03-09 — Exported report proves service teardown first; next slice must measure foreground-service state
- The `2026-03-08 21:24` exported report shows `TerminalManager` `service_destroy bridges=3 pending_reconnect=0` before any SSH/SSM close markers or connectivity-ref cleanup.
- Therefore do not spend the next slice on SSH keepalive or SSM transport changes. The current failure boundary is service/lifecycle state.
- The report still does not prove whether in-app `Persist connections` was enabled at failure time, so add that state to exported logs before attempting another broader lifecycle rewrite.
- The next `TKT-0269` follow-up should:
  - reassert running-notification state whenever binding/start-command/open/disconnect/persist-setting changes occur
  - log `running_notification_state reason=<...> persist=<...> active_network=<...>`
  - use the next operator report to decide whether the root cause is lost foreground state or a separate OS kill path

## 2026-03-09 — Second report proves persisted active sessions reach unbind; next slice must harden the foreground notifier path
- The `2026-03-09 11:44` exported report shows `running_notification_state reason=unbind persist=true active_network=true bridges=3`, so the failure is no longer explained by the user having `Persist connections` off.
- After that marker, the report shows no graceful `service_destroy`, no transport close cascade, and then a cold `APP_CREATED` with a fresh `service_create bridges=0`.
- Treat the next slice as a foreground-service compliance and notifier-observability problem, not an SSH/SSM transport problem.
- Ship three narrow changes together:
  - declare `TerminalManager` as a typed `specialUse` foreground service for API 34+
  - use typed `startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` and log notifier failures explicitly
  - log `task_removed`, `trim_memory`, and `low_memory` so the next report can distinguish task dismissal, memory pressure, and notifier-path failure

## 2026-03-09 — Third report shows the process dies before the first `show_running` marker
- The `2026-03-09 12:27` exported report still shows `unbind persist=true active_network=true`, but no `NOTIFIER_FLOW: show_running...` line and no `show_running_failed=...` line before the later cold `APP_CREATED`.
- Therefore the next failure boundary is earlier than typed `startForeground()` itself.
- Treat the next slice as notification-construction hardening:
  - log `show_running_prepare` before building the running notification
  - switch notifier `PendingIntent`s to explicit immutable flags for modern Android
  - keep the typed FGS declaration and notifier failure logging in place so the next report can separate notification-build failure from `startForeground()` failure

## 2026-03-09 — TKT-0269 closes on notification-construction hardening, not transport changes
- The operator confirmed the `2026-03-09 12:30` build keeps sessions alive across background/foreground again.
- The final exported report `termbot-report-20260309-125321.txt` shows the previously missing notifier path now executing on background `unbind` and later `rebind`:
  - `NOTIFIER_FLOW: show_running_prepare`
  - `NOTIFIER_FLOW: show_running type=special_use`
- There is no cold `APP_CREATED` restart after those background transitions, so the root cause for the observed device failure was in the notification construction / foreground-promotion path rather than SSH or SSM transport behavior.
- Close `TKT-0269` and keep future lifecycle work focused on regression avoidance, not further speculative power-management rewrites.

## 2026-03-09 — TKT-0263 MFA support must follow STS `GetSessionToken` / `AssumeRole`, not a custom SSM flow
- The operator asked specifically for realistic MFA support when AWS access keys or role assumption require MFA.
- AWS documentation review fixed the implementation model:
  - same-account/direct SSM with long-lived IAM-user credentials -> `GetSessionToken`
  - role-based SSM -> `AssumeRole`, optionally with `SerialNumber` + `TokenCode`
- Do not attempt automatic MFA-device discovery in this slice because IAM `ListMFADevices` permissions cannot be assumed for all callers.
- Ship an SSM-only optional MFA serial/ARN field, runtime MFA-code prompts, and marker-only logging for `enhancement_mode`, while leaving YubiKey/WebAuthn MFA UX out of scope for now.

## 2026-03-09 — TKT-0263 closes with STS-based MFA, while FIDO2/passkey MFA remains backlog
- Operator smoke confirmed the shipped STS-based MFA slice works for:
  - direct SSM MFA
  - assume-role MFA
  - invalid MFA-code failure surfacing
- Keep the current ticket closed on the STS model.
- Any future YubiKey-related MFA follow-up must distinguish:
  - YubiKey as a FIDO2/passkey device: blocked by AWS STS/API limitations
  - YubiKey or app as an RFC6238 TOTP source: potentially feasible as a separate follow-up

## 2026-03-09 — Publish root operator docs before RC sign-off
- The workspace root documentation must summarize the shipped product capabilities, not only the engineering process, so operators can understand what the current build actually supports.
- Add a root `MANUAL.md` as the primary usage guide for the already shipped flows:
  - direct SSH
  - `Jump via host`
  - direct SSM shell
  - SSM role ARN
  - SSM MFA
  - SSM port forwarding
  - `Route via SSM host`
  - combined `Route via SSM host` + `Jump via host`
- Refresh `ai_docs/docs/RELEASE.md` so the RC smoke gate includes both the historical security-key matrix and the current SSM operator flows.
