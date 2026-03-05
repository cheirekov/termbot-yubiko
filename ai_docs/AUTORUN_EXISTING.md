# AUTORUN: Existing Project (One-command bootstrap + one instruction to the IDE agent)

Date: 2026-02-27

## What you do (minimal)
1) Configure multi-repo list (once):
   - `ai_docs/config/repos.list`
2) Fill (once, short):
   - `ai_docs/docs/EXISTING/00_intake.md`
3) Run bootstrap (one command):
```bash
ai_docs/scripts/bootstrap_existing.sh
```
4) In VS Code agent (Cline/Codex/Copilot Chat), send ONE message:
> Follow instructions in `references/prompt_bundles/existing_master.md` and write the requested outputs.

That's it.

---

## What the agent will do
- Workflow 01: create inventory/system map/quality snapshot + update QUESTIONS
- Workflow 02: create risk register, debt clusters, and tickets
- Workflow 03: create continuation plan and update delivery board
- Keep outputs in `ai_docs/docs/…`, `ai_docs/tickets/…`, `ai_docs/boards/…`

## Token discipline rules for the agent
- Start from snapshots in `references/<repo>/SNAPSHOT.txt`
- Only open source files when needed for a specific ticket
- Never paste huge files into chat; operate on workspace files directly


---

## Fast path: “Test APK today” (Android)
After initial bootstrap (or even before, if intake is filled):
```bash
ai_docs/scripts/bootstrap_test_build_android.sh
```

Then tell your IDE agent ONE message:
> Follow instructions in `references/prompt_bundles/test_build_android_master.md`.
