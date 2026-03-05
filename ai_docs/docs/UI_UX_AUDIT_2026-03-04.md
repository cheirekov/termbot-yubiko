# UI/UX Audit — 2026-03-04

## Scope
- Host list
- Add/Edit host
- Manage keys / security-key import
- Security-key auth dialogs
- Backup import/export

## Method
- Code-level audit of layout/theme/menu resources and high-traffic activity flows.
- Focus on usability regressions, consistency, accessibility, and upgrade risk.

## Findings
1. `HIGH` Menu overload in Host List action menu.
   - Evidence:
     - `HostListActivity` builds many top-level actions in one overflow menu (`sort`, `keys`, `colors`, `disconnect`, `settings`, `debug report`, `backup export/import`, `probe`, `help`).
   - Impact:
     - High cognitive load; discoverability and accidental taps risk increase.
   - Source:
     - `repos/termbot-termbot/app/src/main/java/org/connectbot/HostListActivity.java` lines ~340-430.

2. `HIGH` Security-key import guidance is still toast-driven while auth guidance is now persistent dialog.
   - Evidence:
     - Import flows use `Toast.makeText(...)` for PIV/FIDO2/OpenPGP key touch prompts.
   - Impact:
     - Inconsistent UX and weaker guidance during NFC/USB waiting.
   - Source:
     - `repos/termbot-termbot/app/src/main/java/org/connectbot/PubkeyListActivity.java` lines ~447-455, ~532-535, ~547-549.

3. `HIGH` Add Key bottom sheet has dense action list with flat visual hierarchy.
   - Evidence:
     - Six sequential text actions + spinner in one accent-colored sheet.
     - All actions share near-identical styling and icon treatment.
   - Impact:
     - Error-prone action selection (especially OpenPGP/PIV/FIDO2 differences).
   - Source:
     - `repos/termbot-termbot/app/src/main/res/layout/dia_pubkey_add.xml`
     - `repos/termbot-termbot/app/src/main/java/org/connectbot/PubkeyAddBottomSheetDialog.java`

4. `MEDIUM` Backup password dialog validation can dismiss too early on invalid input.
   - Evidence:
     - Positive button uses standard `setPositiveButton`; invalid checks show toast then `return`.
   - Impact:
     - Users may need to reopen dialog for retries depending on platform behavior.
   - Source:
     - `repos/termbot-termbot/app/src/main/java/org/connectbot/HostListActivity.java` lines ~646-669.

5. `MEDIUM` Legacy layout system (RelativeLayout + hard margins) limits responsive polish.
   - Evidence:
     - Repeated fixed margins, icon offsets, and old list patterns.
   - Impact:
     - Harder to modernize spacing/typography and support large text gracefully.
   - Source:
     - `repos/termbot-termbot/app/src/main/res/layout/act_hostlist.xml`
     - `repos/termbot-termbot/app/src/main/res/layout/item_host.xml`
     - `repos/termbot-termbot/app/src/main/res/layout/fragment_host_editor.xml`

6. `MEDIUM` Visual system is still single-palette legacy green with limited tokenization.
   - Evidence:
     - Primary/accent both use same green family.
     - Component-specific colors are hardcoded in places.
   - Impact:
     - Weak information hierarchy; difficult theme evolution and brand refresh.
   - Source:
     - `repos/termbot-termbot/app/src/main/res/values/colors.xml`
     - `repos/termbot-termbot/app/src/main/res/values/styles.xml`

7. `MEDIUM` Accessibility polish gaps.
   - Evidence:
     - Multiple icons with `@null` content descriptions in functional flows.
     - Dense action lists with minimal grouping.
   - Impact:
     - Reduced screen reader clarity and slower navigation.
   - Source:
     - `fragment_host_editor.xml`, `dia_pubkey_add.xml`, `dia_security_key_wait_for_touch.xml`.

## Quick Wins (1 sprint)
1. Group Host List menu into sections: `Connection`, `Security Keys`, `Backup`, `Diagnostics`.
2. Replace import toasts with the same persistent waiting panel pattern used in auth.
3. Keep backup password dialogs open on local validation failure.
4. Split Add Key sheet into sections with short descriptions per action.
5. Add semantic accessibility labels to critical icons and action rows.

## Implementation Backlog Linked To Tickets
1. `TKT-0237` Security-key import UX parity + waiting panel states.
2. `TKT-0239` Backup import/export UX hardening and retry-friendly dialogs.
3. `TKT-0238` Theme token modernization + component refresh.
4. `TKT-0225` Host grouping/folders (information architecture + migration compatibility).
5. `TKT-0234` App icon rebrand/store assets (visual identity alignment).
