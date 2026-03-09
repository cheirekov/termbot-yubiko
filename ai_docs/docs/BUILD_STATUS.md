# Build Status (Short)

Keep this file short. Link to full logs in references/logs/.

## Latest build (TKT-0263 Slice C real AWS MFA support — 2026-03-09)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 1m 0s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-09T13-44-07+02-00.log`
- Changes under verification:
  - Added an SSM-only optional MFA device serial/ARN field in the host model, DB schema (`DB_VERSION=32`), encrypted backup/import, and host editor.
  - `SSM` now follows official AWS STS MFA flows:
    - direct SSM with long-lived access keys + configured MFA serial -> `GetSessionToken`
    - role-based SSM + configured MFA serial -> `AssumeRole` with `SerialNumber` + `TokenCode`
    - role-based SSM without MFA serial keeps the existing assume-role path
  - `SsmCredentialResolver` now logs non-secret `enhancement_mode` markers (`none` / `get_session_token` / `assume_role`) plus `mfa_prompted`.
  - Operator closeout:
    - operator confirmed direct SSM MFA
    - operator confirmed assume-role MFA
    - operator confirmed invalid MFA code failure surfacing
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-09T13-44-07+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-09T13-44-07+02-00.apk`

## Prior build (TKT-0269 notifier-construction follow-up — 2026-03-09)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 36s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-09T12-30-24+02-00.log`
- Changes under verification:
  - The `2026-03-09 12:27` exported report still showed cold restarts after `running_notification_state reason=unbind persist=true active_network=true`, but there was still no `NOTIFIER_FLOW: show_running...` marker at all.
  - That means the failure boundary is earlier than the typed `startForeground()` call itself.
  - `ConnectionNotifier` now:
    - logs `show_running_prepare` before building the running notification
    - uses immutable `PendingIntent` flags for notification content/action intents
    - still logs `show_running`, `show_running_failed`, and `hide_running`
  - Goal of this build:
    - determine whether notification construction itself was crashing on modern Android
    - make the next report unambiguous about whether the app reaches the actual typed foreground-promotion call
  - Operator closeout:
    - operator confirmed background/foreground now works and live connections remain available
    - final exported report `termbot-report-20260309-125321.txt` shows `show_running_prepare` + `show_running type=special_use` on background `unbind`/rebind, with no cold `APP_CREATED` restart afterward
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-09T12-30-24+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-09T12-30-24+02-00.apk`

## Prior build (TKT-0269 typed foreground-service follow-up — 2026-03-09)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 48s` (62 actionable tasks: 25 executed, 37 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-09T11-52-25+02-00.log`
- Changes under verification:
  - The `2026-03-09 11:44` exported report proved the prior `11:21` build already had `persist=true` and `active_network=true` at `unbind`, but the app still later cold-started with `bridges=0`.
  - `TerminalManager` is now declared as an explicit typed foreground service:
    - permission: `FOREGROUND_SERVICE_SPECIAL_USE`
    - service type: `specialUse`
    - manifest subtype text documents SSH / SSM / tunnel usage
  - `ConnectionNotifier` now:
    - uses typed `startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` on API 34+
    - logs `NOTIFIER_FLOW` show/hide attempts
    - logs notifier failures instead of letting them fail silently in exported reports
    - falls back to posting the notification if typed foreground promotion throws
  - `TerminalManager` now logs `task_removed`, `trim_memory`, and `low_memory` markers so the next report can distinguish explicit task removal from process pressure or notifier failure.
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-09T11-52-25+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-09T11-52-25+02-00.apk`

## Prior build (TKT-0269 service-boundary follow-up — 2026-03-09)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 54s` (62 actionable tasks: 18 executed, 44 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-09T11-21-56+02-00.log`
- Changes under verification:
  - The exported report from `2026-03-08 21:24` proved `TerminalManager` service teardown is the initiating event for the background drop: `service_destroy bridges=3 pending_reconnect=0` occurs before SSH/SSM close markers and before connectivity ref cleanup.
  - `TerminalManager` now reasserts running-notification / foreground state at bind, rebind, start-command, unbind, connection-open, disconnect, and `Persist connections` preference changes.
  - Added non-secret `running_notification_state` markers with:
    - `reason=<...>`
    - `persist=<true|false>`
    - `active_network=<true|false>`
  - Goal of this build:
    - verify whether the app was silently losing foreground-service state
    - explicitly confirm whether `Persist connections` is enabled on the failing device before another behavioral change is attempted
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-09T11-21-56+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-09T11-21-56+02-00.apk`

## Prior build (TKT-0269 observability diagnosis — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 53s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T21-03-19+02-00.log`
- Changes under verification:
  - This build is observability-only. It does not attempt another background-resilience behavior change.
  - Added non-secret exported-debug-report markers for:
    - `ConsoleActivity` lifecycle and bridge disconnect callbacks
    - `TerminalManager` service create/destroy, open/disconnect, connectivity-lost/restored, and reconnect-queue activity
    - `ConnectivityReceiver` network-ref / Wi-Fi-lock transitions and connectivity broadcasts
    - explicit SSH and SSM transport close / disconnect-dispatch markers
  - Operator-reported launcher regression is no longer the active issue; the next goal is capturing a real background-drop report on device.
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-08T21-03-19+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T21-03-19+02-00.apk`

## Prior build (TKT-0269 launcher diagnostic recovery — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 37s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T17-53-14+02-00.log`
- Changes under verification:
  - `ConnectBotApplication` now treats app-theme and debug-log bootstrap failures as non-fatal and logs them instead of aborting process startup.
  - `HostListActivity` now catches startup-stage exceptions in debug builds and renders the stack trace on screen instead of immediately autoclosing, so the operator can capture the real failure without `adb`.
  - This build is diagnostic only. It is not a claimed `TKT-0269` fix; it exists to isolate the launcher crash that persisted even after the `17:36` rollback recovery.
  - Packaged APKs:
    - `references/builds/termbot-oss-debug-2026-03-08T17-53-14+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T17-53-14+02-00.apk`

## Latest build (TKT-0269 rollback recovery — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 40s` (62 actionable tasks: 17 executed, 4 from cache, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T17-36-24+02-00.log`
- Changes under verification:
  - Rolled back the initial `TKT-0269` Slice A lifecycle/power-management changes after the operator reported that the app no longer opened from the launcher and immediately autoclosed on device.
  - Recovery APKs restore the pre-`0269` stable baseline while preserving the completed `TKT-0268` SSM/SSH routing work:
    - `references/builds/termbot-oss-debug-2026-03-08T17-36-24+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T17-36-24+02-00.apk`
  - `TKT-0269` is returned to `Ready` for safer rework after startup stability is reconfirmed.

## Latest build (TKT-0269 Slice A attempted background-session protection — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 40s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T17-24-04+02-00.log`
- Outcome:
  - Build was green, but operator reported app-startup autoclose on device immediately after install.
  - That slice is not shipping and was rolled back in the `17:36` recovery build above.

## Latest build (TKT-0268 Slice A route-via-SSM integration — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 43s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T16-54-45+02-00.log`
- Changes under verification:
  - Added additive host schema + backup/import support for SSH `ssm_route_host_id`.
  - SSH host editor now exposes a separate `Route via SSM host` selector without changing the existing SSH `Jump via host` meaning.
  - SSH transport now auto-opens an SSM tunnel for the SSH session instead of requiring manual SSM port forwards, and closes that tunnel automatically when the SSH session ends.
  - When both `Route via SSM host` and `Jump via host` are configured, the routing order follows ADR `0002`: SSM tunnel to the SSH jump host first, then the existing SSH jump-forward path to the final target.
  - Packaged APKs for operator smoke:
    - `references/builds/termbot-oss-debug-2026-03-08T16-54-45+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T16-54-45+02-00.apk`
  - Manual device smoke confirmed:
    - direct SSH via selected SSM route host
    - direct SSH regression
    - direct SSM shell regression
    - SSH jump-host regression
  - Remaining follow-up outside this ticket:
    - background/power-management session drops tracked in `TKT-0269`

## Latest build (TKT-0267 smux gating + preamble tolerance follow-up — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 29s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T15-47-04+02-00.log`
- Changes under verification:
  - Operator report on the `15:22` build proved the device was on the new `smux_v1` branch (`agent_version=3.3.2299.0`) but the first inbound bytes were still not a valid frame header.
  - `SsmStreamClient` now captures `SessionType` plus nested session `Properties.Type` from the SSM handshake so the transport can gate mux selection on the actual port-session mode instead of agent version alone.
  - `SSM` now chooses `smux_v1` only for explicit `LocalPortForwarding` sessions on newer agents, keeps raw fallback for non-mux paths, tolerates one printable preamble line before the first `smux` frame, and accepts newer-agent inbound mux control markers.
  - Operator smoke now confirms both:
    - managed-node-local SSH tunnel (`15432 -> localhost:22`, then SSH to `127.0.0.1:15432`)
    - remote-host/private-DB forwarding via localhost client access after forwarding to the DB host/port
  - Separate operator issue observed after smoke: active sessions close when the app backgrounds; tracked in `TKT-0269`.
  - Packaged APKs for operator re-smoke:
    - `references/builds/termbot-oss-debug-2026-03-08T15-47-04+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T15-47-04+02-00.apk`

## Latest build (TKT-0267 modern-agent smux port-forward follow-up — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 30s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T15-22-04+02-00.log`
- Changes under verification:
  - Second diagnosis used the official AWS Session Manager plugin source and confirmed that modern local port-forward sessions use `smux`, not raw TCP bytes over the SSM data stream.
  - `SsmStreamClient` now captures `AgentVersion` from the handshake and exposes flag payloads to the transport layer.
  - `SSM` tunnel sessions now select `smux_v1` for modern agents (`> 3.0.196.0`) and raw forwarding only for older-agent fallback.
  - Added flag handling for `ConnectToPortError` and packaged new APKs for operator re-smoke:
    - `references/builds/termbot-oss-debug-2026-03-08T15-22-04+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T15-22-04+02-00.apk`

## Latest build (TKT-0267 tunnel stream-fix follow-up — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 24s` (62 actionable tasks: 18 executed, 44 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T15-04-35+02-00.log`
- Changes under verification:
  - First operator smoke of the SSM local-forward path reached the tunnel but SSH key exchange failed with `Illegal packet size`, indicating stream corruption rather than bad smoke setup.
  - `SsmStreamClient` now routes Session Manager `stderr` separately from `stdout` instead of injecting both into the forwarded TCP socket.
  - `SSM` shell sessions still render stderr in the terminal, while SSM tunnel sessions now ignore stderr for socket payloads and trace only non-secret tunnel stderr markers.
  - New timestamped APKs were packaged for re-smoke:
    - `references/builds/termbot-oss-debug-2026-03-08T15-04-35+02-00.apk`
    - `references/builds/termbot-google-debug-2026-03-08T15-04-35+02-00.apk`

## Latest build (TKT-0267 Slice 1 tunnel baseline — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 39s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T12-17-01+02-00.log`
- Changes under verification:
  - `TKT-0267` Slice 1 added document-based SSM `StartSession` support for `AWS-StartPortForwardingSession` and `AWS-StartPortForwardingSessionToRemoteHost`.
  - `SSM` transport now supports local port forwards via Session Manager tunnel sessions while keeping existing direct shell sessions unchanged.
  - `PortForwardListActivity` now exposes only `Local` forwards for SSM hosts, with destination `host:port` interpreted from the managed node network view.
  - Added non-secret tunnel lifecycle markers and visible tunnel failure surfacing in the SSM console.

## Latest build (SSM roadmap correction cleanup — 2026-03-08)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 42s` (62 actionable tasks: 17 executed, 4 from cache, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-08T11-47-05+02-00.log`
- Changes under verification:
  - Withdrew the unsmoked `AWS source host` prototype after AWS docs review showed the real next scenario is Session Manager tunneling, not host-to-host credential reuse.
  - Removed the source-host editor, runtime, schema, and backup remnants so the app is back on the verified TKT-0266 assume-role baseline.
  - Re-scoped `TKT-0267` / `TKT-0268` in docs toward Session Manager port forwarding and SSH-over-SSM bastion flows.

## Latest build (TKT-0266 assume-role baseline — 2026-03-07)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 44s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-07T16-40-42+02-00.log`
- Changes under verification:
  - Added optional SSM-only per-host `Assume role ARN` field with host DB migration (`DB_VERSION=30`) and encrypted backup import/export support.
  - Added STS `AssumeRole` client and SigV4 form-post signing for runtime credential elevation before SSM `StartSession`.
  - Resolver now keeps base credentials for persistence while using assumed-role credentials only for the live SSM session.
  - Added non-secret markers for `assume_role_configured` and `credential_enhanced`.
- Manual device smoke (operator, 2026-03-07):
  - Base AWS credentials + role ARN connect path confirmed; operator approved closing the ticket and continuing.

## Latest build (TKT-0263 Slice B temporary session credentials — 2026-03-07)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 43s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-07T15-48-07+02-00.log`
- Changes under verification:
  - Added temporary session-credential support for SSM hosts by prompting for an AWS session token when the access key ID indicates session credentials (`ASIA...`) and no saved token exists.
  - Reused scoped secret storage so saved SSM credentials can now include session tokens.
  - Added non-secret `session_token_source` marker alongside existing credential mode/source markers.

## Latest build (TKT-0265 backup/export compatibility — 2026-03-07)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 28s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-07T15-27-09+02-00.log`
- Changes under verification:
  - Added additive backup JSON support for persisted SSM scoped secrets.
  - Added SSM target-aware host matching during import to avoid restore collisions between different SSM targets in the same region.
  - Added non-secret backup markers for `ssm_hosts` and `scoped_secrets`.
  - No DB schema version bump required for current SSM host shape.
- Manual device smoke (operator, 2026-03-07):
  - Encrypted backup export/import restored the SSM host target and saved secret without re-entry.

## Latest build (TKT-0263 Slice A credential foundation — 2026-03-07)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 38s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-07T15-13-08+02-00.log`
- Changes under verification:
  - Added `SsmCredentialResolver` contract for SSM credential resolution and persistence.
  - Added scoped secret storage in `SavedPasswordStore` for SSM secret/session-token slots with legacy-read compatibility.
  - Updated `SSM` transport to log credential mode/source markers only (no secret values).
  - Updated `EditHostActivity` forget/clear paths to remove SSM scoped secrets.

## Latest build (TKT-0264 strict SSM-only target visibility — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 37s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T19-33-08+02-00.log`
- Changes under verification:
  - Added `postlogin_section_container` and made SSM target block visible only for SSM protocol.
  - Hidden target block for non-SSM protocols as requested by operator.

## Latest build (TKT-0264 SSM target placement UX — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 32s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T17-58-42+02-00.log`
- Changes under verification:
  - Moved `SSM target` editor into expanded URI section near AWS access key/region/port.
  - Kept same underlying `postLogin` model field to preserve existing save/connect behavior.

## Latest build (TKT-0264 quick-connect typing fix — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 24s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T17-48-48+02-00.log`
- Changes under verification:
  - Fixed SSM quick-connect top-field overwrite while typing full compact address (`AKIA...@region/i-target`).
  - Wrapped quick-connect parse/sync in one guarded edit transaction to prevent recursive `setText` feedback from SSM target watcher.
  - Invalid SSM quick-connect input now also clears stale target field state.

## Latest build (TKT-0264 prompt lifecycle fix — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 26s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T17-30-56+02-00.log`
- Changes under verification:
  - Fixed SSM first-connect secret-key prompt race by surfacing already-pending prompts when UI handler attaches (`PromptHelper.setHandler`).
  - Expected UX: first host open now prompts immediately for secret key instead of requiring back-navigation/re-entry.

## Latest build (TKT-0264 host-editor parse/validation fix — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 23s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T17-26-52+02-00.log`
- Changes under verification:
  - TKT-0264 first host-editor bug fix: SSM quick-connect now maps access-key-only input (`AKI...`) to AWS Access Key ID field instead of Region.
  - Added SSM save-enable gate in host editor requiring access key ID + region + target.
  - No transport regressions observed in compile/build checks.

## Latest build (TKT-0262 websocket stream bridge — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 22s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T17-08-52+02-00.log`
- Changes under verification:
  - Added SSM websocket stream bridge (`org.connectbot.aws.SsmStreamClient`) with SigV4-authenticated `ssmmessages` handshake.
  - Added minimal SSM binary frame handling for output/input/ack/channel-close and handshake request/response.
  - Reworked `transport/SSM.java` into real `AbsTransport` flow (`bridge.onConnected()`, terminal `read/write`, resize payload, clean disconnect).
  - Added OkHttp dependency (`com.squareup.okhttp3:okhttp:3.14.9`) for Android-compatible websocket support.
- Manual device smoke (operator, 2026-03-06):
  - SSM connect successful.
  - `uname -a` output received.
  - `whoami` returned `ssm-user`.
  - `exit` disconnected cleanly.

## Latest build (TKT-0262 StartSession bootstrap — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 40s` (62 actionable tasks: 23 executed, 39 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T16-27-55+02-00.log`
- Changes under verification:
  - Added AWS SigV4 + SSM StartSession API client (`org.connectbot.aws` package).
  - Wired `SSM` transport to use access key ID (username), region (host), target (post-login field), and secret key (prompt/saved password store).
  - Added HostEditor SSM protocol support and SSM-specific field labels/hints.
  - Added user-facing SSM status/error strings and kept stream bridge explicitly pending.

## Latest build (TKT-0261/TKT-0262 kickoff — 2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 26s` (62 actionable tasks: 17 executed, 45 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T16-03-17+02-00.log`
- Changes under verification:
  - Added SSM transport kickoff scaffolding (`transport/SSM.java`) with safe placeholder lifecycle and no UI exposure yet.
  - Added `TransportFactory` handling for `ssm` scheme without adding it to user-selectable protocol list.
- Notes:
  - First attempt from sandbox failed due Docker socket permission; rerun with elevated permissions succeeded.

## Latest build (2026-03-06)
- Task: `assembleDebug`
- Result: `BUILD SUCCESSFUL in 3m 47s` (62 actionable tasks: 11 executed, 51 up-to-date)
- Command (canonical Docker-first path used): `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=/home/yc/work/ai-projects-templates/termbot-yubiko/references/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Log: `references/logs/android_build_2026-03-06T15-27-16+02-00.log`
- APK artifacts:
  - `repos/termbot-termbot/app/build/outputs/apk/oss/debug/app-oss-debug.apk` (12,441,825 bytes)
  - `repos/termbot-termbot/app/build/outputs/apk/google/debug/app-google-debug.apk` (8,817,974 bytes)
- Notes: a prior attempt at `references/logs/android_build_2026-03-06T15-24-21+02-00.log` failed due to Gradle cache lock contention (`fileHashes.lock`), then rerun succeeded using isolated Gradle cache path.

## Latest build (TKT-0244 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: FIDO2 USB parity fix; FIDO2 auth bridge (`Fido2SecurityKeyAuthenticatorBridge`) and FIDO2 key import flow (`PubkeyListActivity`) now open CTAP2 over `FidoConnection` first with `SmartCardConnection` fallback, so USB and NFC both work for FIDO2
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T17-30-28+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0243 desktop input+touch hardening — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 31s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: Added Desktop Mode auth prompt hardening in `SecurityKeyActivity` (PIN dialog supports hardware keyboard Enter submit / Escape cancel) and explicit post-PIN wait/touch UI for PIV/FIDO2 flows to better handle touch-policy keys on USB
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T16-58-49+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0243 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: Desktop Mode Manage Keys fix; `PubkeyListActivity` now uses a desktop-safe add-key actions dialog (generate/import/OpenPGP/PIV/FIDO2/setup) for wide windows while keeping existing bottom-sheet flow for narrow/mobile layouts
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T16-13-07+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0242 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 25 executed, 37 up-to-date)
- Changes: P0 USB YubiKey reliability hardening; moved USB/NFC discovery to lifecycle-gated start/stop in `SecurityKeyActivity`, delayed OpenPGP discovery until PIN is collected (race fix for pre-attached USB key), added USB/NFC discovery flow markers including source (`usb`/`nfc`), and added non-required `android.hardware.usb.host` manifest feature declaration
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-58-25+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0225 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 24s (62 actionable tasks: 9 executed, 53 up-to-date)
- Changes: host grouping UX refinement; group headers are always visible (including empty/new groups), headers are expandable/collapsible, and host list now includes synthetic `Ungrouped` and `All` sections with per-group host counts
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-36-57+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0241 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 26s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: P0 jump-host auth hotfix hardening; in addition to restoring auth flags around jump auth, target `authenticate()` now logs `SSH_AUTH_CONTEXT`, forces a publickey attempt for jump-host targets when method advertise is inconsistent, and includes security-key fallback for `PUBKEYID_ANY` to avoid direct password fallback after jump-forward setup
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T15-18-18+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0225 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 38s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0225 host grouping/folders; added host-group table+host `groupid`, editor group assignment, host-list grouped headers + create/rename/delete group actions, and encrypted-backup groups export/import remap
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T14-35-43+02-00.log`
- Errors: 0 | Warnings: existing lint/deprecation/errorprone warnings

## Latest build (TKT-0240 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 36s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0240 UI contrast hotfix; switched app theme to AppCompat DayNight, added `values-night` terminal-friendly palette, fixed v11 list text color override, added popup menu background theming, and added Settings app theme selector (System/Light/Dark)
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T14-05-28+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0234 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 34s (62 actionable tasks: 27 executed, 35 up-to-date)
- Changes: TKT-0234 app icon rebrand; launcher assets replaced (legacy + adaptive + monochrome), manifest switched to `@mipmap/icon` / `@mipmap/icon_round`, and store draft package generated
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-49-59+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0238 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 35s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0238 theme token/component refresh; new color + spacing/type tokens, tokenized list row typography/spacing, dialog text contrast, bottom-sheet surface/icon/text styling, and host-list FAB tokenized action colors
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-39-44+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0239 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 36s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0239 backup UX hardening; backup password dialogs now keep open with inline validation, and export/import now run with duplicate-operation lock + visible non-cancelable progress dialog + non-secret backup operation flow markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T13-11-59+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Latest build (TKT-0237 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 32s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0237 import UX parity; OpenPGP/PIV/FIDO2 import flows now use persistent cancelable waiting dialog in `PubkeyListActivity`, with non-secret wait-state markers and OpenPGP import lifecycle markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T12-51-16+02-00.log`
- Errors: 0 | Warnings: existing deprecation/lint warnings

## Note
- A default-image run (`ghcr.io/cirruslabs/android-sdk:34`) failed before the successful build because it attempted NDK auto-install into a read-only SDK path (`/opt/android-sdk-linux`).
- Failed log: `android_build_2026-03-04T12-50-54+02-00.log`

## Latest build (TKT-0232 — 2026-03-04)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 42s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0232 PIN input policy consistency; local provider-aware PIN validation in `SecurityKeyActivity` (OpenPGP digits+min6, PIV 6-8 chars, FIDO2 optional+min4 when provided), with non-secret reject marker `SK_ACTIVITY_PIN_LOCAL_FORMAT_REJECTED`
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-04T11-51-57+02-00.log`
- Errors: 0 | Warnings: deprecation + existing lint warnings

## Previous build (TKT-0236 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 34s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0236 security-key waiting UI upgrade; replaced OpenPGP/PIV/FIDO2 wait toasts with persistent dialog (large key icon + NFC/USB guidance) and added wait UI debug markers
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T18-04-42+02-00.log`
- Errors: 0 | Warnings: deprecation + existing lint warnings

## Previous build (TKT-0231 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 22s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0231 OpenPGP auth fix for APDU 0x6982 by switching OpenPGP PIN verify mode to PW1 0x82 before INTERNAL AUTHENTICATE
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-49-23+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0230 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 37s (62 actionable tasks: 21 executed, 41 up-to-date)
- Changes: TKT-0230 OpenPGP prompt/fail-fast fix + Manage Keys link cleanup; OpenPGP PIN prompt text corrected, OpenPGP auth errors are non-retryable (no hang loop), `hwsecurity.dev` links replaced by Yubico URL/text
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-33-48+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0229 — 2026-03-03)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 31s (62 actionable tasks: 17 executed, 45 up-to-date)
- Changes: TKT-0229 connection-lifecycle fix; auth/import now open fresh `SmartCardConnection` from `YubiKeyDevice` per operation (OpenPGP/PIV/FIDO2) instead of reusing callback-scoped connections
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `android_build_2026-03-03T16-14-29+02-00.log`
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0228 — 2026-03-02)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL (log: android_build_2026-03-02T21-40-14+02-00.log)
- Changes: OpenPGP NFC two-tap flow (pre-tap PIN); PIV/FIDO2 NFC race fix (removed new Thread() wrapping); `security_key_openpgp_tap_now` string added
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0222 — 2026-03-02)
- Task: assembleDebug
- Result: BUILD SUCCESSFUL in 35s (62 tasks: 21 executed, 41 up-to-date)
- Changes: OpenPGP import stub replaced with `session.getPublicKey(KeyRef.AUT).toPublicKey()`; Ed25519 algorithm name normalization; `pubkey_add_openpgp_success` string resource added
- Errors: 0 | Warnings: deprecation only

## Previous build (TKT-0227 — 2026-03-02)
- Date: 2026-03-02
- Repo: `./repos/termbot-termbot`
- Build command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Docker image: `termbot-android-sdk34-jdk11-agp422:local`
- Result: PASS — `BUILD SUCCESSFUL in 47s` (62 tasks: 19 executed, 43 up-to-date)
- Notes: TKT-0227 complete. hwsecurity SDK fully replaced with YubiKit. Zero de.cotech references.
- Log file: `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T19-31-41+02-00.log`

## Previous builds
- 2026-03-02 TKT-0221: debug log PIN/APDU redaction — PASS (26s) — log: `android_build_2026-03-02T18-34-50+02-00.log`
- 2026-03-02 Phase D: appcompat/constraintlayout/material bumped — PASS (58s) — log: `android_build_2026-03-02T18-24-21+02-00.log`
- 2026-03-02 Phase C: compileSdk/targetSdk 29→34, aapt2 override, manifest exported fix — PASS (11s) — log: `android_build_2026-03-02T18-19-19+02-00.log`
- 2026-03-02 Phase B: AGP 4.0.1→4.2.2, Gradle 6.1.1→6.7.1, buildToolsVersion→34.0.0 — PASS (1m 41s) — log: `android_build_2026-03-02T17-46-00+02-00.log`
- 2026-03-02 Phase A: jcenter removal, duplicate google() removal — PASS — log: `android_build_2026-03-02T17-13-02+02-00.log`
