# Existing Project — 01 Inventory & System Map

ROLE: Architect + PM/BA

## Multi-repo support
If the system spans multiple repos:
- Configure repo paths in `ai_docs/config/repos.list`
- Run `ai_docs/scripts/snapshot_all.sh` to generate per-repo snapshots under `references/<repo>/`

## Inputs (paste into chat)
- Snapshot(s) from `references/` OR a single `SNAPSHOT.txt`
- Your continuation goal (stabilize? features? cost reduction? compliance?)
- Known runtime/deploy info (k8s? docker? serverless? on-prem?)

## Tasks
1) Inventory: components, entrypoints, configs, external deps
2) System map: data flows + trust boundaries
3) Quality snapshot: tests/CI/observability/security
4) Append open questions to `ai_docs/docs/QUESTIONS.md`

## Output files (suggested)
- ai_docs/docs/EXISTING/01_inventory.md
- ai_docs/docs/EXISTING/02_system_map.md
- ai_docs/docs/EXISTING/03_quality_snapshot.md
