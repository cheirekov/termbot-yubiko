# Continuation Engineering Plan

Date: 2026-03-06
Horizon: next 2–6 weeks
Scope: continuation on `repos/termbot-termbot` with Docker-first verification

User-directed branch update (2026-03-06): start `TKT-0260` SSM epic with `TKT-0261` feasibility spike while keeping existing reliability/UX priorities in backlog order when not blocked by SSM decisions.

## Goals (2–6 weeks)
- Keep security-key auth reliability stable while reducing regression risk in lifecycle/jump-host/error-state handling.
- Establish a single, explicit CI/release gate contract so quality signals are unambiguous.
- Close highest-impact TKT-0226 UI/UX tail items with accessibility/input-modality parity.
- Reconcile repo/process documentation drift to keep future sessions grounded in current truth.

## Milestones
### M1 — baseline quality and control-plane clarity (Week 1)
- Deliverables:
  - TKT-0255 CI canonical gate declaration and cleanup (policy + docs)
  - TKT-0259 repo map/state truth reconciliation
  - TKT-0256 RC smoke gate helper/checklist standardization (initial version)
- Exit criteria:
  - Canonical gate path documented and accepted.
  - Repo inventory references no missing active paths.
  - RC smoke evidence format is standardized.

### M2 — key reliability hardening in auth path (Weeks 2–4)
- Deliverables:
  - TKT-0252 security-key lifecycle state machine hardening
  - TKT-0253 jump-host auth regression guardrails
  - TKT-0254 auth failure taxonomy and user surfacing
- Exit criteria:
  - Sentinel subset passes across OpenPGP/PIV/FIDO2 target flows.
  - No indefinite waiting UI in tested failure cases.
  - Jump-host target auth behavior is deterministic and traceable.

### M3 — UX tail closure and release readiness (Weeks 4–6)
- Deliverables:
  - TKT-0257 UI/UX epic tail closure batch A
  - TKT-0258 accessibility and input-modality regression sweep
  - RC smoke run using updated gate process from M1/M2
- Exit criteria:
  - Prioritized UX consistency issues are closed.
  - Keyboard + desktop-mode interaction parity verified for core flows.
  - Release checklist has complete smoke evidence and no unresolved P0/P1 blockers.

## Work strategy
- ticket sizing:
  - Keep PR-sized slices (prefer ≤5 files / ≤300 LOC deltas unless unavoidable).
  - Separate behavior changes from cosmetic/documentation-only updates.
- review gates:
  - Enforce Review WIP limit 2 and oldest-first close policy.
  - Require build evidence and targeted manual verification notes per ticket.
- context capsule discipline:
  - Every ticket uses full context capsule template.
  - Unknowns are appended to `ai_docs/docs/QUESTIONS.md` instead of inferred.

## Risks
- Auth hardening may expose additional legacy state-coupling edge cases.
- CI cleanup may briefly reduce confidence if canonical/legacy split is not communicated clearly.
- UX polish can regress niche device modes if not validated under keyboard/desktop scenarios.
