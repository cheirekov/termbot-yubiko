#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUNDLES_DIR="$ROOT/references/prompt_bundles"
mkdir -p "$BUNDLES_DIR"

mode="${1:-existing}"
out="$BUNDLES_DIR/${mode}_master.md"

cat > "$out" <<'EOF'
# Prompt Bundle (MASTER)

You are operating inside a local workspace/repo using an IDE agent (Cline/Codex/Copilot Chat).
Do NOT ask the user to paste files into chat. Instead, open and read the files by path.

## Non-negotiable rules
- Docker-first builds/tests. Do NOT request installing toolchains on host; use ai_docs/scripts/* docker runners.
- Follow: ai_docs/docs/HARD_RULES.md
- Use only evidence from workspace files.
- If information is missing, append questions to ai_docs/docs/QUESTIONS.md and stop that branch.
- Keep changes small and reviewable.
- Never output secrets.

## Default files you must open first
0) ai_docs/AGENT_MEMORY.md
0.1) ai_docs/docs/WORKFLOW_CONTRACT.md
1) ai_docs/STATE.md
2) ai_docs/docs/EXISTING/00_intake.md   (for existing) OR ai_docs/docs/NEW/00_business_brief.md (for new)
3) All snapshots under references/*/SNAPSHOT.txt

---

EOF

if [ "$mode" = "existing" ]; then
cat >> "$out" <<'EOF'
## WORKFLOW: Existing Project (Continuation Engineering)

### Closeout ritual (MANDATORY after each ticket/slice)
After implementing any ticket, you MUST:
- update DELIVERY_BOARD.md
- update BUILD_STATUS.md if builds/tests run
- update QUESTIONS.md if unknowns found
- update STATE.md only if truth changed
- run: ai_docs/scripts/closeout_check.sh <TICKET_ID>


### Step 1 — Inventory & System Map
Open:
- ai_docs/prompts/workflows/existing_project_01_inventory.md
- references/*/SNAPSHOT.txt

Write/Update:
- ai_docs/docs/EXISTING/01_inventory.md
- ai_docs/docs/EXISTING/02_system_map.md
- ai_docs/docs/EXISTING/03_quality_snapshot.md
- ai_docs/docs/QUESTIONS.md (append questions)

### Step 2 — Risk review, debt clusters, tickets
Open:
- ai_docs/prompts/workflows/existing_project_02_risk_review.md
- ai_docs/docs/EXISTING/01_inventory.md
- ai_docs/docs/EXISTING/02_system_map.md
- ai_docs/docs/EXISTING/03_quality_snapshot.md
- ai_docs/docs/QUESTIONS.md
- ai_docs/tickets/TEMPLATE.md

Write:
- ai_docs/docs/EXISTING/04_risk_register.md
- ai_docs/docs/EXISTING/05_debt_clusters.md
- 8–15 tickets in ai_docs/tickets/ (TKT-02xx_*.md)

### Step 3 — Continuation plan + board
Open:
- ai_docs/prompts/workflows/existing_project_03_continuation_plan.md
- ai_docs/docs/EXISTING/04_risk_register.md
- ai_docs/docs/EXISTING/05_debt_clusters.md
- ai_docs/tickets/*.md (only the new ones)

Write/Update:
- ai_docs/docs/EXISTING/06_continuation_plan.md
- ai_docs/docs/DEFINITION_OF_DONE.md
- ai_docs/boards/DELIVERY_BOARD.md

### Step 4 — Start Slice 1 (optional if user requested a test build today)
If the user wants a test build immediately:
- Choose the smallest 1–3 tickets that fix the main failure mode (timeouts, lifecycle race, fail-fast error).
- Open only the exact source files needed.
- Produce minimal patches and list exact build/install commands.
EOF
else
cat >> "$out" <<'EOF'
## WORKFLOW: New Project (Tech-adaptive)

### Step 1 — Discovery & requirements
Open:
- ai_docs/prompts/workflows/new_project_01_discovery.md
- ai_docs/docs/NEW/00_business_brief.md

Write:
- ai_docs/docs/REQUIREMENTS.md
- ai_docs/docs/RISK_REGISTER.md
- 10–25 tickets in ai_docs/tickets/

### Step 2 — Architecture + technology selection
Open:
- ai_docs/prompts/workflows/new_project_02_architecture.md
- ai_docs/docs/REQUIREMENTS.md

Write:
- ai_docs/docs/ARCHITECTURE.md
- ADRs under ai_docs/docs/adr/
- ai_docs/docs/SECURITY.md
- ai_docs/docs/OBSERVABILITY.md

### Step 3 — Execution plan + quality gates
Open:
- ai_docs/prompts/workflows/new_project_03_execution_plan.md

Write:
- ai_docs/docs/PLAN.md
- ai_docs/docs/QUALITY_PLAN.md
- ai_docs/docs/RELEASE.md
- ai_docs/boards/DELIVERY_BOARD.md
EOF
fi

echo "✅ Wrote prompt bundle: $out"
