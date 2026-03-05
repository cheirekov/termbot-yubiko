# STATE (Authoritative "Now") — KEEP SMALL

Last updated: 2026-03-05 (TKT-0246 closed from user publication + CI confirmations)
Mode: multi-repo
Workspace root: /home/yc/work/ai-projects-templates/workspace
Owners: yc

---

## Project identity (LIMIT=8)
- Product/system: TermBot Android SSH client with YubiKey auth (OpenPGP, PIV, FIDO2)
- Current phase: post-stabilization UX hardening + modernization follow-through
- Primary users: Android SSH users authenticating with YubiKey
- Success definition (2-6 weeks): stable key auth/import UX + incremental visual modernization + backup reliability
- Runtime: Android app on device (Honor Magic V3 / MagicOS class devices validated by user)
- SDK direction: YubiKit-only for active auth flows (legacy hwsecurity not in runtime path)

## Current objectives (LIMIT=10)
- Keep key auth stable while improving UX in small slices
- Execute board priorities in order:
  1) TKT-0226 UI/UX improvements epic tail items
  2) Keep release smoke matrix cadence for each RC
  3) Keep CI green on public repo
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
- NEXT: TKT-0226 UI/UX improvements epic tail items
- Backlog (high): TKT-0226 tail items

## Latest verification (LIMIT=10)
- Latest build: 2026-03-04 `assembleDebug` successful in 32s
- Command: `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Log: `references/logs/android_build_2026-03-04T17-30-28+02-00.log`
- Latest closeout gates passed: `TKT-0243`, `TKT-0244`, `TKT-0245`, `TKT-0247` (2026-03-05), plus earlier `TKT-0232`, `TKT-0233`, `TKT-0237`, `TKT-0239`, `TKT-0238`, `TKT-0234`, `TKT-0240`, `TKT-0225`, `TKT-0241`, `TKT-0242`

## Known risks (LIMIT=15)
- UI consistency gaps remain between auth and import key flows
- Legacy layout/style system slows modernization velocity
- Menu density in Host List increases discoverability/error risk

## Next actions (LIMIT=6)
1) Continue TKT-0226 UI/UX epic tail items
2) Keep Review WIP policy active (review queue currently empty)
3) Use HANDBOOK handoff capsule at each pause/end-of-session
4) Run sentinel matrix (`SK-01..SK-08`) for each release candidate before sign-off
5) Keep GitHub Actions green after each public push
