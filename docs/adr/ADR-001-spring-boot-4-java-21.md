# ADR-001: Use Spring Boot 4.0.1 and Java 21

**Date:** 2026-02
**Status:** Accepted

## Context

Selecting the runtime and framework version for a new full-stack running tracker application. Options were Spring Boot 3.x (LTS), Spring Boot 4.x (latest), and non-Spring alternatives.

## Decision

Use **Spring Boot 4.0.1** with **Java 21**.

## Rationale

- Java 21 is the current LTS release with Virtual Threads (Project Loom) available for high-throughput I/O workloads (Garmin file parsing, RabbitMQ consumers).
- Spring Boot 4.0 completes the Jakarta EE 10 migration (javax → jakarta namespace) and aligns with Spring Framework 7.
- Staying on the bleeding edge demonstrates active engagement with the ecosystem — a meaningful signal for a portfolio project.
- Testcontainers support is first-class in Spring Boot 4.

## Trade-offs

- Spring Boot 4 is not yet the enterprise default; some third-party libraries may lag. Mitigated by pinning versions in pom.xml.
- Requires Java 21 as minimum — no Java 17 compatibility.

## Consequences

All dependencies must be compatible with Spring Boot 4 BOM. Frontend-maven-plugin is pinned to Node 24 to match.
