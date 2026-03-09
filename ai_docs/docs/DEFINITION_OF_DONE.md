# Definition of Done (DoD)

A ticket is Done only when:
- [ ] Acceptance criteria met
- [ ] Verification executed (tests or explicit manual steps) and results recorded
- [ ] Docker-first build/test command used when code changed (or rationale recorded if docs-only)
- [ ] Docs updated if behavior changed
- [ ] Observability updated if runtime behavior changed
- [ ] Security considerations addressed when relevant
- [ ] Rollback plan noted for risky changes
- [ ] DELIVERY_BOARD updated with accurate ticket state
- [ ] QUESTIONS.md appended for unresolved unknowns (instead of assumptions)
- [ ] `ai_docs/scripts/closeout_check.sh <TICKET_ID>` executed and passing
