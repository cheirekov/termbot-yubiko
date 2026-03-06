# Risk Register

Date: 2026-03-06
Scope: `repos/termbot-termbot` continuation work (post-TKT-0247)

Impact / Likelihood scale: 1 (low) .. 5 (high)

| Risk | Category | Impact | Likelihood | Evidence | Mitigation | Owner |
|---|---|---:|---:|---|---|---|
| Security-key auth regressions across provider/transport combinations (OpenPGP/PIV/FIDO2 x NFC/USB) | Reliability/Security | 5 | 3 | Security-key stack spans `SecurityKeyActivity`, `SecurityKeyService`, `securitykey/*`, and SSH transport; recent stabilization tickets indicate historically fragile behavior (`ai_docs/STATE.md`) | Maintain sentinel matrix execution for each RC and after auth-path changes; add lifecycle/auth trace assertions and stronger smoke logging discipline | QA + Lead Dev |
| CI signal fragmentation causes false confidence or delayed regressions | Delivery/Quality | 4 | 4 | Active configs exist in `.github/workflows/*`, `Jenkinsfile`, and `.travis.yml`; only part of this is currently user-confirmed green | Define canonical gate pipeline and mark legacy pipelines as non-blocking or retire; track in board as explicit migration tickets | DevOps/SRE |
| Legacy Android build stack constraints (AGP 4.2.2 / Gradle 6.7.1 / Java 8 source level) reduce modernization velocity | Maintainability | 4 | 3 | Versions are pinned in `app/build.gradle`; prior ticket history spent significant effort on compatibility and CI fixes | Plan staged modernization spike with compatibility guardrails and fast rollback path; avoid broad changes mixed with feature tickets | Architect + Lead Dev |
| Backup/export usability mistakes can cause perceived data loss | UX/Reliability | 4 | 2 | Backup flows were recently hardened in TKT-0239, indicating this area required corrective UX work | Add a focused negative-test checklist and import/export conflict scenarios to release smoke cadence | QA |
| Auth/session state coupling with jump-host and multi-step auth flows can regress silently | Reliability | 5 | 3 | TKT-0241 fixed a P0 state leak in jump-host path; sensitive state machine still complex in `transport/SSH.java` | Add targeted regression tickets for explicit auth-state invariants and bounded retries; keep patches small and instrumented | Lead Dev |
| Documentation/repo-map drift leads to wrong assumptions during continuation | Process | 3 | 4 | `ai_docs/config/repos.list` references `./repos/hwsecurity` but workspace has only `repos/termbot-termbot`; question recorded in `QUESTIONS.md` | Resolve ownership decision and update repo map/state docs in one controlled ticket | PM/BA + State Steward |
| Runtime observability coverage is mostly manual/export-based, limiting proactive detection | Operability | 3 | 3 | Debug report/export exists, but no external metrics/alerts pipeline documented in quality snapshot | Improve on-device structured markers and standard failure capture scripts; define minimum observability gate per release | Dev + QA |
