# TKT-0263 PM/BA Prioritization Note (2026-03-07)

## 1) Summary
- Recommendation: run `TKT-0263` in phased hybrid mode (`backend+security first`, then `UX fields`), with `TKT-0265` as a release gate before shipping any schema-affecting role/jump UX.
- Rationale: avoid locking a crowded host-editor UX too early while still unblocking MFA/role-ready credential architecture.

## 2) Evidence / assumptions
- SSM baseline transport is already working on device and user-smoked: `ai_docs/boards/DELIVERY_BOARD.md`, `ai_docs/STATE.md`.
- Host editor has just completed a high-touch UX cleanup and should avoid immediate overload from role/account/jump fields without UX alignment: `ai_docs/tickets/TKT-0264-ssm-host-editor-and-session-ux.md`.
- User direction explicitly asks for MFA scope beyond only YubiKey and uncertainty about UX placement for roles/account IDs/jump fields: operator chat notes reflected in `ai_docs/tickets/TKT-0263-aws-credentials-and-optional-yubikey-mfa.md`.
- IAM Identity Center remains out of scope/backlog: `ai_docs/docs/DECISION_LOG.md`.

## 3) Deliverable (plan)
- Priority order:
  1) `TKT-0263` Slice A: credential domain expansion (session-token capable contract, MFA-ready hooks, redaction/observability hardening) with minimal UI changes.
  2) `TKT-0265`: schema/export compatibility implementation aligned to finalized credential model.
  3) `TKT-0263` Slice B: UX for role/account/jump input placement (recommended: shared AWS profile manager + per-host selector/override, not full host-editor field explosion).
  4) `TKT-0263` Slice C: assume-role + MFA interaction flow and operator smoke for account-jump scenarios.
- Definition of done for prioritization lock:
  - PM/BA confirms Slice order above.
  - UX confirms field placement model (host-only vs shared-profile vs mixed).
  - MFA phase-1 modes selected (TOTP, hardware-backed, or session-token-only path).

## 4) Risks & mitigations
- Risk: UI churn if role/account fields are added directly into host editor now.
  - Mitigation: defer field-heavy UX until profile model and schema contract are stable.
- Risk: backup/import incompatibility if schema changes are made late.
  - Mitigation: treat `TKT-0265` as explicit gate before shipping role/jump UX.
- Risk: MFA scope creep.
  - Mitigation: lock phase-1 MFA modes before implementation.

## 5) Next questions OR patch
- Confirm PM/BA choice for delivery sequence:
  - Keep recommended order above, or swap slices.
- Confirm UX placement:
  - Mixed model (`shared profile manager + host override`) is recommended default.
- Confirm phase-1 MFA modes:
  - TOTP/app code, hardware-backed (including YubiKey), or session-token-only.

## 6) Execution status
- 2026-03-07: user approved recommended order and implementation started with Slice A.
- Slice A status:
  - implemented backend credential resolver + scoped SSM secret/session-token storage hooks
  - build verified (`references/logs/android_build_2026-03-07T15-13-08+02-00.log`)
  - pending operator smoke + PM/BA/UX decisions for Slice B/C field placement
