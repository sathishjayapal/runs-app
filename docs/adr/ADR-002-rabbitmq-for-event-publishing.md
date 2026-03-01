# ADR-002: RabbitMQ for Garmin Event Publishing

**Date:** 2026-02
**Status:** Accepted

## Context

When a Garmin FIT file is imported, downstream services (e.g., `eventstracker`) need to be notified. Options considered: synchronous REST call, RabbitMQ, Kafka, AWS SNS/SQS.

## Decision

Use **RabbitMQ** with a topic exchange (`x.sathishprojects.events`) and a dedicated queue (`x.garmin.operations`).

## Rationale

- **Decoupling:** The import service does not need to know about downstream consumers; new consumers can subscribe without changing the producer.
- **Durability:** RabbitMQ queues survive producer restarts; events are not lost if `eventstracker` is temporarily down.
- **Simplicity over Kafka:** RabbitMQ requires no ZooKeeper/KRaft cluster, no partition management, and no consumer group offsets. For a single-producer/single-consumer pattern, it is operationally simpler.
- **Docker Compose fit:** RabbitMQ's official image starts cleanly in Docker Compose for local dev and CI.

## Trade-offs

- Not suitable for high-throughput event replay or multi-consumer fan-out at Kafka scale.
- If event history / replay is needed in future, migrating to Kafka would be required.

## Consequences

`GarminRunEvent` is serialized as JSON via `Jackson2JsonMessageConverter`. Consumer services must declare the queue or use the same exchange/routing key convention (`garmin.operations.crud`).
