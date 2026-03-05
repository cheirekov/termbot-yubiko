# Workflow: STATE.md Maintenance (Shrink + De-hallucinate)

ROLE: State Steward (can be Architect or PM/BA)

## Inputs
- Current ai_docs/STATE.md
- ai_docs/docs/QUESTIONS.md
- Any relevant new info (snapshots, command outputs)

## Tasks
1) Verify STATE.md contains only “true now” information (mark unknowns).
2) Reduce size:
   - Keep only top priorities, top risks, key decisions and pointers.
   - Move details to supporting docs (ARCHITECTURE, REQUIREMENTS, ADRs).
3) Ensure each section respects its LIMIT=.
4) Ensure open questions are synced with ai_docs/docs/QUESTIONS.md.
5) Output a compacted STATE.md.

## Output constraints
- ≤ 200 lines preferred (≤ 400 hard limit)
- Bullet items must be short (one line when possible)
- Use file links/paths instead of copying long text
