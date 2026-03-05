# Handbook — How to Use the AI Continuation Engineering Pack

Last updated: 2026-02-27

This handbook is designed for **VS Code + AI plugins** (Codex/Cline/Copilot) and for chat LLMs (ChatGPT/Claude).
It assumes you want **professional**, **low-hallucination**, **context-efficient** execution.

---

## 0) The mental model

### What belongs where
- `ai_docs/STATE.md` → **short “truth now”** (pasteable into any session)
- `ai_docs/docs/*` → requirements, architecture, ADRs, plans (detail lives here)
- `ai_docs/tickets/*` → small work units with a **context capsule**
- `references/` → snapshots, archives, command outputs, generated artifacts

### Token discipline
- The AI should not “run the world.” Humans run commands; AI reads outputs.
- Paste only the **default paste set** (STATE + ticket + relevant snippets).

---

## 1) Install patterns

### A) Monorepo
From repo root:
```bash
bash scripts/install_ai_pack.sh
```

### B) Multi-repo workspace
Create workspace root (any folder), then:
```bash
bash scripts/install_ai_pack.sh
```
Add repo paths in:
- `ai_docs/config/repos.list`

Then generate snapshots:
```bash
ai_docs/scripts/snapshot_all.sh
```

---

## 2) Daily operating routine (recommended)

### Start of day / session
1) Generate snapshots (monorepo: 1, multi-repo: all):
```bash
ai_docs/scripts/snapshot_all.sh
```

2) Update `ai_docs/STATE.md`:
- objectives (top 3–10)
- active tickets
- any new truth discovered

3) Run size gate:
```bash
ai_docs/scripts/state_check.sh
```
If it fails:
```bash
ai_docs/scripts/state_compact.sh
```

### During work
- Always work from a ticket in `ai_docs/tickets/`
- Keep PRs small (see HARD_RULES)
- After each meaningful result, update `STATE.md`

### End of day
- Ensure board reflects reality (`ai_docs/boards/DELIVERY_BOARD.md`)
- Compact state if it grew

### Session handoff capsule (2-minute, when pausing work)
Copy this template into your final session note:
```md
Handoff date: YYYY-MM-DD
Last known good build:
- Command: <command>
- Result: <pass/fail + duration>
- Log: <path or n/a>
Active risks:
- <risk 1>
- <risk 2>
Open Review items:
- <TKT-id + short status>
Next actions (1-3):
1) <next action>
2) <next action>
3) <optional next action>
```

Example (2026-03-05):
```md
Handoff date: 2026-03-05
Last known good build:
- Command: ANDROID_DOCKER_IMAGE=termbot-android-sdk34-jdk11-agp422:local ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
- Result: PASS (BUILD SUCCESSFUL in 32s)
- Log: references/logs/android_build_2026-03-04T17-30-28+02-00.log
Active risks:
- Review queue is above WIP target (5 open review tickets; freeze on new Review entries is active).
- UI consistency gaps still exist between auth and import key flows.
Open Review items:
- TKT-0243 desktop mode smoke pending
- TKT-0244 FIDO2 USB parity smoke pending
- TKT-0245/0246/0247 publication + CI validation pending
Next actions (1-3):
1) Rerun GitHub Actions after TKT-0247 and confirm green assembleDebug.
2) Drain review queue oldest-first: TKT-0243 -> TKT-0244 -> TKT-0245 -> TKT-0246.
3) Execute TKT-0251 regression sentinel matrix.
```

---

## 3) Workflow: Existing Project (Continuation Engineering)

### Step 1 — Intake
Fill:
- `ai_docs/docs/EXISTING/00_intake.md`

This is where you capture:
- business goals (what matters)
- runtime truth (where it runs)
- known pains (incidents/cost/security)
- constraints (“must not break”)

### Step 2 — Inventory & system map
Use:
- `ai_docs/prompts/workflows/existing_project_01_inventory.md`

Output goes to:
- `ai_docs/docs/EXISTING/01_inventory.md`
- `ai_docs/docs/EXISTING/02_system_map.md`
- `ai_docs/docs/EXISTING/03_quality_snapshot.md`
- open questions → `ai_docs/docs/QUESTIONS.md`

### Step 3 — Risk & debt review
Use:
- `ai_docs/prompts/workflows/existing_project_02_risk_review.md`

Outputs:
- risk register, debt clusters, mitigation tickets

### Step 4 — Continuation plan & board
Use:
- `ai_docs/prompts/workflows/existing_project_03_continuation_plan.md`

Outputs:
- 2–6 week plan, milestones, DoD, board ready tickets

---

## 4) Workflow: New Project (Tech-adaptive)

### Step 1 — Discovery
Use:
- `ai_docs/prompts/workflows/new_project_01_discovery.md`

Outputs:
- business brief, requirements, risk register, backlog tickets

### Step 2 — Architecture + technology selection
Use:
- `ai_docs/prompts/workflows/new_project_02_architecture.md`

Important: this step must produce:
- 2–3 stack options
- a recommended stack
- ADRs for major choices

### Step 3 — Execution plan + quality gates
Use:
- `ai_docs/prompts/workflows/new_project_03_execution_plan.md`

Outputs:
- plan, release approach, quality plan, tickets/board

---

## 5) How to run agents in VS Code plugins

### Suggested “copy/paste pack” per agent session
Paste (in this order):
1) `ai_docs/prompts/agents/<ROLE>.md`
2) `ai_docs/STATE.md`
3) the current ticket (context capsule)
4) small relevant snippets:
   - `references/<repo>/SNAPSHOT.txt` excerpt
   - specific file excerpts (only what matters)

### Avoid
- pasting giant logs or whole docs by default
- pasting multiple repos at once unless the ticket is cross-repo

---

## 6) Testing strategy (clean host)

### Default: Docker-first
Use:
- `ai_docs/scripts/docker_run.sh`

Example:
```bash
ai_docs/scripts/docker_run.sh . node:22 "corepack enable && pnpm test"
```

### Token-efficient verification
- Prefer short commands that give clear pass/fail
- If tests are heavy, define a **manual smoke script** and keep it documented in the ticket’s Verification section

Docs:
- `ai_docs/docs/DOCKER_TESTING.md`

---

## 7) STATE.md maintenance (shrinking)

### When it grows
Run:
```bash
ai_docs/scripts/state_check.sh
```

If it fails:
```bash
ai_docs/scripts/state_compact.sh
```

If it’s messy and needs de-hallucination:
- use `ai_docs/prompts/workflows/state_maintenance.md`

Rule of thumb:
- STATE = links + top items
- details move out to docs/ADRs

---

## 8) What to send the AI when you “test the template” with real repos

Best:
- zip each repo and upload here
- or upload `references/<repo>/SNAPSHOT.txt` + a few critical files

Minimal pack for a first pass:
1) `ai_docs/STATE.md` (even a rough first version)
2) `ai_docs/docs/EXISTING/00_intake.md` (goals + constraints)
3) Snapshot(s) (`references/<repo>/SNAPSHOT.txt`)

Then we can produce:
- inventory/system map
- risk register
- continuation plan
- a ready delivery board + tickets

---


---

## AUTORUN mode (minimal prompting)

If you don’t want copy/paste prompting loops, use AUTORUN.

### Existing project (multi-repo or monorepo)
1) Configure `ai_docs/config/repos.list`
2) Fill `ai_docs/docs/EXISTING/00_intake.md`
3) Run:
```bash
ai_docs/scripts/bootstrap_existing.sh
```
4) In your IDE agent, send ONE message:
> Follow instructions in `references/prompt_bundles/existing_master.md` and write the requested outputs.

### New project
1) Fill `ai_docs/docs/NEW/00_business_brief.md`
2) Run:
```bash
ai_docs/scripts/bootstrap_new.sh
```
3) In your IDE agent, send ONE message:
> Follow instructions in `references/prompt_bundles/new_master.md` and write the requested outputs.


---

## Android “Test build today” mode (Docker-first)

If your immediate goal is to produce a debug APK quickly (without planning ceremony):

1) Ensure `ai_docs/docs/EXISTING/00_intake.md` is filled (short is fine).
2) Run:
```bash
ai_docs/scripts/bootstrap_test_build_android.sh
```
3) In your IDE agent, send ONE message:
> Follow `references/prompt_bundles/test_build_android_master.md`.

This instructs the agent to pick a minimal fix slice (1–3 tickets), patch, and build with:
- `ai_docs/scripts/android_docker_build.sh`

Full logs go to:
- `references/logs/`
and short build summary goes to:
- `ai_docs/docs/BUILD_STATUS.md`


---

## Hard “start/end” rules (prevents agent forgetting)

Read:
- `ai_docs/docs/WORKFLOW_CONTRACT.md`
- `ai_docs/AGENT_MEMORY.md`

After each ticket/slice the agent must run:
```bash
ai_docs/scripts/closeout_check.sh TKT-XXXX
```

If the check fails, it means documentation/board updates were missed.
