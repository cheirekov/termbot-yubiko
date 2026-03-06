# ADR 0001: SSM Transport Phased Introduction Strategy

- Date: 2026-03-06
- Status: Proposed
- Owners: yc

## Context
TermBot currently supports `ssh`, `telnet`, and `local` transports through `AbsTransport` + `TransportFactory`. The new SSM epic (`TKT-0260`) requires adding an AWS Session Manager-backed terminal transport while preserving existing transport behavior and keeping compatibility with current Android/Gradle constraints.

## Decision
Adopt a phased SSM implementation strategy:
1) Feasibility and contract spike (TKT-0261).
2) Minimal working SSM transport and stream backbone (TKT-0262).
3) Credential management and optional MFA extension points (TKT-0263).
4) Host editor/session UX integration (TKT-0264).
5) Persistence/backup compatibility (TKT-0265).

For initial implementation, prefer a thin in-app client approach (SigV4 + session stream adapter) over full AWS SDK bundling, pending unresolved protocol details captured in `ai_docs/docs/QUESTIONS.md`.

## Options considered
1) Full AWS SDK integration in the Android app.
2) Thin custom SSM client integrated with existing transport abstractions.
3) External SSM proxy service with app-side simplified transport.

## Consequences
- Positive:
  - Limits blast radius with incremental slices and explicit rollback points.
  - Keeps architecture aligned with current `AbsTransport` model.
  - Defers high-risk credential/MFA complexity until transport backbone is stable.
- Negative:
  - Requires disciplined sequencing and more ticket overhead.
  - Custom client path needs careful protocol validation before production confidence.
- Follow-ups:
  - Resolve open protocol/auth unknowns before finalizing TKT-0262 implementation details.
  - Promote this ADR to `Accepted` after first successful end-to-end SSM session smoke.

## References
- `ai_docs/tickets/TKT-0260-aws-ssm-session-manager-support-epic.md`
- `ai_docs/tickets/TKT-0261-ssm-transport-feasibility-spike.md`
- `ai_docs/docs/EXISTING/07_ssm_feasibility_spike.md`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/AbsTransport.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/transport/TransportFactory.java`
