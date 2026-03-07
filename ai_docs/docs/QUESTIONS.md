# Open Questions (single source of truth)

Append questions here instead of guessing.

## Questions
- 2026-03-06: `ai_docs/config/repos.list` still includes `hwsecurity|./repos/hwsecurity|sdk`, but `repos/hwsecurity` is not present in workspace and STATE says YubiKit-only runtime. Should this entry be removed or retained as historical metadata?
- 2026-03-06: Which CI pipeline is canonical for merge/release gating going forward (GitHub Actions only, or GitHub Actions + Jenkins)? Current repo still contains legacy Travis/Jenkins files.
- 2026-03-06: For initial SSM release scope, should sessions that request `KMSEncryption` handshake be treated as unsupported (current behavior) or should we hard-require KMS handshake support before production rollout?
- 2026-03-06 [RESOLVED]: SSM initial credential mode is access key ID + secret access key first; IAM Identity Center is out of scope/backlog for now.
- 2026-03-06 [RESOLVED]: SSM initial release may ship without YubiKey MFA; optional MFA work remains in TKT-0263 follow-up.
- 2026-03-06 [RESOLVED by implementation scope]: TKT-0262 starts with shell-style StartSession bootstrap first; port-forward/document-specific sessions are deferred to later SSM slices.
- 2026-03-07 [RESOLVED by PM/BA]: `TKT-0263` delivery sequence is hybrid.
  - Slice A: backend credential foundation and secure storage hooks.
  - Slice B: temporary session credential support (`ASIA...` access key + session token) on the existing SSM host flow.
  - Follow-up slices: assume-role / account-jump UX and orchestration after the baseline session-credential mode is smoked.
- 2026-03-07 [RESOLVED by PM/BA + UX]: Role/account-jump inputs will use a mixed model.
  - Per-host SSM editor keeps direct session-scoped inputs only (access key, region, target, and temporary session-credential prompts).
  - Future multi-account role/jump orchestration should move into a shared AWS profile/account abstraction with per-host reference or override.
- 2026-03-07 [RESOLVED by scope]: `TKT-0263` phase 1 MFA path is session-token-only integration without in-app MFA prompts.
  - The app accepts externally issued temporary session credentials now.
  - TOTP/app-code and hardware-backed MFA remain follow-up work.
