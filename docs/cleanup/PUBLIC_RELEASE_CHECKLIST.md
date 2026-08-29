# NAITFS Public Release Checklist

## Must pass before public GitHub or buyer handoff

- [ ] No `.token`, `.env.local`, `.env`, private keys, API keys, JWTs, or real credentials.
- [ ] No unrelated PDFs, job files, screenshots, local machine files, or personal documents.
- [ ] README explains the product in commercial language, not MVP chronology.
- [ ] Demo credentials are clearly labeled local/demo-only.
- [ ] `APP_SECURITY_ENABLED=true` by default outside local profiles.
- [ ] All write endpoints validate request bodies.
- [ ] Tenant-scoped data is queried with tenant filters.
- [ ] Payment idempotency rejects same-key/different-body conflicts.
- [ ] Outbox only marks `PUBLISHED` after Kafka confirmation.
- [ ] CI runs backend tests, frontend build, script syntax, JSON validation, and Docker Compose config.
- [ ] A single stable commercial demo command exists.
- [ ] Compliance claims avoid unsupported certifications such as SOC 2, HIPAA, PCI, or FedRAMP unless formally implemented and verified.
