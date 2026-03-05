# Delivery Board (Kanban)

## Backlog
- [ ] TKT-0226: UI/UX improvements epic (post-modernization focus)
- [ ] TKT-000: Create repo inventory (example)

## Ready
- [ ] (none)

## In Progress
- [ ] (none)

## Review
- [ ] TKT-0243: Desktop Mode Manage Keys action accessibility (implemented; awaiting Desktop Mode smoke)
- [ ] TKT-0244: FIDO2 USB transport parity (implemented; awaiting USB import/auth smoke)
- [ ] TKT-0245: First public GitHub commit preparation (README/.gitignore updated; awaiting wording validation)
- [ ] TKT-0246: Root README/LICENSE and GitHub Actions bootstrap (implemented; awaiting publication validation)
- [ ] TKT-0247: GitHub Actions Java11 compatibility fix (implemented; awaiting CI rerun)

## Done
- [x] PACK-INIT: Installed AI operating pack
- [x] TKT-0248: AI process retro + context-window reliability review — DONE 2026-03-05; added process-only retro with prioritized guardrails (review WIP limit, context checkpoints, handoff capsule, regression sentinel matrix)
- [x] TKT-0201: Security key timeout guard in auth handoff/sign flow
- [x] TKT-0202: Lifecycle-safe service/authenticator delivery
- [x] TKT-0203: Fail-fast auth error + safe log markers
- [x] TKT-0211: In-app debug report export for NFC/OpenPGP PIN-path diagnosis
- [x] TKT-0213: SSH auth verbose tracing for hardware security key auth path
- [x] TKT-0214: Secure saved password auth (optional, per host)
- [x] TKT-0215: SSH ProxyJump via saved host (A -> X)
- [x] TKT-0216: Fix post-success auth state mismatch in security-key flow
- [x] TKT-0217: YubiKey-only policy + SDK migration spike
- [x] TKT-0218: Encrypted backup/restore for hosts, credentials, and keys
- [x] TKT-0219: In-app SSH key pair generation
- [x] TKT-0220: YubiKey PIV and FIDO2 support expansion (phase 1 capability probe)
- [x] TKT-0223: FIDO2 SSH-SK support (YubiKey resident/non-resident)
- [x] TKT-0221: Debug log PIN/APDU redaction hardening — HEX threshold 16→4 pairs, APDU/NFC UID patterns (2026-03-02)
- [x] TKT-0222: YubiKey PIV/OpenPGP import fix — `session.getPublicKey(KeyRef.AUT).toPublicKey()`; Ed25519 normalization; `pubkey_add_openpgp_success` string; assembleDebug GREEN 35s 2026-03-02
- [x] TKT-0224: Android modernization epic — Phases A–D complete (AGP 4.2.2, Gradle 6.7.1, compileSdk/targetSdk 34, appcompat/constraintlayout/material bumped)
- [x] TKT-0227: Replace hwsecurity SDK with YubiKit SDK — DONE 2026-03-02; zero de.cotech refs; YubiKitManager lifecycle; assembleDebug GREEN 47s 2026-03-02
- [x] TKT-0228: OpenPGP NFC two-tap + PIV/FIDO2 NFC race fix — DONE 2026-03-02; pre-tap PIN collection; removed new Thread() NFC race; assembleDebug GREEN 2026-03-02
- [x] TKT-0229: YubiKit connection lifecycle regression fix — DONE 2026-03-03; bridges/import paths now open fresh SmartCardConnection on demand; assembleDebug GREEN 2026-03-03
- [x] TKT-0230: OpenPGP PIN prompt/fail-fast + Manage Keys hwsecurity link cleanup — DONE 2026-03-03; OpenPGP no longer loops/hangs on invalid PIN, prompt text corrected, UI links switched to Yubico
- [x] TKT-0231: OpenPGP auth APDU 0x6982 fix — DONE 2026-03-03; switched OpenPGP verifyUserPin mode to PW1 0x82 for INTERNAL AUTHENTICATE
- [x] TKT-0235: Engineering retro + release readiness — DONE 2026-03-03; retro doc, updated risk register, release-readiness checklist, RC plan drafted
- [x] TKT-0236: Security-key waiting UI (prominent touch/USB prompt) — DONE 2026-03-03; replaced transient toasts with persistent dialog in OpenPGP/PIV/FIDO2 flows
- [x] TKT-0232: PIN input policy and keyboard consistency — DONE 2026-03-04; OpenPGP/PIV/FIDO2 local PIN policy validation + non-secret reject markers
- [x] TKT-0233: Professional UI/UX review + design variants — DONE 2026-03-04; audit + 3 variant directions + linked implementation backlog
- [x] TKT-0237: Security-key import UX parity + waiting-state redesign — DONE 2026-03-04; import wait toasts replaced with persistent cancelable dialog + import wait/OpenPGP lifecycle debug markers
- [x] TKT-0239: Backup import/export UX hardening — DONE 2026-03-04; inline password validation (no dialog reopen), operation lock + progress dialog, backup operation flow markers
- [x] TKT-0238: Theme token and component refresh — DONE 2026-03-04; color/spacing/type tokens added + list/dialog/bottom-sheet/FAB tokenized refresh
- [x] TKT-0234: App icon rebrand and store asset package — DONE 2026-03-04; launcher icon refreshed (legacy + adaptive + monochrome), manifest switched to mipmap launcher assets, and store draft package/concepts added
- [x] TKT-0240: UI contrast regression hotfix + Day/Night theme — DONE 2026-03-04; fixed white-on-white text regressions, added terminal-friendly dark palette (`values-night`), and added System/Light/Dark app theme setting
- [x] TKT-0225: Host grouping/folders with backup import/export compatibility — DONE 2026-03-04; user confirmed grouped list visibility/expand behavior and flow usability on device
- [x] TKT-0241: P0 jump-host auth state leak skips target publickey auth — DONE 2026-03-04; user confirmed jump-host + target security-key auth works after hardening
- [x] TKT-0242: P0 USB YubiKey discovery lifecycle and permission resilience — DONE 2026-03-04; user confirmed USB auth/import flow works on device
