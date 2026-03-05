#!/usr/bin/env bash
set -euo pipefail

# Android Docker build runner
# Usage:
#   ai_docs/scripts/android_docker_build.sh <repo_path> <gradle_task> [-- <extra gradle args>]
#
# Examples:
#   ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
#   ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot testDebugUnitTest -- --stacktrace

if [ $# -lt 2 ]; then
  echo "Usage: android_docker_build.sh <repo_path> <gradle_task> [-- <extra gradle args>]"
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPO_PATH="$1"
TASK="$2"
shift 2

EXTRA_ARGS=()
if [ "${1:-}" = "--" ]; then
  shift
  EXTRA_ARGS=("$@")
fi

IMAGE="${ANDROID_DOCKER_IMAGE:-ghcr.io/cirruslabs/android-sdk:34}"

REPO_ABS="$(cd "$ROOT/$REPO_PATH" && pwd)"

LOG_DIR="$ROOT/references/logs"
mkdir -p "$LOG_DIR"
ts="$(date -Iseconds | tr ':' '-')"
LOG_FILE="$LOG_DIR/android_build_${ts}.log"

# Caches (host dirs) to keep builds fast
GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}"
ANDROID_CACHE="${ANDROID_HOME_CACHE:-$HOME/.android}"

mkdir -p "$GRADLE_CACHE" "$ANDROID_CACHE"

# Detect gradlew
if [ ! -f "$REPO_ABS/gradlew" ]; then
  echo "❌ gradlew not found in repo: $REPO_ABS"
  echo "Expected: $REPO_ABS/gradlew"
  exit 2
fi

chmod +x "$REPO_ABS/gradlew" || true

echo "==> Android Docker build"
echo "Repo: $REPO_ABS"
echo "Image: $IMAGE"
echo "Task: $TASK ${EXTRA_ARGS[*]:-}"
echo "Log:  $LOG_FILE"
echo

# Run as current user to avoid root-owned build outputs
UIDGID="$(id -u):$(id -g)"

# Many android images already contain JDK + SDK.
# We keep it simple: just run gradlew.
set +e
docker run --rm -t   -u "$UIDGID"   -v "$REPO_ABS:/work"   -v "$GRADLE_CACHE:/home/gradle/.gradle"   -v "$ANDROID_CACHE:/home/gradle/.android"   -w /work   -e GRADLE_USER_HOME=/home/gradle/.gradle   "$IMAGE"   bash -lc "./gradlew $TASK ${EXTRA_ARGS[*]:-}" 2>&1 | tee "$LOG_FILE"
rc=${PIPESTATUS[0]}
set -e

if [ $rc -ne 0 ]; then
  echo
  echo "❌ Build failed (exit=$rc). Full log saved to:"
  echo "  $LOG_FILE"
  exit $rc
fi

echo
echo "✅ Build OK. Log saved to:"
echo "  $LOG_FILE"
