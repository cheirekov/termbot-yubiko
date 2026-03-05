#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 3 ]; then
  echo "Usage: docker_run.sh <repo_path> <docker_image> <command>"
  echo "Example: docker_run.sh . node:22 "corepack enable && pnpm test""
  exit 1
fi

REPO_PATH="$1"
IMAGE="$2"
CMD="$3"

# Normalize path
REPO_PATH_ABS="$(cd "$REPO_PATH" && pwd)"

docker run --rm -t   -v "$REPO_PATH_ABS:/work"   -w /work   "$IMAGE"   bash -lc "$CMD"
