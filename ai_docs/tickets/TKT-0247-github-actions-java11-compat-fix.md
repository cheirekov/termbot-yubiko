# Ticket: TKT-0247 - GitHub Actions Java 11 compatibility fix

## Context capsule (must be complete)
### Goal
- Fix GitHub Actions Android debug build failure caused by Java/Gradle incompatibility.

### Scope
- In scope:
- Update root Android CI workflow to run Gradle with Java 11 instead of Docker image default Java 21.
- Keep APK and build-log artifact upload behavior.
- Out of scope:
- Docker image publishing for CI.
- Build system upgrades (Gradle/AGP major bump).

### Constraints
- Keep workflow simple and reproducible on `ubuntu-latest`.
- Preserve current output artifact paths.

### Target areas
- `.github/workflows/android-main-build.yml`

### Acceptance criteria
- [x] Behavior (implementation):
- Workflow sets up Temurin Java 11 and Android SDK packages required for compileSdk/targetSdk 34.
- Workflow runs `assembleDebug` successfully in a Java 11 environment.
- APK artifacts and logs are still uploaded.
- [ ] Tests (manual):
- User reruns GitHub Actions and confirms green build.
- [x] Docs:
- Ticket + board + state + plan updated.

### Verification
- Local workflow lint not executed.
- Expected fix for CI failure: `Unsupported class file major version 65`.
- Follow-up CI fix (2026-03-05): `android-actions/setup-android@v3` pulled cmdline-tools 16.0 (requires JDK17). Workflow now uses JDK17 for SDK setup and switches back to JDK11 for Gradle build.
- Follow-up CI fix (2026-03-05): AGP 4.2.2 expects `dx`; GitHub-installed build-tools 34.0.0 only has `d8`. Added `dx`/`dx.jar` symlink shim to `d8`/`d8.jar` in workflow.
