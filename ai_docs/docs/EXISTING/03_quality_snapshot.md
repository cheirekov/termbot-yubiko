# Quality Snapshot

Date: 2026-03-06
Evidence basis: `ai_docs/docs/BUILD_STATUS.md`, `repos/termbot-termbot/Jenkinsfile`, `repos/termbot-termbot/.github/workflows/*.yml`, `repos/termbot-termbot/.travis.yml`, `repos/termbot-termbot/app/build.gradle`, `ai_docs/STATE.md`

## Testing
- Repeated Docker-based `assembleDebug` runs are documented as green through 2026-03-06 (`ai_docs/docs/BUILD_STATUS.md`).
- Project has unit and instrumentation test scaffolding (`app/src/test`, `app/src/androidTest`) and Gradle targets in CI scripts (`check`, `connectedCheck`, `lint`, `jacocoTestReport`).
- Manual smoke discipline exists for security-key flows (`SK-01..SK-08` referenced in `ai_docs/STATE.md`) but is process-driven rather than fully automated.

## CI/CD
- CI definitions are fragmented across:
  - GitHub Actions (translation export/import workflows)
  - Jenkins pipeline (`Jenkinsfile`) with assemble/check/device-test/coverage stages
  - Travis config (`.travis.yml`) with emulator-heavy flow
- Current user-confirmed signal in state/board: GitHub Actions green for recent publication and Java compatibility fixes.
- Build reproducibility is improved by workspace Docker wrapper (`ai_docs/scripts/android_docker_build.sh`) and pinned local Docker image convention.

## Observability
- In-app diagnostics are present and productized:
  - Security-key debug log ring buffer
  - Export Debug Report flow for field troubleshooting
- Recent tickets emphasize non-secret debug markers for auth/import/wait states.
- Observability remains primarily app-log/report centric; no external telemetry/metrics pipeline is documented.

## Security
- Positive controls in documented direction:
  - YubiKit runtime path is authoritative (`ai_docs/STATE.md`)
  - Explicit policy to avoid logging secrets/PIN/private material (`README.md`, hard rules)
  - Continued hardening around auth state transitions, timeout/fail-fast behavior, and PIN input policy (multiple completed tickets)
- Risk-sensitive areas still active:
  - Transport/lifecycle race conditions across NFC/USB paths
  - Complex multi-provider auth behavior in one legacy app architecture

## Debt highlights
- **CI debt:** simultaneous legacy and modern CI definitions can drift and reduce trust in one source of truth.
- **Architecture debt:** security-key integration spans activity/service/transport layers with lifecycle complexity.
- **UX consistency debt:** broad modernization done, but state indicates remaining tail items in TKT-0226 epic.
- **Process debt:** repo map still references an absent `repos/hwsecurity` path in config, creating documentation ambiguity.
