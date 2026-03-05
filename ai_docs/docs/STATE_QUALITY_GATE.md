# STATE.md Quality Gate

This gate prevents STATE.md from growing beyond a safe “paste set” size.

## Local
```bash
ai_docs/scripts/state_check.sh
```

Set limits (optional):
```bash
MAX_LINES=300 MAX_BYTES=30000 ai_docs/scripts/state_check.sh
```

## GitHub Actions (snippet)
Add a job step:
```yaml
- name: Check STATE.md size
  run: |
    ai_docs/scripts/state_check.sh
```

## GitLab CI (snippet)
```yaml
state_check:
  stage: test
  script:
    - ai_docs/scripts/state_check.sh
```

## Recommendation
Keep defaults:
- MAX_LINES=250
- MAX_BYTES=25000
