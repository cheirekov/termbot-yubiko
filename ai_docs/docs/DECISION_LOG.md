# Decision Log

## 2026-02-27 — Pack installed / initialized
- Initialize local AI operating pack (ai_docs/ + references/).

## 2026-03-05 — Process reliability guardrails for long sessions
- Add a recurring AI-process retro checkpoint to control context drift.
- Treat review-column WIP limits and session handoff capsules as first-class process controls.

## 2026-03-05 — Review queue policy activated
- Enforce `Review` WIP limit of 2 tickets with oldest-first closure.
- Apply transition freeze when historical `Review` queue is already above the limit.

## 2026-03-05 — Standardized session handoff capsule adopted
- Add a 2-minute handoff template in handbook with required fields: last good build, active risks, open review items, and next actions.

## 2026-03-05 — Security-key sentinel smoke matrix adopted
- Treat `SK-01..SK-08` matrix as mandatory release smoke gate for OpenPGP/PIV/FIDO2 and ProxyJump key-auth paths.
- Require immediate in-app debug report export on first smoke failure.

## 2026-03-06 — SSM epic phased rollout path proposed
- Start SSM work with feasibility spike (`TKT-0261`) and explicit sub-ticket decomposition (`TKT-0262..TKT-0265`).
- Prefer thin in-app SSM client approach over immediate full AWS SDK bundling, pending open scope decisions.

## 2026-03-06 — SSM scope decisions confirmed by user
- Initial credential mode: AWS access key ID + secret access key.
- IAM Identity Center is explicitly deferred to backlog.
- Initial ship can proceed without YubiKey MFA integration.
- Start with StartSession shell-oriented bootstrap before role-chain/jump scenarios.

## 2026-03-06 — TKT-0262 implementation phase moved to real stream bridge
- Keep `TKT-0262` focused on shell-session websocket transport first (handshake + output/input/ack + disconnect), before advanced role-chain and multi-account jump flows.
- Keep KMSEncryption handshake action unsupported in this slice; treat encryption-specific handling as follow-up hardening after first interactive smoke.

## 2026-03-06 — TKT-0262 accepted after operator smoke
- Operator validated interactive SSM flow on device: connect, command output (`uname -a`, `whoami`), and clean `exit` disconnect.
- Move focus to `TKT-0264` for host-editor/session UX bug cleanup, then `TKT-0263` for credential/role expansion.

## 2026-03-06 — TKT-0264 quick-connect parsing rule for access-key-only input
- In SSM host editor quick-connect parsing, when input looks like an AWS access key ID and contains no `@`/`/`, treat it as `AWS Access Key ID` (`username`) rather than `Region` (`hostname`).
- Enforce SSM required fields (access key ID + region + target) before the host is considered valid for save.

## 2026-03-06 — TKT-0264 prompt lifecycle handoff policy
- When a session prompt is raised before `ConsoleActivity` prompt handler attachment, keep it visible by replaying the prompt signal during `PromptHelper.setHandler(...)` if `promptRequested` is already set.
- This prevents first-connect SSM password prompt loss caused by UI/service handler attach timing.

## 2026-03-06 — TKT-0264 quick-connect editing transaction for SSM
- Treat quick-connect parse + URI-parts synchronization as one guarded transaction (`mUriFieldEditInProgress`) to prevent recursive quick-field overwrite while typing SSM full compact address.
- On invalid quick-connect parse, clear stale SSM target (`postLogin`) to keep derived fields consistent.

## 2026-03-06 — TKT-0264 SSM target placement in editor
- Move `postLogin` editor block into the expanded URI section so SSM target is colocated with AWS access key ID, region, and port.
- Keep one shared `postLogin` field binding across protocols to avoid save-schema or behavior divergence.

## 2026-03-06 — TKT-0264 strict SSM-only target visibility
- Constrain target block visibility to `ssm` protocol only (`postlogin_section_container` hidden for other protocols), per operator UX preference.
- Keep SSM-specific title/summary/hint logic in `updatePostLoginLabels(...)` as the single control point for protocol-specific post-login UI state.

## 2026-03-07 — TKT-0264 accepted after operator host-editor smoke
- Operator confirmed final host-editor UX fixes, including strict SSM-only target visibility.
- Move active implementation focus to `TKT-0263` (credential expansion toward roles/jump scenarios).

## 2026-03-07 — TKT-0263 gated by PM/BA + UX prioritization
- Treat `TKT-0263` as broader MFA + role/jump readiness scope (not only YubiKey-specific follow-up).
- Pause implementation sequencing until PM/BA and UX clarify where role/account-jump inputs should live and what MFA modes are phase-1.

## 2026-03-07 — TKT-0263 Slice A storage and contract decision
- Keep Slice A backend-first: add credential-resolution contract and scoped secure-storage for SSM secrets/session tokens without shipping role/account-jump UI fields yet.
- Use marker-only observability in SSM flow (`credential_mode`, `secret_source`, `mfa_prompted`) and prohibit logging credential values.
- Preserve backward compatibility by reading legacy saved-password slot for SSM once and migrating to scoped SSM storage on successful connect.

## 2026-03-07 — TKT-0265 persistence decision for current SSM shape
- Do not bump `HostDatabase.DB_VERSION` for this slice: current SSM host metadata already fits existing host columns.
- Treat backup compatibility as an additive encrypted-backup JSON change instead:
  - export/import `saved_scoped_secrets` for persisted SSM scoped secrets
  - include SSM target (`postlogin`) in restore host matching for SSM entries
- Keep backup markers non-secret and include SSM-specific counts only.

## 2026-03-07 — TKT-0265 accepted after operator backup/import smoke
- Operator confirmed encrypted backup export/import restores the SSM host target and saved secret without re-entry.
- Keep the same access key/region with multiple distinct SSM targets as a later validation-matrix smoke, not a blocker for this ticket close.
- Return active sequencing focus to `TKT-0263` Slice B/C once PM/BA and UX decisions are available.

## 2026-03-07 — TKT-0263 PM/BA sequencing resolved
- Do not start with assume-role or multi-account jump UI.
- Ship temporary session credentials first on the existing SSM host flow:
  - detect temporary access keys by `ASIA...`
  - prompt for session token when needed
  - persist session token only in scoped secure storage when `remember password` is enabled
- Keep phase-1 MFA as external session-token issuance, not in-app MFA prompt UX.
- Future role/account-jump UX will use a mixed model: per-host direct session fields stay in the SSM editor, while multi-account role/jump chains move to a shared AWS profile abstraction in follow-up work.

## 2026-03-07 — TKT-0266 assume-role baseline extracted from broader credential scope
- Keep the first app-driven role flow intentionally small:
  - one optional SSM-only per-host role ARN field
  - one STS `AssumeRole` hop from base AWS credentials
  - no shared profile manager, role chain graph, or in-app MFA UX yet
- Persist only the operator-supplied base credentials; never persist assumed-role temporary credentials returned by STS.
- Log only non-secret booleans/categories for the role path (`assume_role_configured`, `credential_enhanced`).
