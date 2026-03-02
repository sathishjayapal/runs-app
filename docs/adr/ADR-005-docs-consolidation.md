# ADR-005: Documentation Consolidation

## Status
Accepted

## Context
Multiple Markdown files at the project root duplicated guidance for admin access, CI/CD, Garmin setup, and RabbitMQ debugging. To reduce drift and keep a single source of truth, these guides are consolidated into one ADR entry.

## Decision
- Retire scattered root-level guides and capture their essential guidance here.
- Keep the project root clean; readers use this ADR for operational/developer references.
- Root `README.md` will point here for detailed operational notes.

## Consolidated Guidance (from retired docs)

### Admin Access & Setup (was `ADMIN_ACCESS_SETUP.md`)
- Admin user provisioning and elevated-access steps should follow centralized IAM policies; avoid hardcoding credentials in env files.
- Store secrets in the configured secrets manager and inject via environment variables at deploy time.

### CI/CD (was `CI_CD_SETUP.md` and `QUICK_START_CI.md`)
- Build with Maven (`mvnw clean verify`) and Node tests (`npm test`) as pipeline stages.
- Use environment-specific configs per stage; never bake credentials into images.
- Publish artifacts only from mainline builds; enforce branch protections for release branches.

### Garmin SDK / FIT Setup (was `GARMIN_SDK_INSTALLATION.md`, `GARMIN_FIT_SETUP.md`, `GARMIN_FIT_IMPORT_README.md`)
- Install the Garmin FIT SDK locally only when building FIT tooling; do not bundle SDK binaries into the app image.
- For FIT imports, prefer stateless converters executed in CI/CD with cached dependencies; keep inputs in object storage, not the repo.
- Validate FIT parsing with a small golden sample set committed under `src/test/resources` (or fetched in tests) to ensure compatibility.

### RabbitMQ Debugging (was `RABBITMQ_DEBUG_GUIDE.md`)
- When messages are unroutable, enable mandatory publishing and inspect return callbacks; verify exchange/queue/routing-key alignment with broker declarations.
- Use the RabbitMQ management UI to confirm bindings and DLQ configuration before redeploying code.
- Favor idempotent consumers and dead-letter queues for poison messages; avoid manual broker mutations in production.

## Consequences
- One authoritative operational/developer doc under `docs/adr/ADR-005-docs-consolidation.md`.
- Root clutter removed; `README.md` links here.
- Future procedural changes should update this ADR instead of creating new scattered Markdown files.

