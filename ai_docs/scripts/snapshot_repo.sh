#!/usr/bin/env bash
set -euo pipefail
# Snapshot a repo for LLM context: git + tree + key files
# Usage: ./ai_docs/scripts/snapshot_repo.sh > SNAPSHOT.txt

echo "## Repo snapshot generated: $(date -Iseconds)"
echo
echo "### Git"
git rev-parse --show-toplevel 2>/dev/null || true
git rev-parse --abbrev-ref HEAD 2>/dev/null || true
git rev-parse HEAD 2>/dev/null || true
echo
echo "### Tree (4 levels)"
if command -v tree >/dev/null 2>&1; then
  tree -L 4 -a -I ".git|node_modules|dist|build|.next|.venv|__pycache__|target|.terraform" || true
else
  find . -maxdepth 4 -print | sed 's#^\./##' | sort
fi
echo
echo "### Key build files"
for f in README.md package.json pnpm-lock.yaml yarn.lock package-lock.json pyproject.toml requirements.txt go.mod Cargo.toml Makefile docker-compose.yml Dockerfile .github/workflows/*; do
  if ls $f >/dev/null 2>&1; then
    echo "--- $f"
    ls -la $f | head -n 5
  fi
done
