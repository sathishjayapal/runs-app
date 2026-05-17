# ADR-002: PostgreSQL + RabbitMQ Stack

**Date:** May 16, 2026  
**Status:** Accepted  
**Authors:** Sathish

## Context

Need to choose a primary data store and messaging system for activity tracking platform. Requirements: strong consistency, transactional guarantees, ability to decouple services (Garmin import → processing → storage).

## Decision

- **Primary DB**: PostgreSQL 18.1 with Flyway migrations
- **Messaging**: RabbitMQ for async task queues (Garmin processing, notifications)
- **Cache**: Redis (future consideration for session/leaderboard cache)

## Rationale

| Component | Choice | Why |
|-----------|--------|-----|
| **DB** | PostgreSQL | ACID guarantees, WAL for crash safety, Flyway for schema versioning, proven for financial/critical systems |
| **Messaging** | RabbitMQ | Reliable delivery, topic-based routing, integration with Kafka (mytracker ecosystem) |
| **Alternative** | MongoDB | Rejected: NoSQL eventual consistency conflicts with RIFL requirements |

## Architecture Benefits

1. **WAL durability**: Mutations durable immediately; RIFL cache loss is safe
2. **Transactional integrity**: UNIQUE constraints, foreign keys prevent data corruption
3. **Operational simplicity**: Single source of truth, no dual-writes
4. **Scaling path**: Read replicas for reads, write master for mutations

## Trade-offs

| Pro | Con |
|-----|-----|
| ACID guarantees | Vertical scaling limits |
| Proven stability | Schema migration complexity |
| Rich query language | NoSQL flexibility |
| Transactional support | Not ideal for unstructured data |

## Consequences

- Schema changes require Flyway migrations (V001, V002, ...)
- Larger datasets require read replicas + load balancing
- RabbitMQ adds operational overhead (monitoring, queue management)
- Must manage connection pools carefully (HikariCP defaults)
