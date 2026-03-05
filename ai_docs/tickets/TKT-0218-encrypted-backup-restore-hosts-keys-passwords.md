# Ticket: TKT-0218 — Encrypted backup/restore for hosts, credentials, and keys

## Context capsule (must be complete)
### Goal
- Let users migrate to a new device without manually re-adding hosts and keys by exporting/importing encrypted backups.

### Scope
- In scope:
- Export host definitions, saved password records, and private/public key entries into one encrypted backup file.
- Password-protect backup using strong KDF + authenticated encryption.
- Import flow with password prompt and validation.
- Optional cloud path in v1 via Android share sheet (user chooses Drive/Nextcloud/other app).
- Out of scope:
- Silent/background auto-sync service in v1.
- Server-side account system.

### Constraints
- Platform/runtime constraints:
- Must work on current min/target setup and both `oss` and `google` flavors.
- Security/compliance constraints:
- No plaintext password or private key material at rest in backup.
- Never log backup password, decrypted payload, or private key bytes.
- Do NOT break:
- Existing host DB schema and saved-password keystore paths.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/util/HostDatabase.java`
- `app/src/main/java/org/connectbot/util/SavedPasswordStore.java`
- `app/src/main/java/org/connectbot/util/*` (new backup serializer/crypto helper)
- `app/src/main/java/org/connectbot/*Activity.java` (UI entry points)
- Interfaces/contracts:
- Versioned backup manifest format for forward-compatible imports.

### Acceptance criteria
- [x] Behavior:
- User can export encrypted backup and import it on another device to restore hosts + keys + saved-password entries.
- [ ] Tests (or explicit manual verification):
- Manual round-trip on two devices/emulators with wrong-password and corrupted-file checks.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Debug report shows backup flow markers only (start/success/failure reason), no secrets.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Export backup, uninstall/reinstall or use second device, import backup, verify hosts/keys restored.
- Expected output(s):
- Restored entries appear and can connect; wrong password fails safely.

### Risks / rollout
- Regression areas:
- DB migration compatibility and key import integrity.
- Rollback plan:
- Keep backup feature behind menu entry; disable import path if verification fails.

## Notes
- Links:
- User request date: 2026-03-01
- Related tickets:
- TKT-0214, TKT-0215

## Implementation progress (2026-03-01)
- Added encrypted backup utility:
- `app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`
- Backup content includes:
- Hosts (with jump-host references remapped on import)
- Pubkeys (private/public payload preserved inside encrypted container)
- Saved host passwords (exported as plaintext only inside encrypted payload, then re-encrypted by `SavedPasswordStore` on import)
- Crypto model:
- Password-based KDF: PBKDF2 (SHA-256 with SHA-1 fallback for compatibility)
- Authenticated encryption: AES-GCM
- Added host list UI actions:
- `Export Encrypted Backup`
- `Import Encrypted Backup`
- Password prompt with confirmation on export.
- File picker + password prompt on import.
- Added picker compatibility fix for Android ROMs/file managers that return selected file via `ClipData` instead of `Intent.getData()`, plus explicit toast when URI is missing.
- Share sheet used for cloud upload path (Drive/Nextcloud/etc via chooser targets).
- Added debug markers:
- `BACKUP_EXPORT`
- `BACKUP_IMPORT`
- Added `SavedPasswordStore.exportPlaintextPasswords(...)` to support secure migration.
- Updated FileProvider paths for backup files (`externalFilesDir("backup")`).
- Build PASS:
- `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-01T18-45-45+02-00.log`

## Manual validation checklist (pending)
- Export encrypted backup with password.
- Import same backup with correct password (success) and wrong password (failure).
- Verify host connect works after import (including jump-host target).
- Verify saved-password host reconnect does not prompt after import.
