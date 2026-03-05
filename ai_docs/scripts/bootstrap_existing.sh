#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> 1) Snapshot repos"
"$ROOT/ai_docs/scripts/snapshot_all.sh"

echo "==> 2) Ensure required files exist"
mkdir -p "$ROOT/ai_docs/docs/EXISTING"
touch "$ROOT/ai_docs/docs/EXISTING/00_intake.md"
touch "$ROOT/ai_docs/STATE.md"

echo "==> 3) Generate prompt bundle"
"$ROOT/ai_docs/scripts/make_prompt_bundle.sh" existing

echo
echo "NEXT (one instruction to your IDE agent):"
echo "  Follow instructions in: references/prompt_bundles/existing_master.md"
