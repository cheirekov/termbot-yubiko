#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> 1) Ensure required files exist"
mkdir -p "$ROOT/ai_docs/docs/NEW"
touch "$ROOT/ai_docs/docs/NEW/00_business_brief.md"
touch "$ROOT/ai_docs/STATE.md"

echo "==> 2) Generate prompt bundle"
"$ROOT/ai_docs/scripts/make_prompt_bundle.sh" new

echo
echo "NEXT (one instruction to your IDE agent):"
echo "  Follow instructions in: references/prompt_bundles/new_master.md"
