# Security & Vulnerability Scanning Policy

## Overview

runs-app maintains a **pragmatic security-first approach** that balances risk mitigation with operational velocity.

## Vulnerability Scanning

### Trivy Image Scanning (CI/CD)

Every container image build is scanned with [Trivy](https://github.com/aquasecurity/trivy) for known vulnerabilities.

**Blocking Threshold:** CRITICAL only
- **CRITICAL:** Build fails immediately. CRITICAL vulnerabilities must be resolved before deployment.
- **HIGH:** Reported in GitHub Security alerts; tracked for remediation but do not block builds.
- **MEDIUM/LOW:** Tracked quarterly; addressed on regular patching cadence.

### Rationale

runs-app's threat model does not include XML parsing:
- Input data: Garmin FIT binary files, JSON from REST APIs (Strava, Anthropic)
- No user XML uploads or untrusted XML parsing
- libexpat (Alpine base image transitive dependency) is not in the application's attack surface

This means HIGH vulnerabilities in libexpat (e.g., [CVE-2026-76956](https://avd.aquasec.com/nvd/cve-2026-76956), [CVE-2026-76957](https://avd.aquasec.com/nvd/cve-2026-76957)) are **not exploitable** in this application.

## Vulnerability Exploitability Exchange (VEX)

runs-app publishes a [`.vex.json`](.vex.json) file documenting vulnerabilities that are **not affected** by this application. VEX allows us to:
- Communicate actual exploitability, not just CVE presence
- Reduce false positives for security teams consuming this data
- Maintain transparency about security posture

See [CycloneDX VEX Specification](https://cyclonedx.org/capabilities/vex/) for details.

## Base Image Updates

The Dockerfile uses `eclipse-temurin:21-jre-alpine` with `apk upgrade` to ensure all OS-level patches are applied at build time. Once Alpine 3.24.2+ is released with patched libexpat, the next image build will automatically include the fix.

## Reporting Security Issues

If you discover a security vulnerability in runs-app, please do **not** open a public GitHub issue. Instead:

1. Email: sathishk.dot@gmail.com
2. Include: vulnerability description, affected version, reproduction steps
3. Allow time for a patch before public disclosure

---

**Last updated:** 2026-09-13  
**Policy owner:** Architecture team
