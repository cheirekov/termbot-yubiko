# STATE.md Protocol (Keep it small, keep it true)

## The two-layer approach
- **STATE.md** = “Now” (short, pasteable into any LLM session)
- **Supporting docs** = detail (architecture, requirements, ADRs, inventory, risk register)

STATE.md should mostly contain:
- pointers (links/paths) to details
- the top priorities, top risks, and current truth

## Size limits (recommended)
- Target: ≤ 200 lines
- Hard limit: ≤ 400 lines OR ≤ 25 KB

When you approach limits, **move detail out**:
- decisions → ADRs
- system details → ARCHITECTURE.md / inventory docs
- long lists → reference files in `references/` (snapshots, outputs)

## Update rule (after every meaningful action)
Any agent who:
- changes scope
- makes a decision
- discovers new repo truth
- completes/starts a ticket
must update STATE.md.

## “State Steward” role
Designate one person (or agent) as steward per session/day to avoid conflicting edits.
Steward responsibilities:
- enforce size limits
- keep top items prioritized
- maintain links to detailed docs

## Compaction procedure (shrinking)
Use **one** of these:

### A) Deterministic compaction script (recommended)
1) `ai_docs/scripts/state_compact.sh`
2) Review the compacted STATE.md
3) Ensure important items are still present and re-add if necessary
4) The archived previous state is in `references/state_archive/`

### B) LLM-assisted compaction (when STATE.md is messy)
1) Copy `ai_docs/prompts/workflows/state_maintenance.md` into the LLM
2) Provide:
   - current STATE.md
   - links to supporting docs
3) Ask for a compacted STATE.md within the size limits
4) Archive old STATE.md before replacing it

## Anti-hallucination rule
If something is uncertain, write “unknown” and add a question.
Never invent runtime/deploy, SLAs, or business rules.
