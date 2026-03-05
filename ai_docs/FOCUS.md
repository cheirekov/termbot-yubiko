# FOCUS — Provider switch continuation (read first)

## Goal for this provider session
Continue from the current repo state and proceed with:
- TKT-0224: Android modernization epic — Phase B (AGP/Gradle uplift attempt in Docker)
Do not restart planning. Do not re-run old tickets.

## Hard rules
- Follow: ai_docs/AGENT_MEMORY.md and ai_docs/docs/WORKFLOW_CONTRACT.md
- Docker-first only. No host installs. Use:
  - ai_docs/scripts/android_docker_build.sh ./repos/termbot-termbot assembleDebug
- After each ticket/slice:
  - update DELIVERY_BOARD.md
  - update BUILD_STATUS.md if build ran
  - run ai_docs/scripts/closeout_check.sh TKT-XXXX

## Current known-good baseline
- Build passed on 2026-03-02; details in ai_docs/docs/BUILD_STATUS.md
- Do not regress YubiKey signing; if uncertain, add a short manual smoke step and note it in BUILD_STATUS.md

## What “done” looks like for Phase B
- AGP/Gradle uplift attempted in small increments
- Either:
  - success: build still green in Docker, or
  - failure: clear diagnosis + rollback plan + next ticket(s)
- Docs updated + closeout gate passes