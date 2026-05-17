# ADR-003: Lease-Based Garbage Collection

**Date:** May 16, 2026  
**Status:** Accepted  
**Authors:** Sathish

## Context

RIFL caches completion records in memory. Without cleanup, cache grows unbounded, consuming memory and causing performance degradation. Need an automatic, efficient GC strategy that works with distributed clients.

## Decision

Implement **lease-based garbage collection** with per-client heartbeat tracking and automatic reaping of expired clients' records.

## Design

```
Client lifecycle:
1. POST /api/rifl/lease/open → Returns clientId
2. Client makes requests with X-Client-Id header
3. Client renews lease every 30s via POST /api/rifl/lease
4. Server tracks lastSeen[clientId] = Instant.now()
5. After 60s without renewal → lease expires
6. RiflGcScheduler finds expired clients every 30s
7. ResultTracker.reapAll(clientId) deletes all records
```

## Components

| Component | Purpose |
|-----------|---------|
| **LeaseManager** | Tracks per-client heartbeats, detects expiry |
| **LeaseController** | HTTP endpoints for lease open/renew |
| **RiflGcScheduler** | @Scheduled task runs every 30s, reaps expired clients |
| **ResultTracker.reapAll()** | Atomic deletion of all records for a client |

## Rationale

1. **Simple**: No distributed consensus, no complex state machines
2. **Safe**: Expired clients assumed crashed; safe to delete their state
3. **Automatic**: Clients don't need to GC; server-side cleanup
4. **Configurable**: Lease duration (60s) and GC interval (30s) are tunable

## Trade-offs

| Pro | Con |
|-----|-----|
| Zero client logic | Requires periodic renewal |
| Memory bounded | Heartbeat overhead |
| Deterministic cleanup | False positives (slow clients) |
| No distributed coordination | Single-point-of-truth on lease |

## Consequences

- Clients must renew leases every 30 seconds (half the expiry time)
- Network partitions will cause false expiry of disconnected clients
- Horizontal scaling: each server tracks its own clients independently. This is safe because each client maintains a single lease with a designated server; no cross-server cache coherence needed
- Monitored metric: cache size, GC frequency, false expiry rate
