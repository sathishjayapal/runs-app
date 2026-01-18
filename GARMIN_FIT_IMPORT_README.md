# Garmin FIT File Import - Setup & Usage Guide

## Overview

This module provides automated scheduled processing of Garmin .FIT files using ShedLock for distributed locking. The system monitors a designated folder every 30 minutes, processes ZIP files and individual FIT files, and imports activity data into the `garmin_run` table.

## Architecture

### Components

1. **GarminFitImportScheduledJob** - Scheduled job that runs every 30 minutes
2. **GarminFitImportService** - Core business logic for processing files
3. **GarminFitFileParser** - Parses Garmin FIT binary files using Garmin FIT SDK
4. **FileNameTracker** - Tracks processed files to prevent duplicates
5. **ShedLock** - Ensures only one instance runs in distributed environments

### Database Tables Used

- **`garmin_run`** - Stores imported activity data
- **`file_name_tracker`** - Tracks processed files (prevents re-processing)
- **`shedlock`** - Manages distributed locks for scheduled jobs

**No new tables required!** All existing infrastructure is reused.

---

## Configuration

### 1. Application Properties

Add to `application.yml` or set as environment variables:

```yaml
app:
  garmin:
    import:
      folder: /data/garmin-fit-files  # Folder to monitor
      systemUserId: 10000              # User ID for system imports
```

**Environment Variables:**
- `GARMIN_IMPORT_FOLDER` - Path to folder containing ZIP/FIT files
- `GARMIN_SYSTEM_USER_ID` - User ID that will be set as creator (default: 10000)

### 2. Folder Structure

Create the import folder structure:

```bash
mkdir -p /data/garmin-fit-files
mkdir -p /data/garmin-fit-files/processed
```

**Folder Layout:**
```
/data/garmin-fit-files/
├── activities_2024.zip          # ZIP files with .fit files inside
├── morning_run.fit               # Individual .fit files
├── evening_run.fit
└── processed/                    # Archived files after processing
    └── activities_2024.zip
```

### 3. File Permissions

Ensure the application has read/write access:

```bash
chmod 755 /data/garmin-fit-files
chown -R <app-user>:<app-group> /data/garmin-fit-files
```

---

## How It Works

### Scheduled Processing (Every 30 Minutes)

1. **Job Triggers** at :00 and :30 of every hour
2. **ShedLock Acquires Lock** - Prevents concurrent execution
3. **Scans Import Folder** for `.zip` and `.fit` files
4. **For Each ZIP File:**
   - Extracts to temporary directory
   - Processes each `.fit` file inside
   - Archives ZIP to `processed/` folder
5. **For Each FIT File:**
   - Checks `file_name_tracker` (skip if already processed)
   - Parses FIT file using Garmin SDK
   - Converts to `GarminRunDTO`
   - Saves to `garmin_run` table
   - Records in `file_name_tracker`
6. **Logs Summary** - Success/Skipped/Failed counts

### Data Mapping

| FIT File Field | GarminRun Field | Notes |
|----------------|-----------------|-------|
| Session.sport | activityType | Mapped to: running, strength_training, elliptical |
| Session.startTime | activityDate | ISO 8601 format with timezone |
| Activity.timestamp | activityId | Unix timestamp as string |
| Session.totalDistance | distance | Converted from meters to miles |
| Session.totalElapsedTime | elapsedTime | Formatted as HH:MM:SS |
| Session.maxHeartRate | maxHeartRate | Integer (40-220 bpm) |
| Session.totalCalories | calories | Integer (1-10000) |
| Filename | activityName | If not in FIT file |

---

## Usage

### Option 1: Automatic (Scheduled)

Simply place ZIP or FIT files in the configured folder. The job runs automatically every 30 minutes.

```bash
# Copy files to import folder
cp ~/Downloads/garmin-activities-2024.zip /data/garmin-fit-files/

# Wait for next scheduled run (max 30 minutes)
# Check logs for processing status
```

### Option 2: Manual Trigger (REST API)

Trigger import immediately via REST endpoint (Admin only):

```bash
curl -X POST http://localhost:8080/api/garmin-import/trigger \
  -u admin@runsapp.com:admin123 \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "successFiles": ["morning_run.fit", "evening_run.fit"],
  "skippedFiles": ["already_processed.fit"],
  "failedFiles": {
    "corrupted.fit": "FIT file integrity check failed"
  },
  "successCount": 2,
  "skippedCount": 1,
  "failedCount": 1,
  "totalProcessed": 4
}
```

---

## Monitoring & Logs

### Log Messages

**Successful Import:**
```
INFO  - Starting Garmin FIT Import Scheduled Job
INFO  - Found 3 file(s) to process
INFO  - Processing ZIP file: activities_2024.zip
INFO  - Successfully imported activity from file: morning_run.fit (ID: 10523)
INFO  - Import completed - Success: 5, Skipped: 2, Failed: 0
```

**Skipped (Already Processed):**
```
DEBUG - File already processed, skipping: morning_run.fit
```

**Failed Import:**
```
ERROR - Failed to process FIT file: corrupted.fit
ERROR - FIT file integrity check failed
```

### Database Queries

**Check processed files:**
```sql
SELECT * FROM file_name_tracker 
WHERE file_name LIKE '%.fit' 
ORDER BY created_at DESC;
```

**Check imported activities:**
```sql
SELECT activity_id, activity_name, activity_date, distance, calories
FROM garmin_run
WHERE activity_description LIKE 'Imported from FIT file:%'
ORDER BY created_at DESC;
```

**Check ShedLock status:**
```sql
SELECT * FROM shedlock 
WHERE name = 'garminFitImportJob';
```

---

## Troubleshooting

### Issue: Job Not Running

**Check:**
1. Verify `@EnableScheduling` is present in `ShedLockConfig`
2. Check application logs for scheduling errors
3. Verify ShedLock table exists and is accessible

**Solution:**
```sql
-- Check if shedlock table exists
SELECT * FROM information_schema.tables 
WHERE table_name = 'shedlock';

-- If missing, create it (should be created by Flyway)
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

### Issue: Files Not Being Processed

**Check:**
1. Folder path is correct in configuration
2. Application has read permissions
3. Files are `.zip` or `.fit` extension (case-insensitive)

**Debug:**
```bash
# Check folder exists and has files
ls -la /data/garmin-fit-files/

# Check application can read folder
sudo -u <app-user> ls /data/garmin-fit-files/
```

### Issue: Duplicate Imports

**Cause:** File was renamed or `file_name_tracker` entry was deleted

**Solution:**
```sql
-- Check if file is tracked
SELECT * FROM file_name_tracker WHERE file_name = 'your_file.fit';

-- Manually add to tracker to prevent re-import
INSERT INTO file_name_tracker (file_name, updated_by, created_by_id, created_at, updated_at)
VALUES ('your_file.fit', 'MANUAL', 10000, NOW(), NOW());
```

### Issue: System User Not Found

**Error:** `System user not found with ID: 10000`

**Solution:**
```sql
-- Check if system user exists
SELECT * FROM run_app_user WHERE id = 10000;

-- Create system user if missing (adjust ID as needed)
INSERT INTO run_app_user (id, email, password, name, created_at, updated_at)
VALUES (10000, 'system@runsapp.com', 'disabled', 'System Import User', NOW(), NOW());
```

---

## Performance Considerations

### Large ZIP Files

- Files are extracted to temp directory one at a time
- Each FIT file is processed in a transaction
- Failed files don't block others

### Concurrent Execution

- ShedLock prevents multiple instances from running simultaneously
- Lock duration: 25 minutes (job should complete within this)
- Minimum lock: 1 minute (prevents rapid re-execution)

### Cleanup

Processed ZIP files are moved to `processed/` subfolder. To clean up old archives:

```bash
# Delete archives older than 30 days
find /data/garmin-fit-files/processed -name "*.zip" -mtime +30 -delete
```

---

## Development & Testing

### Manual Testing

1. Place test FIT file in import folder
2. Call manual trigger endpoint or wait for scheduled run
3. Check logs and database

### Unit Testing

```java
@Test
void testFitFileParser() throws Exception {
    FitActivityData data = parser.parse("test-files/sample.fit");
    assertNotNull(data.getActivityId());
    assertEquals("running", data.getActivityType());
}
```

---

## API Reference

### POST /api/garmin-import/trigger

**Description:** Manually trigger FIT file import

**Authentication:** Required (Admin only)

**Request:**
```bash
curl -X POST http://localhost:8080/api/garmin-import/trigger \
  -u admin@runsapp.com:admin123
```

**Response:** `ImportResult` object with processing summary

---

## Dependencies

- **ShedLock 5.10.2** - Distributed lock management
- **Garmin FIT SDK 21.141.0** - FIT file parsing
- **Spring Scheduling** - Cron-based job execution
- **PostgreSQL** - Database for locks and tracking

---

## Security

- Import endpoint restricted to `ROLE_ADMIN`
- Files are validated before processing
- FIT file integrity check performed
- System user ID configurable (not hardcoded)

---

## Future Enhancements

- [ ] Web UI for upload and monitoring
- [ ] Email notifications on import completion
- [ ] Support for TCX and GPX formats
- [ ] Batch delete/reprocess functionality
- [ ] Import history dashboard
- [ ] Configurable schedule (not just 30 minutes)

---

## Support

For issues or questions:
1. Check application logs: `logs/runs-app.log`
2. Verify configuration in `application.yml`
3. Check database tables: `garmin_run`, `file_name_tracker`, `shedlock`
4. Review this documentation

---

**Last Updated:** January 17, 2026
**Version:** 1.0.0
