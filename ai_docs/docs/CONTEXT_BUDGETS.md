# Context Budgets (Practical anti-token discipline)

## Default “paste set” for any AI session
1) ai_docs/STATE.md  (must be small)
2) The current ticket (ai_docs/tickets/TKT-xxxx...)
3) Relevant snapshot excerpt(s) from references/<repo>/SNAPSHOT.txt
4) Only the exact file snippets needed

## What NOT to paste by default
- Full test logs
- Entire large markdown docs
- Whole repo trees beyond 4 levels
- Multiple long stack traces without pruning

## Pattern: human runs, AI reads
- AI proposes: commands + expected outputs
- Human runs: commands
- Human pastes: trimmed outputs (top/bottom + errors)
