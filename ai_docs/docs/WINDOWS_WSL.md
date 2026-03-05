# Windows + WSL2 Compatibility

This pack is designed to work on:
- Linux native
- Windows with Docker Desktop
- Windows with WSL2 (recommended)

## Recommended setup (Windows)
1) Install Docker Desktop
2) Enable WSL2 integration in Docker Desktop
3) Clone repos inside WSL filesystem (best performance):
   - e.g. `/home/<user>/work/...` not `/mnt/c/...`

## Running scripts
Run from WSL terminal:
```bash
bash scripts/install_ai_pack.sh
ai_docs/scripts/snapshot_all.sh
```

## Docker runner
Works the same in WSL:
```bash
ai_docs/scripts/docker_run.sh . node:22 "node -v"
```

## VS Code
Use VS Code + Remote WSL extension:
- Open the workspace folder inside WSL
- Install Cline/Copilot/Codex extensions as desired
