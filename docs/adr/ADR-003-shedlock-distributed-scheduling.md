# ADR-003: ShedLock for Distributed Job Scheduling

**Date:** 2026-02
**Status:** Accepted

## Context

The Garmin FIT import job runs on a schedule and must execute exactly once across all running instances. Options: Spring `@Scheduled` alone, Quartz, ShedLock, AWS EventBridge.

## Decision

Use **ShedLock** with the JDBC provider backed by PostgreSQL.

## Rationale

- `@Scheduled` alone fires on every instance simultaneously — causes duplicate imports and duplicate RabbitMQ events.
- **ShedLock** uses a `shedlock` database table as a distributed mutex with a configurable lock-until timeout.
- No additional infrastructure required — reuses the existing PostgreSQL instance.
- Lighter than Quartz (no 11-table schema, no clustering config).
- Works correctly in single-instance deployments and scales to multi-instance transparently.

## Trade-offs

- Lock table in same DB as application data (acceptable; it's a small table).
- If the node holding the lock crashes mid-job, lock expires after `lockAtMostFor` duration — next scheduled run picks up.

## Consequences

Flyway migration `V003__FIX_SHEDLOCK_TABLE.sql` ensures the `shedlock` table uses `VARCHAR(64)` for the `name` column (JDBC provider requirement). Lock name: `GarminFitImportScheduledJob`.
