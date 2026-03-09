# Ticket: TKT-0270 — Release-candidate docs and operator manual baseline

## Context capsule (must be complete)
### Goal
- Publish a release-candidate documentation baseline for the shipped TermBot feature set so operators can understand what is implemented, how to use it, and what remains out of scope before RC sign-off.

### Scope
- In scope:
  - create a dedicated RC documentation ticket
  - refresh the root `README.md` so it summarizes the shipped product capabilities, not only the workspace process
  - add a practical operator manual for the shipped SSH, YubiKey, and AWS SSM flows
  - refresh `ai_docs/docs/RELEASE.md` so RC smoke expectations include the current SSM feature set
- Out of scope:
  - code changes in the Android app
  - release signing or Play publishing
  - new YubiKey OATH/TOTP autofill work

### Constraints
- Platform/runtime constraints:
  - documentation must match the already shipped and operator-smoked behavior only
- Security/compliance constraints:
  - do not document unsafe secret-handling practices
  - make AWS MFA limitations explicit where AWS STS does not support passkey/FIDO assertions
- Do NOT break:
  - board/state closeout discipline
  - existing RC process docs

### Target areas
- Files/modules:
  - `README.md`
  - `MANUAL.md`
  - `ai_docs/docs/RELEASE.md`
  - `ai_docs/boards/DELIVERY_BOARD.md`
  - `ai_docs/STATE.md`
  - `ai_docs/docs/DECISION_LOG.md`
- Interfaces/contracts:
  - operator-facing product documentation
  - RC smoke-gate documentation

### Acceptance criteria
- [x] Behavior:
  - a new ticket exists for RC documentation baseline work
  - root `README.md` clearly lists shipped capabilities, docs links, and build entry points
  - an operator manual exists for direct SSH, jump host, direct SSM, role ARN, MFA, SSM port forwarding, SSH via SSM route host, and combined route + jump usage
- [x] Tests (or explicit manual verification):
  - docs reviewed against operator-confirmed shipped flows from `TKT-0263`, `TKT-0267`, `TKT-0268`, and `TKT-0269`
- [x] Docs:
  - `README.md`, `MANUAL.md`, and `ai_docs/docs/RELEASE.md` updated
  - board/state/decision log updated
- [x] Observability (if relevant):
  - not applicable for this docs-only slice

### Verification (token-efficient)
- Docker command(s) to run:
  - none; docs-only ticket
- Manual script(s) the user can run:
  - review `README.md`, `MANUAL.md`, and `ai_docs/docs/RELEASE.md`
  - confirm the manual matches the already tested app flows
- Expected output(s):
  - operators can find one concise entry point for shipped features and one practical manual for common SSH/SSM tasks

### Risks / rollout
- Regression areas:
  - documentation drift if later feature behavior changes without doc updates
- Rollback plan:
  - revert the docs-only commit or patch and restore the prior README/release docs

## Notes
- Links:
  - `README.md`
  - `MANUAL.md`
  - `ai_docs/docs/RELEASE.md`
- Related tickets:
  - `TKT-0235`
  - `TKT-0263`
  - `TKT-0267`
  - `TKT-0268`
  - `TKT-0269`
