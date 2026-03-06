# System Map

Date: 2026-03-06
Primary repo: `repos/termbot-termbot`

## Components diagram (text)
1. User (Android UI)  
   -> `HostListActivity` / `ConsoleActivity` / `PubkeyListActivity` / `SecurityKeyActivity`
2. UI + app orchestration layer  
   -> `TerminalManager` + `SecurityKeyService`
3. Auth provider bridges  
   -> `YubiKitOpenPgpAuthenticatorBridge` / `PivSecurityKeyAuthenticatorBridge` / `Fido2SecurityKeyAuthenticatorBridge`
4. Hardware transport  
   -> NFC or USB via YubiKit connection APIs
5. SSH transport/session layer  
   -> `org.connectbot.transport.SSH`
6. Remote SSH server(s)

Parallel local state path:
- UI/Service layers <-> `HostDatabase` / `PubkeyDatabase` / `SavedPasswordStore` / encrypted backup manager

## Data flows
1) **Connection + auth flow (runtime critical path)**
- User starts/opens host session (`HostListActivity` / deep-link to `ConsoleActivity`)
- App/session layer invokes SSH auth negotiation (`transport/SSH.java`)
- If security-key method selected, app delegates to security-key flow (`SecurityKeyActivity`, service/bridge path)
- Provider bridge requests operation on YubiKey (OpenPGP/PIV/FIDO2) over NFC/USB
- Signature/assertion result is returned to SSH layer
- SSH layer continues auth success/failure handling and terminal session setup

2) **Key import/management flow**
- User opens `PubkeyListActivity` and triggers key import/add action
- Activity coordinates provider-specific import logic (OpenPGP/PIV/FIDO2)
- Public key metadata stored in local key DB (`PubkeyDatabase`)
- Imported key appears in UI and becomes available to SSH auth path

3) **Backup/export flow**
- User triggers encrypted backup export/import
- `EncryptedBackupManager` + DB layers package/restore host/key/password state
- File-based handoff occurs through app storage/FileProvider

4) **Diagnostics flow**
- Runtime events append to in-app debug buffers (`SecurityKeyDebugLog`)
- User exports report with `SecurityKeyDebugReportExporter`
- Report shared out-of-app for troubleshooting

## Trust boundaries
- **Boundary A: Device user/UI input -> app runtime**
  - Sensitive inputs include PIN/password data; must remain redacted in logs.
- **Boundary B: App runtime -> external hardware token (NFC/USB)**
  - Hardware communication reliability/lifecycle is failure-prone and security-sensitive.
- **Boundary C: App runtime -> remote SSH host/network**
  - Untrusted network/server boundary; auth decisions and retry semantics are high risk.
- **Boundary D: App runtime -> local storage/backup artifacts**
  - Host and credential-related data at rest; backup encryption and import validation are mandatory controls.
- **Boundary E: Repo/CI automation -> production app decisions**
  - Multiple CI definitions (`.github/workflows`, `Jenkinsfile`, `.travis.yml`) may diverge and create quality signal ambiguity.
