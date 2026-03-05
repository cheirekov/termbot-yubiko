# TKT-0239 — Backup import/export UX hardening

## Status: DONE
## Priority: MEDIUM
## Epic: TKT-0226

## Problem
Backup import/export works functionally but UX is fragile in edge cases: dialog validation feedback is toast-based, and progress/error states are not clearly staged.

## Goal
Make backup workflows more reliable and self-explanatory for non-ADB users.

## Scope
- In scope:
  - Keep password dialogs open for local validation failures.
  - Provide inline validation and clearer progress/result messaging.
  - Improve file-pick/import recovery messaging.
- Out of scope:
  - Encryption format changes.
  - Cloud sync implementation.

## Acceptance Criteria
- [x] Invalid input does not force users to reopen dialogs.
- [x] Import/export progress and completion/error are clearly visible.
- [x] Flow remains compatible with existing `.tbbak` backups.

## Implementation Notes (2026-03-04)
- `HostListActivity` backup password dialogs now keep the dialog open for local validation errors:
  - empty password -> inline error on password field
  - export confirm mismatch -> inline error on confirm field
- Added backup operation in-progress lock and blocking progress dialog for both export and import:
  - prevents duplicate backup operations while one is active
  - shows clear staged message during export/import work
- Added non-secret backup operation state markers:
  - `BACKUP_OPERATION: export_started/export_success/export_failed/export_finished`
  - `BACKUP_OPERATION: import_started/import_success/import_failed/import_finished`
- Backup import format/crypto compatibility remains unchanged (`.tbbak` v1 path untouched).

## Verification
- Docker build command:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Result:
  - `BUILD SUCCESSFUL in 36s` (62 actionable tasks: 21 executed, 41 up-to-date)
- Log:
  - `references/logs/android_build_2026-03-04T13-11-59+02-00.log`
