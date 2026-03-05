#!/usr/bin/env bash
set -euo pipefail

# Mechanical gate to prevent “LLM forgetting” to update docs.
# Usage:
#   ai_docs/scripts/closeout_check.sh TKT-0201
#
# The gate is conservative: it checks that key artifacts exist and
# that the ticket appears on the delivery board somewhere.
# It also enforces STATE size gate.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TICKET_ID="${1:-}"

fail () {
  echo "❌ $1"
  exit 1
}

echo "==> Closeout check (workspace): $ROOT"
echo

# Required files
req=(
  "$ROOT/ai_docs/docs/HARD_RULES.md"
  "$ROOT/ai_docs/docs/WORKFLOW_CONTRACT.md"
  "$ROOT/ai_docs/STATE.md"
  "$ROOT/ai_docs/boards/DELIVERY_BOARD.md"
  "$ROOT/ai_docs/docs/QUESTIONS.md"
  "$ROOT/ai_docs/docs/HOST_DEPS.md"
)
for f in "${req[@]}"; do
  [ -f "$f" ] || fail "Missing required file: $f"
done
echo "✅ Required files exist"

# STATE size gate
"$ROOT/ai_docs/scripts/state_check.sh" >/dev/null
echo "✅ STATE size gate passed"

# Ticket existence + board presence (if provided)
if [ -n "$TICKET_ID" ]; then
  # find ticket file (any matching)
  ticket_file="$(ls "$ROOT/ai_docs/tickets/${TICKET_ID}"*.md 2>/dev/null | head -n 1 || true)"
  [ -n "$ticket_file" ] || fail "Ticket file not found for id: $TICKET_ID in ai_docs/tickets/"
  echo "✅ Ticket file exists: $(basename "$ticket_file")"

  if grep -Rqs "$TICKET_ID" "$ROOT/ai_docs/boards/DELIVERY_BOARD.md"; then
    echo "✅ Ticket appears on DELIVERY_BOARD.md"
  else
    fail "Ticket id not found on DELIVERY_BOARD.md (move it to Ready/In Progress/Review/Done)"
  fi
fi

# Build status is optional but recommended; check presence and size
bs="$ROOT/ai_docs/docs/BUILD_STATUS.md"
if [ -f "$bs" ]; then
  bytes="$(wc -c < "$bs" | tr -d ' ')"
  if [ "$bytes" -lt 40 ]; then
    echo "⚠️  BUILD_STATUS.md is very small; if you ran builds/tests, update it."
  else
    echo "✅ BUILD_STATUS.md present"
  fi
else
  echo "⚠️  BUILD_STATUS.md missing; create it if builds/tests are run."
fi

echo
echo "✅ Closeout check passed"
