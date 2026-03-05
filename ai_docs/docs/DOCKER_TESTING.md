# Docker-first Testing (Clean Host)

Goal: avoid installing toolchains locally; run tests in containers.

## Pattern
1) AI proposes commands + Docker image
2) Human runs the command
3) Human pastes output back → AI iterates grounded

## Generic runner script
Use:
- `ai_docs/scripts/docker_run.sh`

It mounts a repo path into a container and runs a command inside.

### Examples
Node (pnpm):
```bash
ai_docs/scripts/docker_run.sh ./repos/api node:22 "corepack enable && pnpm -v && pnpm test"
```

Python:
```bash
ai_docs/scripts/docker_run.sh ./repos/api python:3.12 "python -V && pip install -r requirements.txt && pytest -q"
```

Go:
```bash
ai_docs/scripts/docker_run.sh ./repos/api golang:1.22 "go test ./..."
```

## Notes
- For monorepos, pass `.` as the repo path.
- Prefer short, deterministic test commands.
- If tests are too heavy, define a **manual smoke script** and keep it in repo scripts/ (or in ai_docs/scripts/).
