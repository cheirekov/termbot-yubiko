#!/usr/bin/env bash
set -euo pipefail
# Creates a compact context pack in ./context_pack/ for a single repo root.
# Usage: ./ai_docs/scripts/make_context_pack.sh

OUT="context_pack"
rm -rf "$OUT"
mkdir -p "$OUT"

INCLUDE=(
  "README.md"
  "docs"
  "package.json"
  "pnpm-lock.yaml"
  "yarn.lock"
  "package-lock.json"
  "pyproject.toml"
  "requirements.txt"
  "go.mod"
  "Cargo.toml"
  "Makefile"
  "docker-compose.yml"
  "Dockerfile"
  ".github/workflows"
)

copy_item () {
  local item="$1"
  if [ -e "$item" ]; then
    mkdir -p "$OUT/$(dirname "$item")"
    cp -R "$item" "$OUT/$item"
  fi
}

for item in "${INCLUDE[@]}"; do
  copy_item "$item"
done

echo "Context pack created in $OUT/"
