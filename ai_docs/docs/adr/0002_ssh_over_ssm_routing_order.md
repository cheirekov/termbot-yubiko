# ADR 0002: SSH over SSM Routing Order with Optional SSH Jump Host

- Date: 2026-03-08
- Status: Accepted
- Owners: yc

## Context
`TKT-0268` adds a new SSH-only `Route via SSM host` path while the app already supports SSH `Jump via host` (`ProxyJump`) from `TKT-0215`. Once both controls exist, the routing order must be explicit so the app does not guess or silently fall back to an unsafe path.

## Decision
Keep SSH jump hosts and SSM route hosts as separate controls with defined composition:
1) If only `Route via SSM host` is configured, open an SSM tunnel to the final SSH target and run SSH over the localhost tunnel.
2) If both `Route via SSM host` and `Jump via host` are configured, open the SSM tunnel to the SSH jump host first, authenticate the jump host over that tunnel, then apply the existing SSH local-forward jump to the final target.
3) Do not require or reuse manual SSM port-forward entries for this path.

## Options considered
1) Make SSH jump hosts and SSM route hosts mutually exclusive.
2) Overload the existing `Jump via host` field to accept either SSH or SSM hosts.
3) Keep them separate and define the routing order explicitly.

## Consequences
- Positive:
  - Preserves the existing SSH `Jump via host` behavior without changing its meaning.
  - Lets operators combine an SSM-managed bastion reachability path with an SSH jump host when both are genuinely needed.
  - Avoids manual pre-created SSM port forwards for SSH-over-SSM sessions.
- Negative:
  - Adds one more host-editor control for SSH hosts.
  - Leaves more complex nested SSM hop scenarios out of scope for now.
- Follow-ups:
  - Operator smoke verified:
    - direct SSH via SSM route host
    - SSH jump host regression
    - combined SSM route host + SSH jump host
  - `TKT-0269` must harden lifecycle/background behavior before deeper multi-hop expansion.

## References
- `ai_docs/tickets/TKT-0268-ssh-over-ssm-bastion-tunnel-integration.md`
- `ai_docs/tickets/TKT-0269-background-power-management-session-resilience.md`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSH.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/SSM.java`
