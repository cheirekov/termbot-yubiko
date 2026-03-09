# Ticket: TKT-0268 — SSH over SSM bastion/tunnel integration

## Context capsule (must be complete)
### Goal
- Let TermBot connect to a private SSH host by opening an SSM tunnel through a managed node and then running the existing SSH transport over that tunnel.
- Make this feel like a first-class host-routing feature, not a manual port-forward workaround.

### Scope
- In scope:
  - Reuse the tunnel/session plumbing from `TKT-0267`.
  - Model an SSH target that is reached through an SSM-managed bastion/tunnel node.
  - Allow an SSH host to select an existing SSM host as its route/tunnel host.
  - Automatically create, manage, and tear down the needed localhost tunnel for the SSH session instead of requiring the operator to pre-create manual port forwards on the SSM host.
  - Reuse existing SSH auth methods on the final SSH hop (password, key, hardware-key paths where supported today).
  - Add host editor UX that keeps SSH `Jump via host` separate from SSM routing:
    - SSH jump host remains SSH-only
    - SSM route host is a separate selectable SSM host
- Out of scope:
  - Generic DB/app-client integration outside TermBot.
  - Multiple nested SSM tunnel hops.
  - Shared AWS profile manager UI.

### Constraints
- Platform/runtime constraints:
  - Must compose with the existing SSH transport rather than duplicating SSH logic.
- Security/compliance constraints:
  - Never log AWS credentials, SSH secrets, or sensitive target details.
- Do NOT break:
  - Existing direct SSH and direct SSM flows.
  - Existing SSH jump-host behavior (`ProxyJump`) for pure SSH cases.
  - Existing manual SSM port-forward workflow from `TKT-0267`.

### Target areas
- Files/modules:
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSH.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/`
  - `repos/termbot-termbot/app/src/main/java/org/connectbot/bean/HostBean.java`
- Interfaces/contracts:
  - Tunnel-backed socket/stream handoff into the SSH transport
  - Host editor contract for “route via SSM host” vs “jump via SSH host”

### Acceptance criteria
- [x] Behavior:
  - Connect to a private SSH host behind an SSM-managed node without SSHing to the bastion directly.
  - Operator selects an existing SSM host on the SSH host, and the app manages the temporary tunnel automatically.
  - No manual port-forward entry is required on the SSM host for the SSH-over-SSM path.
  - Existing SSH auth on the final hop still works.
- [x] Tests (or explicit manual verification):
  - Manual smoke to a private SSH host behind the managed node.
  - Manual regression smoke for direct SSH and direct SSM shell.
  - Manual regression smoke that SSH `Jump via host` still works and is not confused with SSM routing.
- [x] Docs:
  - Dependency on `TKT-0267` and the real bastion/tunnel scenario are documented.
- [x] Observability (if relevant):
  - Non-secret markers distinguish SSM tunnel setup from final SSH auth.

### Verification (token-efficient)
- Docker command(s) to run:
  - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
  - Configure a managed SSM node with network access to a private SSH host, then connect to the final SSH host through the SSM tunnel.
- Expected output(s):
  - Final SSH session opens through the SSM-managed tunnel, while direct SSH/direct SSM flows remain unaffected.

### Risks / rollout
- Regression areas:
  - SSH transport assumptions about direct sockets.
  - Host editor UX complexity around dual endpoints.
  - Interaction with existing jump-host and hardware-key SSH flows.
- Rollback plan:
  - Keep this behind a separate host mode/path so direct SSH and direct SSM remain untouched.

## Notes
- Related tickets:
  - `TKT-0260`, `TKT-0267`

## Progress
- 2026-03-08 — Created after AWS docs review:
  - Split from the withdrawn source-host idea.
  - Depends on `TKT-0267` to supply the underlying Session Manager tunnel capability.
- 2026-03-08 — PM/BA refinement after operator feedback:
  - Keep focus on SSM work by making this the next operator-facing slice after `TKT-0267`.
  - Explicit UX requirement:
    - an SSH host should be able to point at an existing SSM host as its route/tunnel host
    - the app should establish the tunnel automatically for the SSH session
    - operators should not need to create manual SSM port forwards just to use SSH-over-SSM
- 2026-03-08 — Slice A implemented and build-verified:
  - Added additive `ssm_route_host_id` persistence across host schema, editor state, and encrypted backup/import remapping.
  - SSH host editor now shows a separate `Route via SSM host` selector for SSH hosts only; existing `Jump via host` remains SSH-only.
  - SSH transport now:
    - opens an automatic SSM tunnel to the final SSH target when only an SSM route host is configured
    - opens the SSM tunnel to the SSH jump host first when both an SSM route host and SSH jump host are configured, then applies the existing SSH jump-forward path to the final target
    - closes the temporary SSM tunnel automatically when the SSH session closes
  - Observability: added non-secret SSH markers for SSM route host selection and route-stage transitions.
  - Verification:
    - Docker build passed: `references/logs/android_build_2026-03-08T16-54-45+02-00.log`
    - APKs packaged:
      - `references/builds/termbot-oss-debug-2026-03-08T16-54-45+02-00.apk`
      - `references/builds/termbot-google-debug-2026-03-08T16-54-45+02-00.apk`
  - Remaining before closeout:
    - operator smoke for direct SSH via selected SSM route host
    - regression smoke for direct SSH, direct SSM shell, and SSH jump-host behavior
    - optional combined `SSM route host + SSH jump host` topology smoke if available
- 2026-03-08 — Operator smoke confirmed and ticket closed:
  - Operator confirmed all requested `TKT-0268` scenarios are working on the `16:54` build:
    - direct SSH via selected SSM route host
    - direct SSH regression
    - direct SSM shell regression
    - SSH jump-host regression
  - `TKT-0269` remains the next visible SSM slice because the background/power-management issue still affects long-lived sessions.
  - Residual note:
    - combined `SSM route host + SSH jump host` topology remains an optional follow-up smoke, but it is not required for `TKT-0268` closeout
- 2026-03-09 — Optional combined-topology smoke confirmed:
  - Operator explicitly confirmed the combined path also works:
    - SSH host configured with `Route via SSM host`
    - the same SSH host also configured with `Jump via host`
    - runtime behavior matches ADR `0002`: TermBot uses SSM as the route path first, reaches the SSH jump host through that tunnel, and then reaches the final SSH target through the existing SSH jump flow
  - This closes the last unsmoked note attached to the `TKT-0268` routing-order design.
