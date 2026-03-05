# Ticket: TKT-0215 — SSH ProxyJump via saved host (A -> X)

## Context capsule (must be complete)
### Goal
- Support connecting to target host `X` through jump/bastion host `A` from within TermBot.

### Scope
- In scope:
- Add per-host setting `Jump via host` (single hop) in host editor.
- Resolve target connection through selected jump host.
- Reuse existing auth flow for both jump and target hosts.
- Out of scope:
- Multi-hop chains (`A -> B -> X`) in first version.
- ProxyCommand parsing compatibility.

### Constraints
- Platform/runtime constraints:
- Must work with current Trilead transport APIs and Android network stack.
- Security/compliance constraints:
- Do not log credentials or raw auth payloads.
- Do NOT break:
- Existing direct SSH connections and port forwarding features.

### Target areas
- Files/modules:
- `app/src/main/java/org/connectbot/HostEditorFragment.java`
- `app/src/main/java/org/connectbot/bean/HostBean.java`
- `app/src/main/java/org/connectbot/util/HostDatabase.java`
- `app/src/main/java/org/connectbot/transport/SSH.java`
- `app/src/main/java/org/connectbot/service/TerminalManager.java`
- Interfaces/contracts:
- SSH connection lifecycle and disconnection cleanup.

### Acceptance criteria
- [x] Behavior:
- Host `X` can connect via `A` with one setting, without manual tunnels.
- [x] Tests (or explicit manual verification):
- Manual verify with bastion host and unreachable-direct target (confirmed by user on 2026-03-01).
- [x] Docs:
- Ticket + board + build status updated.
- [x] Observability (if relevant):
- Debug report logs `JUMP_HOST_USED=<nickname>` and jump-stage failures.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Configure host A and host X, set X to jump via A, connect to X.
- Expected output(s):
- Successful terminal on X when direct path to X is blocked but A is reachable.

### Risks / rollout
- Regression areas:
- Connection teardown ordering (jump session vs target session).
- Failure handling when jump host disconnects mid-session.
- Rollback plan:
- Keep schema field but short-circuit to direct-connect path if jump logic disabled.

## Notes
- Links:
- N/A
- Related tickets:
- TKT-0213

## Implementation progress (2026-03-01)
- Added per-host jump setting in host model/database/editor:
- `HostBean.jumpHostId`
- `HostDatabase.FIELD_HOST_JUMPHOSTID` with DB upgrade to version `28`
- Host editor UI field `Jump via host` with `None` + saved SSH hosts list
- Implemented SSH transport jump flow:
- Connect/authenticate jump host first
- Create local forward `127.0.0.1:<ephemeral> -> targetHost:targetPort`
- Connect/authenticate target host through local forward
- Reused existing auth methods for both hops (password/pubkey/security-key)
- Added debug markers to report:
- `JUMP_HOST_USED=<nickname>`
- `JUMP_STAGE=<stage>`
- `JUMP_STAGE_LAST=<latest stage summary>` in exported report header
- Build PASS:
- `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-01T18-25-23+02-00.log`

## Manual validation checklist
- Create host `A` (jump) and host `X` (target), set `X -> Jump via host: A`.
- Verify `X` connects when direct route to `X` is blocked but `A` is reachable.
- Export debug report and confirm `JUMP_HOST_USED`/`JUMP_STAGE_LAST` lines.

## Validation evidence (2026-03-01)
- User confirmation in session: "Jump host working super !"
