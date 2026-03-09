# Release

## Environments

- dev: local debug builds (`ossDebug`, `googleDebug`)
- staging: internal tester APK distribution (manual install)
- prod: Google Play track (planned)

## Current RC Candidate Baseline

- Candidate id: `RC-2026-03-yubikey-ssm-baseline`
- Status: documentation baseline prepared; release sign-off still pending
- Operator docs:
  - `README.md`
  - `MANUAL.md`

### Candidate scope

- YubiKey-backed SSH auth with OpenPGP, PIV, and FIDO2
- Direct SSH hosts and SSH `Jump via host`
- Saved passwords, encrypted backup/export, host grouping, and debug report export
- Direct SSM shell sessions
- SSM temporary-session credentials
- SSM assume-role support
- SSM MFA support through AWS STS
- SSM local port forwarding to the managed node or to a remote host behind it
- SSH `Route via SSM host`
- Combined `Route via SSM host` + `Jump via host`
- Background/foreground session resilience on the currently validated debug baseline

### Explicitly out of scope for this candidate

- AWS IAM Identity Center
- FIDO2/passkey MFA for AWS STS API calls
- In-app YubiKey OATH/TOTP autofill
- Non-local SSM forward modes

## Deployment Strategy

- staged rollout after internal RC validation:
  - RC build
  - internal smoke matrix
  - limited tester cohort
  - broader rollout

## Rollback

- procedure:
  - revert to the previous known-good APK release tag or packaged APK
  - disable newly introduced feature flags if available
  - rerun the release smoke gate before re-approving the candidate
- migration considerations:
  - preserve DB schema compatibility for hosts, route/jump references, and backups
  - keep encrypted backup format backward-compatible

## Release Readiness Checklist

- [x] OpenPGP auth stable on device
- [x] PIV auth stable on device
- [x] FIDO2 auth stable on device
- [x] In-app debug export available from host list
- [x] Docker `assembleDebug` green with pinned image
- [x] Security-key sentinel smoke matrix defined
- [x] Direct SSM shell operator-smoked
- [x] SSM assume-role operator-smoked
- [x] SSM MFA operator-smoked
- [x] SSM managed-node and remote-host port forwarding operator-smoked
- [x] SSH via SSM route host operator-smoked
- [x] Combined `Route via SSM host` + `Jump via host` operator-smoked
- [x] Background/foreground session survival operator-smoked on current validated baseline
- [ ] RC build/tag selected for sign-off
- [ ] Final RC smoke rerun completed on the exact candidate build
- [ ] Known-issues section reviewed and approved

## Per-release Smoke Gate

1. Run the security-key sentinel matrix in `ai_docs/docs/QUALITY_PLAN.md`.
2. Run the SSM/operator matrix on the exact RC candidate build:
   - direct SSM shell
   - SSM assume-role
   - SSM MFA
   - SSM local port forward to managed-node port
   - SSM local port forward to remote host/private DB
   - SSH via selected SSM route host
   - SSH via selected SSM route host plus SSH jump host
   - background/foreground session survival for at least one active SSH or SSM session
3. If any case fails, export a debug report immediately and attach it to the active ticket.
4. Do not advance RC status until all failed cases are rerun and pass on the same candidate build.
