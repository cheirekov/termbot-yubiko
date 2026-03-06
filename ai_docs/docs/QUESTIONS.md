# Open Questions (single source of truth)

Append questions here instead of guessing.

## Questions
- 2026-03-06: `ai_docs/config/repos.list` still includes `hwsecurity|./repos/hwsecurity|sdk`, but `repos/hwsecurity` is not present in workspace and STATE says YubiKit-only runtime. Should this entry be removed or retained as historical metadata?
- 2026-03-06: Which CI pipeline is canonical for merge/release gating going forward (GitHub Actions only, or GitHub Actions + Jenkins)? Current repo still contains legacy Travis/Jenkins files.
- 2026-03-06: For SSM initial release scope, which credential mode must be supported first: static access key + session token, profile-based temporary creds, or IAM Identity Center device flow?
- 2026-03-06: Should SSM phase 1/2 target shell-only Session Manager sessions first, with port-forward/document sessions explicitly deferred?
- 2026-03-06: Is it acceptable for first SSM delivery to skip YubiKey MFA integration and ship explicit extension points only (to be completed in TKT-0263 follow-up)?
