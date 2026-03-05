#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CFG="$ROOT/ai_docs/config/repos.list"
OUT="$ROOT/references"

mkdir -p "$OUT"

while IFS='|' read -r name path role; do
  [[ -z "${name// }" ]] && continue
  [[ "$name" =~ ^# ]] && continue

  repo_path="$ROOT/$path"
  if [ ! -d "$repo_path" ]; then
    echo "⚠️  Repo path not found: $repo_path (name=$name role=$role)"
    continue
  fi

  echo "==> Context pack: $name"
  mkdir -p "$OUT/$name"
  pushd "$repo_path" >/dev/null
  "$ROOT/ai_docs/scripts/make_context_pack.sh" >/dev/null || true
  # zip the context_pack folder into references
  if command -v zip >/dev/null 2>&1; then
    (cd "$repo_path" && zip -qr "$OUT/$name/${name}.contextpack.zip" context_pack) || true
  else
    echo "zip not found; leaving as folder at $repo_path/context_pack"
  fi
  popd >/dev/null
done < "$CFG"

echo "✅ Context packs stored under references/<repo>/"
