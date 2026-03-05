# Risk Register

| Risk | Category | Impact | Likelihood | Evidence | Mitigation | Owner |
|---|---|---:|---:|---|---|---|
| OpenPGP auth regression due to wrong PIN verification mode/state | Security-key auth | High | Medium | Prior failures with APDU `0x6982` before TKT-0231 | Keep provider-specific auth tests + debug markers; preserve `verifyUserPin(..., true)` for AUTH flow | Android Eng |
| Connection lifecycle misuse in YubiKit callbacks | Platform integration | High | Medium | Prior `Call connect() first!`/stale connection failures before TKT-0229 | Require on-demand `openConnection(...)` for operations; add code review checklist item | Android Eng |
| UX confusion during hardware-key interaction (tap/USB prompt too subtle) | UX | Medium | High | User feedback: current indicator too small vs prior SDK dialog | Execute TKT-0233 with prominent guidance panel and usability review | UX Team |
| Inconsistent PIN input behavior across providers | UX/Security | Medium | Medium | OpenPGP numeric keypad vs PIV/FIDO2 text input | Execute TKT-0232 and document provider PIN policy | Android Eng |
| Release quality drift after rapid iteration | Process | High | Medium | Multiple hotfix tickets across short period | Complete TKT-0235 release-readiness gate + smoke matrix before RC | PM + Eng |
