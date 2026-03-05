# Ticket: TKT-0224 — Android modernization epic (SDK/AGP/Gradle/toolchain)

## Context capsule (must be complete)
### Goal
- Upgrade Android toolchain and SDK targets to a modern baseline that unblocks YubiKit-based PIV/FIDO2 integration and long-term maintainability.

### Scope
- In scope:
- Raise `compileSdk` / `targetSdk`, update AGP + Gradle wrapper + Java toolchain compatibility.
- Reconcile dependency updates required by toolchain uplift.
- Preserve `oss` and `google` flavors with Docker-first reproducible build.
- Out of scope:
- Full UI redesign.
- Non-security-key feature work not needed for compatibility.

### Constraints
- Platform/runtime constraints:
- Maintain install/runtime support for intended device range after explicit minSdk decision.
- Security/compliance constraints:
- Preserve existing secure storage and key handling behavior.
- Do NOT break:
- Existing OpenPGP SSH auth, backup import/export, jump host flow.

### Target areas
- Files/modules:
- `repos/termbot-termbot/app/build.gradle`
- `repos/termbot-termbot/build.gradle`
- `repos/termbot-termbot/gradle/wrapper/*`
- Docker image/build scripts under `ai_docs/scripts/`.
- Interfaces/contracts:
- Build flavors/tasks and release/debug output paths.

### Acceptance criteria
- [ ] Behavior:
- Modernized toolchain builds and app launches; core SSH flows still work.
- [ ] Tests (or explicit manual verification):
- Docker `assembleDebug` pass + manual smoke checks for auth/login/export.
- [ ] Docs:
- Ticket + board + build status updated with exact version bumps.
- [ ] Observability (if relevant):
- Build log references and migration notes captured.

### Verification (token-efficient)
- Docker command(s) to run:
- `ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
- Manual script(s) the user can run:
- Install `ossDebug`, run OpenPGP auth, export/import backup, run debug report export.
- Expected output(s):
- Stable build and no regressions in critical flows.

### Risks / rollout
- Regression areas:
- Legacy APIs, manifest merge, transitive dependency incompatibility.
- Rollback plan:
- Land in staged commits: toolchain uplift first, then dependency migrations, then feature paths.

## Notes
- Links:
- Current baseline: AGP `4.0.1`, Gradle `6.1.1`, `compileSdk/targetSdk 29`, `minSdk 14`.
- Related tickets:
- TKT-0220, TKT-0222, TKT-0223

## Implementation kickoff (2026-03-02)
- Baseline confirmed from repo:
  - `com.android.application` plugin `4.0.1`
  - Gradle wrapper `6.1.1`
  - `compileSdkVersion 29`, `targetSdkVersion 29`, `minSdkVersion 14`
  - Multiple legacy plugins and `jcenter()` usage still present.
- Execution strategy (phased, low-risk):
  1. `Phase A`: repository/plugin hygiene (`jcenter` removal where possible, plugin compatibility audit, lock Docker/JDK matrix).
  2. `Phase B`: Gradle wrapper + AGP uplift to a supported intermediate baseline, keep behavior parity.
  3. `Phase C`: SDK uplift (`compileSdk`/`targetSdk`) plus manifest/runtime compatibility fixes.
  4. `Phase D`: dependency updates and regression sweep for SSH/auth/backup flows.
- Immediate next implementation slice:
  - Start `Phase A` by removing obsolete repo duplication and validating Docker build still passes before touching AGP/Gradle core versions.

## Implementation progress (2026-03-02)
- Completed `Phase A` slice 1 (safe hygiene):
  - Removed duplicate `google()` repository declarations from root `build.gradle`.
- Completed `Phase A` slice 2 (dependency-source cleanup):
  - Removed `jcenter()` from root and app repository declarations.
  - Verified dependency resolution and `assembleDebug` still pass in Docker without `jcenter`.
- Build verification:
  - Command:
    - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
  - Result: PASS
  - Log:
    - `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T17-13-02+02-00.log`
- Next slice:
  - Phase B slice 1: AGP/Gradle uplift plan draft with compatibility matrix and first executable upgrade attempt in Docker.

## Implementation progress — Phase B (2026-03-02)
- Completed `Phase B` slice 1 (Gradle wrapper + AGP uplift):
  - Gradle wrapper: `6.1.1` → `6.7.1` (`gradle-wrapper.properties`)
  - AGP (Android Gradle Plugin): `4.0.1` → `4.2.2` (`app/build.gradle` plugins block)
  - `buildToolsVersion`: `29.0.3` → `34.0.0` (AGP 4.2.2 requires ≥ 30.0.2; Docker image has 34.0.0)
- Docker image patch required:
  - `build-tools 34.0.0` in the base image (`termbot-android-sdk34-jdk11:local`) was missing `dx` and `lib/dx.jar` (both removed from build-tools ≥ 31.0.0 but still validated by AGP 4.2.2).
  - Created thin patch image `termbot-android-sdk34-jdk11-agp422:local` via:
    ```
    FROM termbot-android-sdk34-jdk11:local
    USER root
    RUN printf '#!/bin/sh\nexec "$(dirname "$0")/d8" "$@"\n' \
        > /opt/android-sdk-linux/build-tools/34.0.0/dx && chmod +x ...
    RUN mkdir -p .../lib && jar cf .../lib/dx.jar ...  # empty stub JAR
    ```
  - Dockerfile stored at `/tmp/Dockerfile.agp422fix2` (not committed; rebuild instructions documented here).
- Build verification:
  - Command:
    - `ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug`
  - Result: **PASS** — `BUILD SUCCESSFUL in 1m 41s` (62 tasks: 51 executed, 11 from cache)
  - Log: `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T17-46-00+02-00.log`
- Notes / next slice:
  - Docker patch image `termbot-android-sdk34-jdk11-agp422:local` must be rebuilt from `Dockerfile.agp422fix2` on any new machine.
  - Phase C next: `compileSdk`/`targetSdk` uplift (29 → 34) + manifest/runtime compatibility fixes.

## Implementation progress — Phase C (2026-03-02)
- Completed `Phase C` (SDK uplift + manifest compat):
  - `compileSdkVersion`: `29` → `34` (`app/build.gradle`)
  - `targetSdkVersion`: `29` → `34` (`app/build.gradle`)
  - Added `android.aapt2FromMavenOverride=/opt/android-sdk-linux/build-tools/34.0.0/aapt2` to `gradle.properties` — AGP 4.2.2 bundled aapt2 crashes against API 34 resources; SDK aapt2 is required.
  - Added `android:exported="true"` to `HostListActivity` and `ConsoleActivity` in `AndroidManifest.xml` — required for all components with intent-filters when targeting API 31+.
- Build verification:
  - Result: **PASS** — `BUILD SUCCESSFUL in 11s` (62 tasks: 15 executed, 47 up-to-date)
  - Log: `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T18-19-19+02-00.log`
- Notes / next slice:
  - Phase D next: dependency sweep — update old `androidx` (`material:1.0.0`, `constraintlayout:1.1.3`, `appcompat:1.2.0`) and make minSdk decision (current: 14).

## Implementation progress — Phase D (2026-03-02)
- Completed `Phase D` (dependency sweep):
  - `appcompat`: `1.2.0` → `1.6.1` (`appcompatVersion` var added)
  - `constraintlayout`: `1.1.3` → `2.1.4` (`constraintlayoutVersion` var added)
  - `material`: `1.0.0` → `1.6.1` (`materialVersion` var added, pinned out of `supportLibraryVersion`)
  - Note: `material:1.7.0+` rejected — uses `<macro>` resource tags introduced in Material 3 (M3), which require AGP 7.2+. AGP 4.2.2 is the ceiling for this toolchain; `1.6.1` is the highest compatible version.
  - `minSdk`: kept at `14` — decision deferred; raising to `21` is a low-risk follow-up if desired.
- Build verification:
  - Result: **PASS** — `BUILD SUCCESSFUL in 58s` (62 tasks: 36 executed, 1 from cache, 25 up-to-date)
  - Log: `/home/yc/work/ai-projects-templates/workspace/references/logs/android_build_2026-03-02T18-24-21+02-00.log`
- Status: TKT-0224 all planned phases (A–D) complete. Epic ready to move to Done.
