# Prompt Bundle (TEST BUILD — ANDROID)

You are an IDE agent (Codex/Cline/Copilot Chat) operating inside a local workspace.
Do not ask the user to paste files. Read and edit files by path.

## Hard rules
- Follow: ai_docs/docs/HARD_RULES.md
- Docker-first builds/tests. Do NOT request installing Android SDK/Gradle on host.
- Use: ai_docs/scripts/android_docker_build.sh for builds/tests.
- Keep changes minimal (1–3 small tickets) aimed at producing a test APK today.
- Never log secrets/PINs/key material.

## What to open first
0) ai_docs/AGENT_MEMORY.md
0.1) ai_docs/docs/WORKFLOW_CONTRACT.md

1) ai_docs/STATE.md
2) ai_docs/docs/EXISTING/00_intake.md
3) ai_docs/docs/EXISTING/01_inventory.md (if exists)
4) ai_docs/docs/EXISTING/02_system_map.md (if exists)
5) ai_docs/docs/EXISTING/03_quality_snapshot.md (if exists)
6) ai_docs/docs/EXISTING/04_risk_register.md (if exists)
7) ai_docs/tickets/ (find the newest TKT-02xx tickets)
8) references/*/SNAPSHOT.txt

## Mission: get a test APK today
1) Pick the smallest 1–3 tickets that directly address the reported hang:
   - lifecycle-safe handoff (service/activity/authenticator delivery)
   - timeout guard to prevent infinite waits
   - fail-fast user-visible error + safe log markers
2) Implement minimal patches in the Android app repo (termbot).
3) Update or create a ticket execution note:
   - After finishing each ticket, run: ai_docs/scripts/closeout_check.sh <TICKET_ID>

   - ai_docs/docs/BUILD_STATUS.md (short summary, links to logs)
4) Run build in Docker:
   - ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
   Save build log automatically (script does this).
5) Report:
   - APK output path(s)
   - adb install command
   - logcat filter command (no secrets)
6) If build fails:
   - analyze `references/logs/android_build_*.log`
   - make smallest fix and re-run build.

