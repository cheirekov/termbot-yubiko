# STATE (Authoritative "Now") — KEEP SMALL

Last updated: 2026-03-07 (TKT-0266 assume-role baseline implemented and build-verified; operator smoke pending)
Mode: multi-repo
Workspace root: /home/yc/work/ai-projects-templates/workspace
Owners: yc

---

## Project identity (LIMIT=8)
- Product/system: TermBot Android SSH client with YubiKey auth (OpenPGP, PIV, FIDO2)
- Current phase: post-stabilization UX hardening + SSM epic kickoff
- Primary users: Android SSH users authenticating with YubiKey
- Success definition (2-6 weeks): stable key auth/import UX + incremental visual modernization + backup reliability
- Runtime: Android app on device (Honor Magic V3 / MagicOS class devices validated by user)
- SDK direction: YubiKit-only for active auth flows (legacy hwsecurity not in runtime path)

## Current objectives (LIMIT=10)
- Keep key auth stable while improving UX in small slices
- Continue AWS SSM epic with credential foundations in place (`TKT-0263`), backup compatibility closed (`TKT-0265`), and app-driven assume-role baseline now in operator review (`TKT-0266`)
- Execute board priorities in order:
  1) Operator smoke for TKT-0266 with base AWS credentials + role ARN
  2) Close TKT-0266 if assume-role smoke passes
  3) Reconcile whether residual MFA/jump planning stays in `TKT-0263` or is split into later follow-up tickets
  4) TKT-0226 UI/UX improvements epic tail items
  5) Keep release smoke matrix cadence for each RC
  6) Keep CI green on public repo
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
- IN PROGRESS 2026-03-07: TKT-0263 AWS credential management + MFA + role/jump readiness (Slice A operator-smoked; Slice B build-verified with session-token prompt path confirmed in debug report; broader role/jump work is being split into focused follow-ups)
- DONE 2026-03-07: TKT-0265 SSM backup/export compatibility + schema migration (operator confirmed encrypted backup export/import restores SSM host target and saved secret; same-key/region multi-target smoke deferred to later matrix)
- IN REVIEW 2026-03-07: TKT-0266 SSM assume-role baseline (optional per-host role ARN, STS AssumeRole runtime chaining, runtime-only assumed creds, build-verified; operator smoke pending)
- Backlog (high): TKT-0226 tail items + later SSM MFA/jump/profile follow-ups

## Latest verification (LIMIT=10)
- Latest build: 2026-03-07 `assembleDebug` BUILD SUCCESSFUL in 44s (TKT-0266 assume-role baseline: per-host role ARN persistence + STS AssumeRole runtime chaining + runtime-only assumed creds)
- Log: `references/logs/android_build_2026-03-07T16-40-42+02-00.log`
- Prior build: 2026-03-07 `assembleDebug` BUILD SUCCESSFUL in 43s (TKT-0263 Slice B temporary session credentials: `ASIA...` session-token prompt + scoped persistence + marker-only session token source logging)
- Command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local GRADLE_USER_HOME=…/.gradle_cache_isolated ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug -- --no-daemon`
- Prior build log: `references/logs/android_build_2026-03-07T15-48-07+02-00.log`
- Prior build (TKT-0265): 2026-03-07 `assembleDebug` BUILD SUCCESSFUL in 28s — log `references/logs/android_build_2026-03-07T15-27-09+02-00.log`
- Manual smoke (operator, 2026-03-06): SSM connect successful; `uname -a` returned output; `whoami` returned `ssm-user`; `exit` cleanly disconnected.
- Manual smoke (operator, 2026-03-07): SSM host editor fixes confirmed (target movement + strict SSM-only target visibility behavior).
- Manual smoke (operator, 2026-03-07): TKT-0263 Slice A confirmed for valid credentials, invalid secret surfacing, and remember-password OFF re-prompt behavior.
- Manual smoke (operator, 2026-03-07): TKT-0265 encrypted backup export/import restored SSM host target and saved secret without re-entry.
- APKs: `references/builds/termbot-oss-debug-2026-03-07T16-40-42+02-00.apk`, `references/builds/termbot-google-debug-2026-03-07T16-40-42+02-00.apk`
- Latest closeout gates passed: `TKT-0243`, `TKT-0244`, `TKT-0245`, `TKT-0247` (2026-03-05), plus earlier `TKT-0232`, `TKT-0233`, `TKT-0237`, `TKT-0239`, `TKT-0238`, `TKT-0234`, `TKT-0240`, `TKT-0225`, `TKT-0241`, `TKT-0242`

## Known risks (LIMIT=15)
- UI consistency gaps remain between auth and import key flows
- Legacy layout/style system slows modernization velocity
- Menu density in Host List increases discoverability/error risk
- SSM stream bridge baseline works; remaining risk is edge-case protocol handling + host-editor UX roughness and later role/jump flows

## Next actions (LIMIT=6)
1) Run operator smoke for TKT-0266 with base AWS credentials + role ARN
2) Close TKT-0266 if assume-role smoke passes and no credential leakage/regression is observed
3) Decide whether residual MFA/jump planning closes `TKT-0263` or becomes new follow-up tickets after the assume-role baseline lands
4) Continue TKT-0226 UI/UX epic tail items in parallel only when SSM slice is blocked
5) Keep Review WIP policy active (review queue currently contains `TKT-0266`)
6) Run sentinel matrix (`SK-01..SK-08`) for each release candidate before sign-off
