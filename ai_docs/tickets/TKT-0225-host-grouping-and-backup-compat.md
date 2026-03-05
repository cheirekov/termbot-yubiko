# Ticket: TKT-0225 — Host grouping/folders with backup import/export compatibility

## Context capsule (must be complete)
### Goal
- Allow users to organize hosts into groups/folders and preserve that structure across encrypted backup export/import.

### Scope
- In scope:
- Add host groups (create/rename/delete) and assign/move hosts to groups.
- Add grouped host list presentation with clear ungrouped/default section.
- Extend encrypted backup format to include group metadata and host-group links.
- Import path restores groups first, then remaps hosts to restored groups.
- Out of scope:
- Nested groups/subfolders in v1.
- Per-group secrets/policies in v1.

### Constraints
- Platform/runtime constraints:
- Must work on both `oss` and `google` flavors.
- Keep backward compatibility for older backups without group data.
- Security/compliance constraints:
- No new plaintext secret storage; group data is metadata only.
- No logging of backup plaintext payloads.
- Do NOT break:
- Existing host connect flow, jump-host references, and encrypted backup restore.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/bean/HostBean.java`
- `app/src/main/java/org/connectbot/util/HostDatabase.java`
- `app/src/main/java/org/connectbot/HostListActivity.java`
- `app/src/main/java/org/connectbot/HostEditorActivity.java`
- `app/src/main/java/org/connectbot/util/EncryptedBackupManager.java`
- Interfaces/contracts:
- Versioned backup schema must tolerate missing `groups` field.

### Acceptance criteria
- [x] Behavior:
- User can create groups and move/create hosts inside groups.
- Grouped structure is visible in host list and persists after app restart.
- Group headers remain visible even when empty/newly created and can be expanded/collapsed.
- Host list includes `Ungrouped` and `All` sections for quick navigation.
- [x] Tests (or explicit manual verification):
- Manual round-trip: create groups/hosts -> export backup -> clear data/import -> verify groups and host mappings restored.
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Exported debug report includes non-sensitive markers for group export/import counts.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Create 2 groups, add/move hosts, export encrypted backup, import, verify groups and host placement.
- Expected output(s):
- Host list shows restored group layout; hosts connect normally.

### Risks / rollout
- Regression areas:
- DB migration (new group table/column), host list sorting/filtering, backup schema compatibility.
- Rollback plan:
- Keep migrations additive; if UI path fails, hosts remain accessible via ungrouped fallback.

## Notes
- Links:
- User request date: 2026-03-02
- Related tickets:
- TKT-0218, TKT-0224, TKT-0226
- Implementation status (2026-03-04):
- DB/model: `hostgroups` table + host `groupid` column and migration to DB v29.
- Backup: `groups` array export/import with old->new group-id remap and non-secret count markers.
- UI: Host editor group picker; Host list grouped sections with visible empty groups and expandable headers; synthetic `Ungrouped` and `All` sections; menu actions for create/rename/delete group.
- Build verification: `assembleDebug` PASS, log `references/logs/android_build_2026-03-04T15-36-57+02-00.log`.
- Device confirmation (2026-03-04): user confirmed grouping behavior on device; new groups visible and expandable/collapsible.
