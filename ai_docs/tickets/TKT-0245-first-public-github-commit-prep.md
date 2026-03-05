# Ticket: TKT-0245 — First public GitHub commit preparation

## Context capsule (must be complete)
### Goal
- Prepare the repository for the first public GitHub commit of the current YubiKey-focused TermBot state.

### Scope
- In scope:
- Replace outdated repo `README.md` content with current app direction and upgrade summary.
- Review and fix `.gitignore` for workspace hygiene.
- Capture a release-prep checklist for public push readiness.
- Out of scope:
- Publishing/release automation.
- Final commit execution and tagging.

### Constraints
- Keep public README accurate (no obsolete hwsecurity runtime claims).
- Do not expose secrets, internal credentials, or private infrastructure details.
- Keep docs concise and user-facing.

### Target areas
- `repos/termbot-termbot/README.md`
- `.gitignore`
- `ai_docs/boards/DELIVERY_BOARD.md`
- `ai_docs/STATE.md`

### Acceptance criteria
- [x] Behavior (implementation):
- Public-facing README reflects current YubiKit-based architecture and key upgrades delivered so far.
- Workspace `.gitignore` typo fixed and local/deleted hwsecurity path handling clarified.
- [ ] Tests (manual):
- User validates README wording for public publication.
- [x] Docs:
- Ticket + board + state updated.

### Verification
- N/A (docs-only update)

## Release-prep checklist
- [x] README no longer references hwsecurity SDK as runtime dependency.
- [x] README includes current feature set and Docker-first debug build command.
- [x] `.gitignore` reviewed and corrected (`ai-project-template-v7` typo).
- [ ] Final pre-push sweep:
- choose which tickets/changes are included in first public commit
- verify licenses/attributions for bundled assets
- perform final smoke pass on NFC + USB auth flows
