# Ticket: TKT-0220 — YubiKey PIV and FIDO2 support expansion

## Context capsule (must be complete)
### Goal
- Expand TermBot hardware-key support beyond OpenPGP to include YubiKey PIV and FIDO2-based SSH auth.
- This ticket now acts as an umbrella for phased delivery; execution is split into dedicated follow-up tickets.

### Scope
- In scope:
- Technical spike + phased implementation plan for PIV and FIDO2 in app.
- PIV signing flow integration for SSH where feasible.
- FIDO2/SK key auth feasibility against current SSH library constraints.
- Out of scope:
- Non-YubiKey vendor support.
- Full desktop OpenSSH parity in one iteration.

### Constraints
- Platform/runtime constraints:
- Must stay compatible with Android NFC/USB capabilities on supported devices.
- Security/compliance constraints:
- No PIN/private key/APDU payload logging.
- Do NOT break:
- Existing working OpenPGP YubiKey flow and debug report export.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/securitykey/*`
- `app/src/main/java/org/connectbot/SecurityKey*`
- `app/src/main/java/org/connectbot/transport/SSH.java`
- Interfaces/contracts:
- `SecurityKeyAuthenticatorBridge` abstraction for SDK/provider implementations.

### Acceptance criteria
- [x] Behavior:
- Documented and approved implementation path for PIV/FIDO2; at least one new flow prototyped.
- [x] Tests (or explicit manual verification):
- Manual validation completed for phase-1 capability probe + debug export markers.
- [x] Docs:
- Ticket + board + build status updated with capability matrix.
- [x] Observability (if relevant):
- New flow markers appear in debug report without sensitive data.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Execute PIV/FIDO2 auth attempt and export report.
- Expected output(s):
- Clear success/failure markers that identify protocol stage.

### Risks / rollout
- Regression areas:
- Library support gaps for OpenSSH security-key algorithms (`sk-ecdsa`, `sk-ed25519`).
- Rollback plan:
- Keep OpenPGP as default path and gate new paths behind explicit enablement.

## Notes
- Links:
- User request date: 2026-03-01
- Related tickets:
- TKT-0217
- TKT-0221
- TKT-0222
- TKT-0223
- TKT-0224

## Implementation summary (phase 1, 2026-03-01)
- Added a new in-app prototype flow: `Run YubiKey Capability Probe` from Host List menu.
- Probe writes structured markers to in-app ring buffer (no secrets/APDU/PIN):
  - `YUBIKEY_CAPABILITY_PROBE_START`
  - `YUBIKEY_CAPABILITY_PROBE_RESULT`
  - `PIV_PROTOTYPE_STATUS`
  - `FIDO2_PROTOTYPE_STATUS`
  - `SSHLIB_SK_ECDSA_SUPPORTED`
  - `SSHLIB_SK_ED25519_SUPPORTED`
- Debug report export now includes capability matrix fields:
  - `usb_host_supported`
  - `PIV_PROTOTYPE_STATUS`
  - `FIDO2_PROTOTYPE_STATUS`
  - `SSHLIB_SK_ECDSA_SUPPORTED`
  - `SSHLIB_SK_ED25519_SUPPORTED`

## Capability matrix snapshot
- OpenPGP (existing): working in production path.
- PIV (new prototype): `prototype_planned_sdk_selection` (flow + observability scaffolding in place).
- FIDO2/SK (new prototype): currently blocked by SSH library lacking `sk-ecdsa` / `sk-ed25519` classes in current dependency set.
- Transport support indicators: measured in-app and exported via debug report.

## Phased plan
- Phase 1 (completed): capability probe + report matrix + markers.
- Phase 2 (active via TKT-0222): PIV import + signing provider integration for SSH auth path.
- Phase 3 (queued via TKT-0223): SSH `sk-*` algorithm support and FIDO2 auth path integration.
- Phase 4 (queued via TKT-0224): Android/SDK/AGP modernization required for long-term YubiKit support.
