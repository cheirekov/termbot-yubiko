# Using with VS Code AI plugins (Codex / Cline / Copilot) and Chat LLMs

## General workflow
- Keep long-lived knowledge in `ai_docs/docs/` (not in chat memory).
- Paste **only** the relevant ticket + snapshot excerpt into the model.

## Recommended prompt pattern
1) Paste the agent role prompt from `ai_docs/prompts/agents/<ROLE>.md`
2) Paste ticket context capsule
3) Paste relevant snapshot excerpts or file snippets
4) Ask for either:
   - updated docs/tickets, or
   - a patch/diff within PR-size limits

## Token discipline
- Prefer “inventory first” then “small ticket”.
- Humans run commands; AI reads outputs.


## Paste discipline (important)
Default to pasting only:
- `ai_docs/STATE.md` (short)
- the current ticket
- relevant snapshots/snippets

Do NOT paste large docs unless the ticket needs them.


## STATE quality gate (recommended)
Run:
```bash
ai_docs/scripts/state_check.sh
```
If it fails:
```bash
ai_docs/scripts/state_compact.sh
```
