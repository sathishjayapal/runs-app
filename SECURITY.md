# Runs App - Security & Credentials Management

## Overview

This document outlines security best practices for Runs App, especially around credentials and secrets management.

---

## Golden Rules

1. **Never commit `.env` files to git**
   - `.env` files contain actual credentials
   - They are ignored by `.gitignore`
   - Use `.env.example` as template instead

2. **Never commit credential files**
   - Google Drive service account JSON keys
   - Certificate files (*.key, *.pem, *.p12)
   - Private keys of any kind
   - These are ignored by `.gitignore`

3. **All secrets use environment variables**
   - Database passwords
   - API keys
   - Email passwords
   - Folder IDs (sensitive path information)

4. **Use `.env.example` as configuration documentation**
   - Committed to git
   - Contains only placeholder/template values
   - Shows what needs to be configured
   - No actual credentials

---

## Environment Variables

### Application Configuration

All sensitive configuration is loaded via environment variables in `application.yml`:

```yaml
spring:
  datasource:
    password: ${JDBC_DATABASE_PASSWORD}  # ✓ From .env
  mail:
    password: ${SMTP_PASSWORD}           # ✓ From .env
    
app:
  garmin:
    drive:
      service-account-key-path: ${GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH}  # ✓ From .env
```

**Never use hardcoded values in configuration files.**

### Local Development (.env)

```bash
# .env (LOCAL ONLY - ignored by git)
JDBC_DATABASE_PASSWORD=actual-local-password
SMTP_PASSWORD=actual-app-password
GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH=/absolute/path/to/service-account.json
```

### Docker/Cloud Deployment

For containerized deployments:

1. **Mount .env file at runtime**
   ```dockerfile
   # docker-compose.yml
   services:
     runs-app:
       build: .
       env_file: .env.production  # Not in git, mounted at runtime
   ```

2. **Or pass as environment variables**
   ```bash
   docker run -e JDBC_DATABASE_PASSWORD="$DB_PASSWORD" ...
   ```

3. **Or use secrets management**
   - Kubernetes secrets
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault

---

## Google Drive Credentials

### Handling Service Account Keys

**DO NOT:**
- Commit `service-account*.json` files
- Put keys in Docker images
- Share keys in Slack/email
- Use keys in URLs or logs

**DO:**
- Store locally outside the repo
- Use path variables: `GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH=/secure/path/key.json`
- Or use base64 encoding: `GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_BASE64=<encoded>`
- Rotate keys periodically
- Use service accounts with minimal permissions

### Service Account Permissions

Grant the service account **only** what it needs:

```
Google Drive Folder → Share with service-account@project.iam.gserviceaccount.com
Permission: Editor (can read/write/move files)
```

Do NOT grant:
- Admin access to entire Drive
- Access to personal files
- Ownership of folders

---

## Email Credentials

### Gmail App Passwords

For Gmail SMTP:

1. Enable 2-Factor Authentication on Google Account
2. Generate App Password (not account password)
3. Use App Password in `SMTP_PASSWORD`

**Why?**
- Account password grant too much access
- App passwords are limited to specific service
- Can be revoked independently
- More secure

### SMTP Configuration

```bash
# .env
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-specific-password    # ← NOT account password
```

---

## Git Configuration

### .gitignore

Configured to ignore:
```
.env                          # Local environment config
.env.development              # Dev-specific config
.env.*.local                  # Environment-specific
*.key, *.pem, *.p12          # Certificate files
service-account*.json         # Google Drive credentials
credentials.json              # Generic credentials
```

### Checking for Secrets

Before committing, verify no credentials are exposed:

```bash
# Check for common patterns
git diff --cached | grep -i "password\|key\|secret\|token"

# Use git-secrets tool (if installed)
git secrets --scan
```

---

## Development vs. Production

### Local Development (.env)

- Simple configuration
- Database on localhost
- Default/test credentials acceptable
- Service account key on local disk
- Email to test accounts

Example:
```bash
JDBC_DATABASE_PASSWORD=devpassword123
SMTP_PASSWORD=test-app-password
GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH=~/Downloads/key.json
```

### Production

- All secrets from secure vault
- Database on managed service (RDS, Cloud SQL)
- Real credentials with strong randomization
- Service account key from secrets manager
- Email alerts to production addresses
- Use environment-specific .env files or secret injection

```bash
# NOT in repo - injected at runtime
JDBC_DATABASE_PASSWORD=${DB_PASSWORD_PROD}
SMTP_PASSWORD=${SMTP_PASSWORD_PROD}
GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_BASE64=${GCP_SERVICE_ACCOUNT_BASE64}
```

---

## Secret Rotation

### When to Rotate

- Quarterly (standard practice)
- After team member leaves
- If credentials are exposed
- After security incident

### How to Rotate

1. Generate new credential
2. Test with new credential in staging
3. Update in vault/secrets manager
4. Update in .env (local dev)
5. Redeploy applications
6. Revoke old credential

---

## Security Checklist

- [ ] `.env` is in `.gitignore`
- [ ] No actual credentials in `.env.example`
- [ ] All secrets use `${VARIABLE}` syntax in `application.yml`
- [ ] Service account keys stored outside repo
- [ ] Google Drive folders shared with least-privilege
- [ ] Gmail app password used (not account password)
- [ ] Production uses secure secrets management
- [ ] No credentials in logs or error messages
- [ ] Pre-commit hooks check for secrets
- [ ] Credentials are rotated regularly

---

## Suspicious Activity

If you find secrets committed to git:

1. **Do NOT push the branch**
2. **Immediately revoke the compromised credential**
3. **Report to security team**
4. **Generate new credential**
5. **Force-push only if approved** (rewrites history)

Example:
```bash
# EMERGENCY: Revoke Google Drive key
# Go to Google Cloud Console → Service Accounts → Delete compromised key

# Generate new key
# Update GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH in .env
# Restart application
```

---

## Questions?

Refer to [SETUP.md](./SETUP.md) for configuration walkthrough.

For security concerns, escalate to the security team.
