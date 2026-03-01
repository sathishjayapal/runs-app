# ADR-004: Embed React Frontend in Spring Boot JAR

**Date:** 2026-02
**Status:** Accepted

## Context

The application has a React 19 + TypeScript frontend and a Spring Boot backend. Deployment options: separate frontend CDN/server, embedded in Spring Boot JAR via Maven plugin, or monorepo with separate build pipelines.

## Decision

Use **frontend-maven-plugin** to build React assets during `mvn package` and embed the static bundle inside the Spring Boot JAR at `BOOT-INF/classes/static/`.

## Rationale

- **Single deployable artifact:** One JAR contains everything. No separate Nginx, S3 bucket, or CDN needed for a portfolio or small-scale deployment.
- **Simplified CI:** One Maven build step produces a fully runnable artifact.
- **SPA routing handled by Spring:** `SpaRoutingConfig` forwards all non-`/api/**` requests to `index.html`, enabling React Router to handle client-side navigation.
- `frontend-maven-plugin` downloads Node.js automatically — no Node installation required on the build server (works in GitHub Actions matrix).

## Trade-offs

- Maven build is slower (npm install + webpack on every build). Mitigated by CI caching of `node_modules`.
- Independent frontend deployment (e.g., Vercel, CloudFront) would require splitting the build. This is a future migration path if scale demands it.

## Consequences

- Dev workflow: `npm run devserver` (port 3000) with `API_PATH=http://localhost:8080` proxies to backend.
- Prod: `mvn spring-boot:build-image` packages the full stack into an OCI image.
- Webpack output path: `target/classes/static/`.
