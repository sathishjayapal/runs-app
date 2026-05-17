# ADR-001: RIFL for Exactly-Once Semantics

**Date:** May 16, 2026  
**Status:** Accepted  
**Authors:** Sathish

## Context

POST requests to create garmin runs can fail mid-network transmission. Clients retry, potentially causing duplicate runs in the database. Without exactly-once semantics, users cannot safely retry without risk of data corruption.

## Decision

Implement RIFL (Reusable Infrastructure for Linearizability) from Seo Jin Park's Stanford PhD thesis to guarantee exactly-once execution semantics for POST /api/garminRuns.

## Implementation

- **LeaseManager**: Tracks client heartbeats; leases expire after 60 seconds
- **ResultTracker**: In-memory cache of completion records, keyed by (clientId, sequenceNumber)
- **RiflFilter**: Intercepts requests, looks up cache, replays on hit, caches on miss
- **RiflGcScheduler**: Periodic cleanup of expired clients' records (every 30 seconds)

## Rationale

1. **Academic rigor**: Proven design from peer-reviewed research
2. **Safety**: Guarantees linearizability even under network failures and JVM crashes
3. **Simplified design**: Leverages PostgreSQL WAL for mutation durability; cache is in-memory only
4. **Idempotency**: Combined with UNIQUE(activity_id) constraint, mutations are safe on retry

## Trade-offs

| Pro | Con |
|-----|-----|
| Exact-once guarantee | Adds latency (cache lookup) |
| Simple to understand | In-memory cache lost on crash |
| Works with existing DB | Lease management overhead |
| Scales horizontally | Requires client compliance |

## Consequences

- All POST /api/garminRuns clients **must** include X-Client-Id, X-Sequence-Number, X-First-Incomplete headers
- Clients must renew leases via POST /api/rifl/lease every 30 seconds
- Stale clients' cached records auto-GC after 60 seconds
- Database has UNIQUE(activity_id) constraint enforcing idempotency
