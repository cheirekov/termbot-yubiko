# Ticket: TKT-0257 — UI/UX epic tail closure (batch A)

## Context capsule (must be complete)
### Goal
- Close the highest-impact remaining UI/UX tail items from epic TKT-0226 with small, reviewable polish slices.

### Scope
- In scope:
  - Resolve top-priority visual/interaction inconsistencies still observed after recent theme/token work.
  - Tighten hierarchy and spacing consistency in host list / key management / settings touchpoints.
  - Ensure error and empty states follow the same component language.
- Out of scope:
  - Full redesign and major navigation restructuring.

### Constraints
- Platform/runtime constraints:
  - Must preserve support on current device targets (including desktop mode behavior).
- Security/compliance constraints:
  - UX improvements must not weaken auth clarity or expose sensitive info.
- Do NOT break:
  - Existing auth/import workflows and recently fixed contrast/theme behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/res/layout/`
  - `repos/termbot-termbot/app/src/main/res/values*/`
  - Relevant activities/fragments (`HostListActivity`, `PubkeyListActivity`, `SettingsActivity`)
- Interfaces/contracts:
  - Existing user interaction flows and menu actions.

### Acceptance criteria
- [ ] Behavior:
  - No major visual inconsistency remains in prioritized screens.
- [ ] Tests (or explicit manual verification):
  - Manual walkthrough of host list, manage keys, and settings in light/dark/system themes.
- [ ] Docs:
  - Ticket notes include before/after summary of resolved items.
- [ ] Observability (if relevant):
  - N/A (UI-only unless runtime behavior changed).

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Validate visual consistency across key screens and theme modes.
- Expected output(s):
  - No obvious spacing/contrast/hierarchy regressions in targeted screens.

### Risks / rollout
- Regression areas:
  - Theme/style overrides in older layouts.
- Rollback plan:
  - Revert isolated layout/style commits by area.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0226-ui-ux-improvements-epic.md`
- Related tickets:
  - `TKT-0258`
