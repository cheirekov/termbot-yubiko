#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STATE="$ROOT/ai_docs/STATE.md"
ARCHIVE_DIR="$ROOT/references/state_archive"

mkdir -p "$ARCHIVE_DIR"

if [ ! -f "$STATE" ]; then
  echo "STATE.md not found at: $STATE"
  exit 1
fi

ts="$(date -Iseconds | tr ':' '-')"
cp "$STATE" "$ARCHIVE_DIR/STATE_${ts}.md"

# Compact: limit bullet-like lines per section if header contains (LIMIT=N)
# Keeps heading lines, keeps non-bullet lines, trims bullet/numbered lines beyond limit.
tmp="$(mktemp)"
awk '
function parse_limit(line,   m) {
  limit = -1
  if (match(line, /LIMIT=([0-9]+)/, m)) limit = m[1] + 0
}
function is_bullet(line) {
  return (line ~ /^- / || line ~ /^[0-9]+\) / || line ~ /^[0-9]+\. /)
}
BEGIN { limit=-1; count=0; in_section=0; }
{
  if ($0 ~ /^## /) {
    in_section=1
    count=0
    parse_limit($0)
    print $0
    next
  }
  if (!in_section) { print $0; next }
  if (limit < 0) { print $0; next }
  if (is_bullet($0)) {
    count++
    if (count <= limit) print $0
    next
  }
  # Always keep non-bullet lines (blank lines, notes, etc.)
  print $0
}
' "$STATE" > "$tmp"

mv "$tmp" "$STATE"

echo "✅ STATE compacted."
echo "Archived previous state to: $ARCHIVE_DIR/STATE_${ts}.md"
