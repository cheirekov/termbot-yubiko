# Ticket: TKT-0226 — UI/UX improvements epic (post-modernization focus)

## Context capsule (must be complete)
### Goal
- Modernize TermBot interaction quality with clearer information architecture and security-key UX, while preserving current power-user workflows.

### Scope
- In scope:
- Host list UX refresh (readability, grouping affordances, quick actions).
- Security-key UX polish (clear PIN prompts, clearer tap/USB/NFC guidance, better error texts).
- Security-key interaction cue redesign: prominent screen/overlay with clear YubiKey image and explicit "Touch via NFC or insert via USB" states (not only small status icon).
- Backup/import/export UX clarity (explicit success/failure feedback and progress states).
- Incremental visual cleanup aligned with existing app style unless explicitly redesigned.
- Out of scope:
- Full design-system rewrite in one pass.
- Breaking navigation paradigm changes in v1.

### Constraints
- Platform/runtime constraints:
- Prioritize after/alongside Android toolchain modernization to reduce rework.
- Must remain functional on supported device range.
- Security/compliance constraints:
- UI changes must not expose secrets in logs/screens.
- Do NOT break:
- Existing SSH terminal behavior and auth flows.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/HostListActivity.java`
- `app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `app/src/main/res/layout/*`
- `app/src/main/res/values/strings.xml`
- Interfaces/contracts:
- Existing activity/fragment contracts and backup/auth callbacks.

### Acceptance criteria
- [ ] Behavior:
- Primary flows (host connect, key auth, backup import/export) are visually clearer and require fewer user retries.
- [ ] Tests (or explicit manual verification):
- Manual UX smoke pass on phone: connect host, auth with YubiKey, export/import backup.
- [ ] Docs:
- Ticket + board + build status updated.
- [ ] Observability (if relevant):
- Existing debug markers still emitted for key UX states; no sensitive data added.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Validate updated screens for host list, security-key prompts, and backup flows.
- Expected output(s):
- Reduced ambiguity in prompts and clearer flow completion.

### Risks / rollout
- Regression areas:
- Layout regressions on small screens, accessibility text truncation, theme inconsistencies.
- Rollback plan:
- Land in small slices with feature-level rollback by reverting specific UI commits.

## Notes
- Links:
- User prioritization note: modernization first, then UI improvements.
- Related tickets:
- TKT-0224, TKT-0225, TKT-0222, TKT-0223
