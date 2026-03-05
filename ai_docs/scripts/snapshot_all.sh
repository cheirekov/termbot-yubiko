#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CFG="$ROOT/ai_docs/config/repos.list"
OUT="$ROOT/references"

mkdir -p "$OUT"

if [ ! -f "$CFG" ]; then
  echo "Missing config: $CFG"
  exit 1
fi

echo "Using config: $CFG"
echo

while IFS='|' read -r name path role; do
  # skip comments/empty
  [[ -z "${name// }" ]] && continue
  [[ "$name" =~ ^# ]] && continue

  repo_path="$ROOT/$path"
  if [ ! -d "$repo_path" ]; then
    echo "⚠️  Repo path not found: $repo_path (name=$name role=$role)"
    continue
  fi

  echo "==> Snapshot: $name ($role) at $repo_path"
  mkdir -p "$OUT/$name"
  pushd "$repo_path" >/dev/null
  "$ROOT/ai_docs/scripts/snapshot_repo.sh" > "$OUT/$name/SNAPSHOT.txt" || true
  (git remote -v 2>/dev/null || true) > "$OUT/$name/GIT_REMOTE.txt"
  popd >/dev/null
done < "$CFG"

echo
echo "✅ Snapshots written under: $OUT/<repo>/SNAPSHOT.txt"
