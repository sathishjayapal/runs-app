# Runs App - Setup & Configuration Guide

## Quick Start (Local Development)

### 1. Start Database & Initialize Configuration

```bash
./dev-up.sh
```

This script will:
- Start PostgreSQL in Docker
- Generate `.env` and `.env.development` files with local defaults
- Set up RabbitMQ credentials from infrastructure config
- Create the database and run migrations

### 2. Configure Application

Copy the template:
```bash
cp .env.example .env
```

Edit `.env` and fill in:
- **Database credentials** (dev-up.sh defaults should work)
- **Google Drive configuration** (see below)
- **Email configuration** (SMTP)
- **Alert email recipients** (for CSV import failures)

### 3. Start the Application

```bash
mvn spring-boot:run
```

Or from your IDE: Click the Run button

---

## Configuration Details

### Google Drive Setup

To enable CSV imports from Google Drive:

1. **Create a Google Cloud Project**
   - Go to https://console.cloud.google.com
   - Create a new project
   - Enable Google Drive API

2. **Create Service Account**
   - In Google Cloud Console → Service Accounts
   - Create new service account
   - Create a JSON key
   - Download and save securely (DO NOT commit)

3. **Set Configuration**
   ```bash
   # In .env file:
   GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH=/path/to/downloaded-key.json
   ```

4. **Create Google Drive Folders**
   - Create folders in your Google Drive:
     - `garmin-imports` (source folder)
     - `garmin-processed` (successful imports)
     - `garmin-quarantine` (partial failures)
     - `garmin-failed` (complete failures)
     - `garmin-retry` (retry manifests)

5. **Share Folders with Service Account**
   - Get service account email from the JSON key
   - Share each folder with the service account (Editor role)

6. **Get Folder IDs**
   - Open each folder in Google Drive
   - Folder URL: `https://drive.google.com/drive/folders/{FOLDER_ID}`
   - Copy FOLDER_ID and set in `.env`:
   ```bash
   GARMIN_DRIVE_FOLDER_ID=your-imports-folder-id
   GARMIN_DRIVE_PROCESSED_FOLDER_ID=your-processed-folder-id
   GARMIN_DRIVE_QUARANTINE_FOLDER_ID=your-quarantine-folder-id
   GARMIN_DRIVE_FAILED_FOLDER_ID=your-failed-folder-id
   GARMIN_DRIVE_RETRY_FOLDER_ID=your-retry-folder-id
   ```

### Email Configuration

For CSV import failure alerts:

1. **SMTP Setup** (using Gmail example)
   ```bash
   SMTP_HOST=smtp.gmail.com
   SMTP_USERNAME=your-email@gmail.com
   SMTP_PASSWORD=your-app-specific-password
   ```

2. **Generate Gmail App Password** (NOT account password)
   - Go to https://myaccount.google.com/apppasswords
   - Select Mail and Windows Computer (or your device)
   - Copy the generated password
   - Use as `SMTP_PASSWORD` in `.env`

3. **Alert Recipients**
   ```bash
   GARMIN_ALERT_EMAIL_RECIPIENTS=admin@example.com,devops@example.com
   GARMIN_ALERT_EMAIL_FROM=garmin-import@example.com
   ```

### RabbitMQ

Default local setup (dev-up.sh handles this):
- Host: localhost
- Port: 5672
- Username/Password: from infrastructure config

---

## Environment Variables Reference

| Variable | Purpose | Required | Default |
|----------|---------|----------|---------|
| `JDBC_DATABASE_URL` | PostgreSQL connection | Yes | localhost:5443 |
| `JDBC_DATABASE_USERNAME` | DB user | Yes | postgres |
| `JDBC_DATABASE_PASSWORD` | DB password | Yes | None |
| `RABBITMQ_HOST` | RabbitMQ host | Yes | localhost |
| `RABBITMQ_PORT` | RabbitMQ port | Yes | 5672 |
| `RABBITMQ_USERNAME` | RabbitMQ user | Yes | guest |
| `RABBITMQ_PASSWORD` | RabbitMQ password | Yes | None |
| `SMTP_HOST` | Email SMTP server | No | smtp.gmail.com |
| `SMTP_USERNAME` | Email account | No | None |
| `SMTP_PASSWORD` | Email password | No | None |
| `MAIL_FROM` | Email sender address | No | noreply@example.com |
| `MAIL_DISPLAY_NAME` | Email sender name | No | Runs App |
| `GARMIN_DRIVE_SERVICE_ACCOUNT_KEY_PATH` | Path to Google Drive credentials | No | None |
| `GARMIN_DRIVE_FOLDER_ID` | Google Drive import folder | No | None |
| `GARMIN_DRIVE_PROCESSED_FOLDER_ID` | Google Drive processed folder | No | None |
| `GARMIN_ALERT_EMAIL_RECIPIENTS` | Alert email addresses (comma-separated) | No | None |
| `GARMIN_ALERT_EMAIL_ENABLED` | Enable import alerts | No | true |
| `GARMIN_ALERT_EMAIL_MAX_RETRY_ATTEMPTS` | Retry threshold for alerts | No | 3 |

---

## Useful Commands

### Database
```bash
# Connect to local database
psql -h localhost -p 5443 -U postgres -d runsapp_db

# Reset database
./dev-up.sh --reset

# View migration status
./dev-up.sh --help
```

### Application
```bash
# Start backend
mvn spring-boot:run

# Start frontend (separate terminal)
npm run devserver

# Run tests
mvn test

# Check application health
curl http://localhost:8080/actuator/health
```

### Docker
```bash
# View logs
docker logs runs-app-postgres

# Stop containers
docker compose down

# Restart everything
docker compose up -d
```

---

## Troubleshooting

### Database Connection Refused
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start it:
docker compose up -d

# Or reset:
./dev-up.sh --reset
```

### RabbitMQ Connection Issues
Check infrastructure config in `jubilant-memory/config/.env`

### Google Drive Errors
- Verify service account has access to folders
- Check folder IDs are correct (no spaces)
- Ensure JSON key file exists at specified path

### Email Not Sending
- Verify SMTP credentials in `.env`
- For Gmail: Use App Password, not account password
- Check `GARMIN_ALERT_EMAIL_RECIPIENTS` is configured

---

## Security Notes

See [SECURITY.md](./SECURITY.md) for detailed security practices.

**Key Points:**
- Never commit `.env` files to git
- Never commit Google Drive service account keys
- Use environment variables for all secrets
- .env.example serves as configuration template only
