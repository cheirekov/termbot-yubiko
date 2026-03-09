# Ticket: TKT-0258 — Accessibility and input-modality regression sweep

## Context capsule (must be complete)
### Goal
- Improve confidence that recent UI/security-key improvements remain accessible across touch, hardware keyboard, and desktop-mode interaction patterns.

### Scope
- In scope:
  - Run focused accessibility sweep (labels, focus order, actionable controls, contrast follow-up).
  - Validate key flows with hardware keyboard and desktop-mode pointer interactions.
  - File/fix high-severity regressions in small patches.
- Out of scope:
  - Full WCAG certification effort.

### Constraints
- Platform/runtime constraints:
  - Must test on currently used Android device profile and desktop-mode scenario.
- Security/compliance constraints:
  - Accessibility hints must not reveal secrets.
- Do NOT break:
  - Existing key auth/import interaction behavior.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/` (activities/dialog interactions)
  - `repos/termbot-termbot/app/src/main/res/layout/` and related style resources
- Interfaces/contracts:
  - Keyboard enter/escape behavior, focus traversal, and action discoverability.

### Acceptance criteria
- [ ] Behavior:
  - No high-severity accessibility blocker remains in auth/import/manage-key core flows.
- [ ] Tests (or explicit manual verification):
  - Manual sweep checklist executed for touch + hardware keyboard + desktop mode.
- [ ] Docs:
  - Findings and fixes summarized in ticket notes.
- [ ] Observability (if relevant):
  - Add non-secret debug marker only if accessibility fix alters runtime auth behavior.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Navigate core flows using keyboard-only and desktop-mode pointer to verify focus/action parity.
- Expected output(s):
  - Core flows are operable without touch-only assumptions.

### Risks / rollout
- Regression areas:
  - Dialog key handling and focus side effects in legacy layouts.
- Rollback plan:
  - Revert specific interaction changes and keep non-invasive accessibility metadata updates.

## Notes
- Links:
  - `ai_docs/tickets/TKT-0243-desktop-mode-manage-keys-action-accessibility.md`
- Related tickets:
  - `TKT-0257`
