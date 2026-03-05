#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STATE="$ROOT/ai_docs/STATE.md"

MAX_LINES="${MAX_LINES:-250}"
MAX_BYTES="${MAX_BYTES:-25000}"

if [ ! -f "$STATE" ]; then
  echo "❌ Missing STATE.md at: $STATE"
  exit 2
fi

lines="$(wc -l < "$STATE" | tr -d ' ')"
bytes="$(wc -c < "$STATE" | tr -d ' ')"

ok=1
if [ "$lines" -gt "$MAX_LINES" ]; then
  echo "❌ STATE.md too large: $lines lines (max $MAX_LINES)"
  ok=0
else
  echo "✅ STATE.md lines: $lines / $MAX_LINES"
fi

if [ "$bytes" -gt "$MAX_BYTES" ]; then
  echo "❌ STATE.md too large: $bytes bytes (max $MAX_BYTES)"
  ok=0
else
  echo "✅ STATE.md bytes: $bytes / $MAX_BYTES"
fi

if [ "$ok" -ne 1 ]; then
  echo
  echo "Fix: run compaction:"
  echo "  ai_docs/scripts/state_compact.sh"
  echo
  echo "Or raise limits temporarily (not recommended):"
  echo "  MAX_LINES=350 MAX_BYTES=40000 ai_docs/scripts/state_check.sh"
  exit 1
fi
