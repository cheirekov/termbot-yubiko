# Debt Clusters

Date: 2026-03-06

## Cluster 1 — Security-key lifecycle and auth state complexity
- Evidence:
  - Repeated high-priority fixes across TKT-0228/0229/0230/0231/0241/0242 show fragile interactions among activity lifecycle, transport callbacks, and auth state handling.
  - Core path spans multiple layers (`SecurityKeyActivity`, `SecurityKeyService`, `securitykey/*`, `transport/SSH.java`).
- Approach:
  - Add explicit invariant checks and narrow-scope regression tickets around lifecycle ownership, retry boundaries, and jump-host state restoration.
  - Keep instrumentation/log markers non-secret and consistent.
- Tickets:
  - `TKT-0252-security-key-lifecycle-state-machine-hardening.md`
  - `TKT-0253-jump-host-auth-regression-guardrails.md`
  - `TKT-0254-auth-failure-taxonomy-and-user-surfacing.md`

## Cluster 2 — CI and release-gate ambiguity
- Evidence:
  - Coexisting CI systems (`.github/workflows`, `Jenkinsfile`, `.travis.yml`) with no single declared canonical merge gate.
  - Recent work already required CI compatibility repair tickets (TKT-0246/TKT-0247).
- Approach:
  - Define canonical CI lane(s), deprecate or quarantine legacy pipelines, and codify release gate commands and artifacts.
- Tickets:
  - `TKT-0255-ci-canonical-gate-declaration-and-cleanup.md`
  - `TKT-0256-release-candidate-smoke-gate-automation-script.md`

## Cluster 3 — UX consistency tail and accessibility hardening
- Evidence:
  - STATE still lists `TKT-0226` tail work after major theme/flow refresh tickets.
  - Multiple recent tickets were hotfix-style UX parity adjustments.
- Approach:
  - Finish the epic with focused polish slices: hierarchy, affordance consistency, accessibility defaults, and error UX consistency.
- Tickets:
  - `TKT-0257-ui-ux-epic-tail-closure-batch-a.md`
  - `TKT-0258-accessibility-and-input-modality-regression-sweep.md`

## Cluster 4 — Process/documentation drift in multi-repo metadata
- Evidence:
  - `ai_docs/config/repos.list` references missing `repos/hwsecurity` path while runtime truth is YubiKit-only.
  - This can mislead future inventory/risk runs.
- Approach:
  - Reconcile repo metadata and state references, then enforce snapshot hygiene in continuation workflow.
- Tickets:
  - `TKT-0259-repo-map-and-state-truth-reconciliation.md`
