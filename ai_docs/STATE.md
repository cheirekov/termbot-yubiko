# STATE (Authoritative "Now") — KEEP SMALL

Last updated: 2026-03-09 (`TKT-0270` published the RC documentation baseline with a refreshed root `README.md`, root `MANUAL.md`, and updated `ai_docs/docs/RELEASE.md` for the shipped YubiKey + SSM feature set)
Mode: multi-repo
Workspace root: /home/yc/work/ai-projects-templates/workspace
Owners: yc

---

## Project identity (LIMIT=8)
- Product/system: TermBot Android SSH client with YubiKey auth (OpenPGP, PIV, FIDO2)
- Current phase: release-candidate documentation baseline + post-stabilization backlog execution
- Primary users: Android SSH users authenticating with YubiKey
- Success definition (2-6 weeks): stable key auth/import UX + incremental visual modernization + backup reliability
- Runtime: Android app on device (Honor Magic V3 / MagicOS class devices validated by user)
- SDK direction: YubiKit-only for active auth flows (legacy hwsecurity not in runtime path)

## Current objectives (LIMIT=10)
- Keep key auth stable while improving UX in small slices
- Continue AWS SSM epic with credential foundations in place (`TKT-0263`), backup compatibility closed (`TKT-0265`), assume-role baseline closed (`TKT-0266`), Session Manager tunneling baseline closed (`TKT-0267`), SSH-over-SSM routing closed in `TKT-0268`, and background/session resilience now closed in `TKT-0269`
- Keep root operator documentation current for the shipped YubiKey + SSM baseline (`TKT-0270`)
- Execute board priorities in order:
  1) Decide the next backlog slice after the RC docs baseline
  2) TKT-0226 UI/UX improvements epic tail items
  3) Keep release smoke matrix cadence for each RC
- Keep Docker-first build/test flow green after each slice
- Maintain strict docs closeout discipline (board/build/questions/state + closeout_check)

## Repo map (LIMIT=25)
- termbot path=./repos/termbot-termbot role=android_app branch=main sha=de9bd9b2

## Critical constraints (LIMIT=20)
- Never log secrets (PIN/private key/APDU raw payload)
- Must-not-break: working OpenPGP/PIV/FIDO2 auth/import
- Keep diffs small and reversible
- Docker-first; avoid host toolchain installs

## Decisions (ADR index) (LIMIT=15)
- YubiKit runtime path is authoritative for security-key features
- In-app debug report and ring-buffer logs are primary diagnostics channel
- UX work is phased (audit -> focused tickets), not big-bang redesign
- ADR `0002`: SSH `Jump via host` and SSH `Route via SSM host` stay separate; if both are set, SSM routes to the jump host first and SSH jump-forwarding then reaches the final target

## Active work (LIMIT=25)
- DONE 2026-03-05: TKT-0246 root publication bootstrap (user confirmed public repo state with root README/LICENSE and green GitHub Actions)
- DONE 2026-03-05: TKT-0247 GitHub Actions Java/build-tools compatibility fix (user confirmed CI green on current code)
- DONE 2026-03-05: TKT-0245 first public GitHub commit preparation (user confirmed public repo state with current README/LICENSE)
- DONE 2026-03-05: TKT-0244 FIDO2 USB transport parity (user confirmed USB import/auth smoke passing)
- DONE 2026-03-05: TKT-0243 Desktop Mode Manage Keys accessibility (user confirmed Desktop Mode smoke passing)
- DONE 2026-03-05: TKT-0251 security-key regression sentinel matrix (added SK-01..SK-08 manual smoke matrix, failure debug-report capture rule, and baseline dry run record)
- DONE 2026-03-05: TKT-0250 session handoff capsule template/adoption (added 2-minute handoff template + filled example in HANDBOOK)
- DONE 2026-03-05: TKT-0249 review-WIP policy hardening (max Review WIP=2, oldest-first tie-break, transition freeze while Review >2)
- DONE 2026-03-05: TKT-0248 AI process retro + context-window reliability review (added process retro doc with prioritized guardrails and follow-up ticket proposals)
- DONE 2026-03-03: TKT-0231 OpenPGP auth APDU 0x6982 fix
- DONE 2026-03-03: TKT-0236 security-key waiting prompt redesign (persistent touch/USB UI)
- DONE 2026-03-04: TKT-0232 PIN policy consistency (OpenPGP/PIV/FIDO2 local validation + non-secret reject markers)
- DONE 2026-03-04: TKT-0233 professional UI/UX audit + 3 design variants + linked backlog tickets
- DONE 2026-03-04: TKT-0237 security-key import UX parity (persistent cancelable import wait UI + flow markers)
- DONE 2026-03-04: TKT-0239 backup import/export UX hardening (inline validation + operation progress lock/dialog)
- DONE 2026-03-04: TKT-0238 theme token/component refresh (tokenized colors/dimens + list/dialog/bottom-sheet/FAB refresh)
- DONE 2026-03-04: TKT-0234 app icon rebrand + store package (legacy+adaptive launcher refresh, concept set, store draft assets)
- DONE 2026-03-04: TKT-0240 UI contrast regression hotfix + Day/Night theme support (fixed white-on-white screens/menu, added System/Light/Dark theme selector)
- DONE 2026-03-04: TKT-0225 host grouping/folders with backup compatibility (device-confirmed grouping visibility + expandable headers + All/Ungrouped sections)
- DONE 2026-03-04: TKT-0241 P0 jump-host auth state leak fix (device-confirmed jump-host target publickey flow)
- DONE 2026-03-04: TKT-0242 P0 USB YubiKey discovery lifecycle/permission resilience (user confirmed USB flow working on device)
- DONE 2026-03-06: TKT-0261 SSM transport feasibility spike (created TKT-0262..0265, drafted feasibility doc + ADR, captured and resolved initial scope decisions)
- DONE 2026-03-06: TKT-0262 SSM websocket transport (operator smoke confirmed interactive connect, command I/O, and clean exit)
- NEW EPIC 2026-03-06: TKT-0260 AWS SSM Session Manager support
- DONE 2026-03-07: TKT-0264 SSM host editor UX + session UI integration (operator smoke confirmed host-add UX fixes incl. parse, prompt, quick-connect stability, target placement, strict SSM-only visibility)
- DONE 2026-03-09: TKT-0263 AWS credential management + MFA + role/jump readiness (operator confirmed direct SSM MFA, assume-role MFA, and invalid-MFA-code surfacing on the `13:44` build; scope closes on STS `GetSessionToken` / MFA-capable `AssumeRole`)
- DONE 2026-03-09: TKT-0270 release-candidate docs and operator manual baseline (published root `README.md` shipped-capability summary, root `MANUAL.md`, and refreshed `ai_docs/docs/RELEASE.md` so RC smoke scope includes the shipped SSM and SSH routing flows)
- DONE 2026-03-07: TKT-0265 SSM backup/export compatibility + schema migration (operator confirmed encrypted backup export/import restores SSM host target and saved secret; same-key/region multi-target smoke deferred to later matrix)
- DONE 2026-03-07: TKT-0266 SSM assume-role baseline (operator smoke confirmed app-driven assume-role flow with base AWS credentials + role ARN)
- DONE 2026-03-08: TKT-0267 SSM port-forwarding + remote-host tunnel baseline (operator confirmed both managed-node SSH tunnel smoke and remote-host/private-DB forwarding on the 15:47 build)
- DONE 2026-03-08: TKT-0268 SSH over SSM bastion/tunnel integration (operator confirmed direct SSH via selected SSM route host, direct SSH regression, direct SSM shell regression, SSH jump-host regression, and later also confirmed the combined `SSM route host + SSH jump host` topology)
- DONE 2026-03-09: TKT-0269 Background/power-management session resilience (operator confirmed background/foreground survival on the `12:30` build, and the final report shows `show_running_prepare` + `show_running type=special_use` on background `unbind`/rebind with no cold `APP_CREATED` restart)
- CORRECTION 2026-03-08: withdrew the unsmoked `AWS source host` prototype from code and board after AWS docs review showed the real scenario is Session Manager tunneling, not one SSM host acting as another host's credential source
- Backlog (high): TKT-0226 tail items + later SSM MFA/profile follow-ups

## Latest verification (LIMIT=10)
- Latest build: 2026-03-09 `assembleDebug` BUILD SUCCESSFUL in 1m 0s (`TKT-0263` Slice C; SSM hosts now support an optional MFA serial field and use official STS `GetSessionToken` / MFA-capable `AssumeRole` flows)
- Log: `references/logs/android_build_2026-03-09T13-44-07+02-00.log`
- APKs: `references/builds/termbot-oss-debug-2026-03-09T13-44-07+02-00.apk`, `references/builds/termbot-google-debug-2026-03-09T13-44-07+02-00.apk`
- Manual smoke (operator, 2026-03-09): `TKT-0263` passed on the `13:44` build for direct SSM MFA, assume-role MFA, and invalid MFA code surfacing
- Prior build: 2026-03-09 `assembleDebug` BUILD SUCCESSFUL in 36s (`TKT-0269` notifier-construction follow-up; operator now confirms background/foreground survival on this build, and the final report shows notifier promotion on `unbind`/rebind instead of a cold restart)
- Prior log: `references/logs/android_build_2026-03-09T12-30-24+02-00.log`
- Prior APKs: `references/builds/termbot-oss-debug-2026-03-09T12-30-24+02-00.apk`, `references/builds/termbot-google-debug-2026-03-09T12-30-24+02-00.apk`
- Manual smoke (operator, 2026-03-09): `TKT-0269` passed on the `12:30` build; active connections remained available after background/foreground and the exported report `termbot-report-20260309-125321.txt` shows `show_running_prepare` + `show_running type=special_use` on background `unbind`/rebind with no cold `APP_CREATED`
- Prior build: 2026-03-09 `assembleDebug` BUILD SUCCESSFUL in 48s (report-driven `TKT-0269` foreground-service compliance follow-up; `TerminalManager` logged `task_removed` / `trim_memory`, `ConnectionNotifier` logged show/hide success/failure, and `TerminalManager` was declared as a typed `specialUse` foreground service)
- Prior log: `references/logs/android_build_2026-03-09T11-52-25+02-00.log`
- Prior APKs: `references/builds/termbot-oss-debug-2026-03-09T11-52-25+02-00.apk`, `references/builds/termbot-google-debug-2026-03-09T11-52-25+02-00.apk`
- Prior build: 2026-03-09 `assembleDebug` BUILD SUCCESSFUL in 54s (report-driven `TKT-0269` service-boundary follow-up; `TerminalManager` reasserted running-notification state at bind/unbind/open/disconnect/start-command/pref-change and logged whether `Persist connections` is enabled)
- Prior log: `references/logs/android_build_2026-03-09T11-21-56+02-00.log`
- Prior APKs: `references/builds/termbot-oss-debug-2026-03-09T11-21-56+02-00.apk`, `references/builds/termbot-google-debug-2026-03-09T11-21-56+02-00.apk`
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 53s (observability-only `TKT-0269` diagnosis build; the operator report later proved service teardown, not SSH/SSM transport, is the initiating event)
- Prior log: `references/logs/android_build_2026-03-08T21-03-19+02-00.log`
- Prior APKs: `references/builds/termbot-oss-debug-2026-03-08T21-03-19+02-00.apk`, `references/builds/termbot-google-debug-2026-03-08T21-03-19+02-00.apk`
- Prior recovery build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 40s (rollback of the initial `TKT-0269` slice after operator-reported startup autoclose on the 17:24 build, but operator later reported the 17:36 recovery still autoclosed)
- Prior recovery log: `references/logs/android_build_2026-03-08T17-36-24+02-00.log`
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 40s (`TKT-0269` Slice A attempted app foreground/background visibility broadcasts, background-only partial wake-lock protection for active network sessions, and running-notification battery-optimization guidance, but was rolled back before operator smoke because the app no longer launched on device)
- Prior log: `references/logs/android_build_2026-03-08T17-24-04+02-00.log`
- Prior recovery baseline: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 43s (`TKT-0268` Slice A: separate SSH `Route via SSM host` selector, additive route-host schema/backup support, and automatic SSH-over-SSM tunnel setup/teardown with defined route+jump ordering)
- Prior baseline log: `references/logs/android_build_2026-03-08T16-54-45+02-00.log`
- ADR: `ai_docs/docs/adr/0002_ssh_over_ssm_routing_order.md`
- Prior APKs: `references/builds/termbot-oss-debug-2026-03-08T16-54-45+02-00.apk`, `references/builds/termbot-google-debug-2026-03-08T16-54-45+02-00.apk`
- Manual smoke (operator, 2026-03-08/09): `TKT-0268` passed for direct SSH via selected SSM route host, direct SSH regression, direct SSM shell regression, SSH jump-host regression, and the combined `SSM route host + SSH jump host` topology.
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 29s (`TKT-0267` follow-up fix: gate `smux` by handshake session type, tolerate printable preamble before first mux frame, and accept newer-agent inbound mux markers)
- Prior log: `references/logs/android_build_2026-03-08T15-47-04+02-00.log`
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 30s (`TKT-0267` follow-up fix: modern-agent local port forwarding now uses client-side `smux_v1` based on official AWS plugin behavior)
- Prior log: `references/logs/android_build_2026-03-08T15-22-04+02-00.log`
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 24s (`TKT-0267` follow-up fix: keep Session Manager stderr out of the forwarded TCP socket path)
- Prior log: `references/logs/android_build_2026-03-08T15-04-35+02-00.log`
- Prior build: 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 39s (`TKT-0267` Slice 1: document-based SSM tunnel sessions, raw port-stream forwarding, and SSM local-forward UI guardrails)
- Command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=…/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Prior build log: `references/logs/android_build_2026-03-08T12-17-01+02-00.log`
- Prior build (roadmap cleanup): 2026-03-08 `assembleDebug` BUILD SUCCESSFUL in 42s — log `references/logs/android_build_2026-03-08T11-47-05+02-00.log`
- Manual smoke (operator, 2026-03-06): SSM connect successful; `uname -a` returned output; `whoami` returned `ssm-user`; `exit` cleanly disconnected.
- Manual smoke (operator, 2026-03-07): SSM host editor fixes confirmed (target movement + strict SSM-only target visibility behavior).
- Manual smoke (operator, 2026-03-07): TKT-0263 Slice A confirmed for valid credentials, invalid secret surfacing, and remember-password OFF re-prompt behavior.
- Manual smoke (operator, 2026-03-07): TKT-0265 encrypted backup export/import restored SSM host target and saved secret without re-entry.
- Manual smoke (operator, 2026-03-07): TKT-0266 confirmed assume-role flow works; operator asked to mark the ticket done and continue.
- Manual smoke (operator, 2026-03-08): `TKT-0267` managed-node test used `15432 -> localhost:22` and a separate SSH host to `127.0.0.1:15432`; the raw build and stderr-segregation follow-up failed during SSH key exchange, which led to the official-plugin review and `smux` implementation follow-up.
- Manual smoke (operator, 2026-03-08): report `termbot-report-20260308-153951.txt` from the `15:22` build confirmed the device was on `smux_v1` with agent `3.3.2299.0`, but the tunnel still aborted on `Unexpected SSM smux version 83`; that led to the `15:47` handshake-gating and preamble-tolerance follow-up.
- Manual smoke (operator, 2026-03-08): the same managed-node SSH scenario now passes on the `15:47` build (`15432 -> localhost:22`, then SSH to `127.0.0.1:15432`).
- Manual smoke (operator, 2026-03-08): remote-host/private-DB forwarding also works on the `15:47` build; operator connected a DB client to localhost on the forwarded port after creating a forward to the DB host/port.
- Operator issue (2026-03-08): active sessions still close when the app backgrounds; tracked in `TKT-0269` but not allowed to derail the next SSM operator-flow slice.
- APKs: `references/builds/termbot-oss-debug-2026-03-08T15-47-04+02-00.apk`, `references/builds/termbot-google-debug-2026-03-08T15-47-04+02-00.apk`
- Latest closeout gates passed: `TKT-0267` (2026-03-08), `TKT-0243`, `TKT-0244`, `TKT-0245`, `TKT-0247` (2026-03-05), plus earlier `TKT-0232`, `TKT-0233`, `TKT-0237`, `TKT-0239`, `TKT-0238`, `TKT-0234`, `TKT-0240`, `TKT-0225`, `TKT-0241`, `TKT-0242`

## Known risks (LIMIT=15)
- UI consistency gaps remain between auth and import key flows
- Legacy layout/style system slows modernization velocity
- Menu density in Host List increases discoverability/error risk
- `TKT-0268` landed new routing UX on top of the tunnel baseline; future lifecycle work in `TKT-0269` must not regress existing SSH `Jump via host` or the new SSM route-host path
- FIDO2/passkey MFA for the access-key/STS path remains a backlog item because AWS documents that passkeys/security keys are console-only and their MFA assertions cannot be passed to STS API operations

## Next actions (LIMIT=6)
1) Pick the next backlog slice after the RC docs baseline: YubiKey OATH/TOTP convenience follow-up versus TKT-0226 UI/UX work
2) Run the RC smoke gate from `ai_docs/docs/RELEASE.md` on the exact candidate build before sign-off
3) Keep sentinel matrix (`SK-01..SK-08`) plus the SSM operator matrix current for each release candidate
