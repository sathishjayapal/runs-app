# ADR-004: Simplified RIFL Design (In-Memory Cache)

**Date:** May 16, 2026  
**Status:** Accepted  
**Authors:** Sathish

## Context

Park's RIFL paper (Ch. 2) proposes durable completion records: store responses in the database, making cache persistent across crashes. This requires atomic writes of mutations + completion records.

For a startup/MVP, complexity vs. safety trade-off needs resolution.

## Decision

Implement **simplified RIFL design (Ch. 3)**: completion records live in memory only, relying on PostgreSQL WAL to durably store mutations. On JVM crash, cache is lost but mutations are durable; retries re-execute mutations, which are safe due to idempotency.

## Trade-off Analysis

| Design | Consistency | Crash Safety | Implementation | Operational |
|--------|-------------|--------------|-----------------|-------------|
| **Full RIFL (Ch. 2)** | Perfect | Mutation + response durable | Complex (dual writes) | Hard to test |
| **Simplified (Ch. 3)** | Good (idempotent) | Mutation durable, retry re-executes | Simple | Easy to test |

## Why Simplified Works

1. **Idempotency**: UNIQUE(activity_id) + business logic ensures safe re-execution
2. **WAL guarantees**: PostgreSQL commits mutations durably before returning
3. **Window is small**: Crash between Postgres commit and RAM cache write is rare
4. **Cost/benefit**: 99% consistency at 20% implementation cost

## Scenario: JVM Crash

```
Client: POST /api/garminRuns (activityId=ABC)
Server: INSERT into DB ✓ (committed via WAL)
Server: Cache populated ✓
Network: Response sent ✓
Client: ACK received ✓

vs.

Client: POST /api/garminRuns (activityId=ABC)
Server: INSERT into DB ✓ (committed via WAL)
🔥 JVM CRASH 🔥
Server: Cache LOST ✗
Client: Timeout, retries
Server: INSERT into DB again → UNIQUE violation (idempotent!) ✓
Client: Sees error or retry loop
```

## Consequences

- Cache is **not** persistent across JVM restarts
- On crash, next client request may fail with UNIQUE constraint error
- Requires monitoring: "retry rate" metric to detect crash patterns
- Production: Should pair with automatic restarts (Kubernetes, systemd)

## Monitoring

Add metrics:
- Cache size (should stay < 100K records)
- GC frequency (should match schedule)
- Retry failure rate (should stay < 0.01%)
