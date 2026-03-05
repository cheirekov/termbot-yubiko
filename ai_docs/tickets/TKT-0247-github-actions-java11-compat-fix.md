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
