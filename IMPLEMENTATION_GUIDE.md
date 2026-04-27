# Implementation Guide: File Import Reconciliation

## Overview

This implementation replaces the current file processing logic in `GarminCsvImportService` with a **two-phase commit pattern** that ensures atomicity and provides complete reconciliation tracking.

**Key Changes:**
- Files only move to "processed" folder when ALL rows are successfully inserted
- Partial failures are quarantined with retry manifests
- Complete audit trail with database-backed reconciliation status
- API endpoints for monitoring import health

---

## Files Created

### New Entities & Services

1. **FileImportRecord.java** - JPA entity for tracking import metrics
2. **ProcessingStatus.java** - Enum for lifecycle status
3. **ReconciliationStatus.java** - Enum for reconciliation verification
4. **FileImportRecordRepository.java** - Data access layer
5. **FileImportRecordService.java** - Business logic interface
6. **FileImportRecordServiceImpl.java** - Implementation
7. **ReconciliationService.java** - Reconciliation verification logic

### DTOs & Resources

8. **FileImportRecordDTO.java** - Data transfer object
9. **ReconciliationReportDTO.java** - Detailed reconciliation report
10. **FileImportRecordResource.java** - REST endpoints

### Database

11. **V006__CreateFileImportRecordTable.sql** - Flyway migration

### Documentation

12. **IMPORT_RECONCILIATION_DESIGN.md** - Complete design document
13. **IMPLEMENTATION_GUIDE.md** - This file

---

## Integration with GarminCsvImportService

### Step 1: Add Dependencies

```java
@Service
@Slf4j
public class GarminCsvImportService {

    // ... existing dependencies ...

    // ADD THESE:
    private final FileImportRecordService fileImportRecordService;
    private final ReconciliationService reconciliationService;

    public GarminCsvImportService(
        GarminCsvParser csvParser,
        GarminRunService garminRunService,
        GarminRunRepository garminRunRepository,
        RabbitTemplate rabbitTemplate,
        List<CsvFileProvider> csvFileProviders,
        GarminCsvImportProperties properties,
        FileImportRecordService fileImportRecordService,      // ADD
        ReconciliationService reconciliationService            // ADD
    ) {
        // ... existing assignments ...
        this.fileImportRecordService = fileImportRecordService;
        this.reconciliationService = reconciliationService;
    }
}
```

### Step 2: Replace processFile() Method

The core logic needs to change from:
```java
// OLD: Process rows → Mark file as processed
processAllRows(rows);
markProcessed(handle);  // ❌ Marks as processed regardless of failures
```

To:
```java
// NEW: Initialize → Process rows → Reconcile → Route file
FileImportRecord importRecord = fileImportRecordService.createImportRecord(
    handle.getFileName(), rows.size()
);

ProcessingStats stats = processAllRows(rows, handle, importRecord);
ReconciliationReport reconcReport = reconciliationService.verify(importRecord, stats);

if (reconcReport.getStatus() == ReconciliationStatus.PASS) {
    markProcessed(handle);  // ✅ Move to processed
} else if (reconcReport.getStatus() == ReconciliationStatus.PARTIAL_PASS) {
    moveToQuarantine(handle);  // ⚠️ Move to quarantine
} else {
    moveToFailedFolder(handle);  // ❌ Move to failed
}
```

### Step 3: Extract ProcessingStats Class

Create a nested class to track metrics:

```java
class ProcessingStats {
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;
    long dbMatchCount = 0;
    List<String> failedRowIds = new ArrayList<>();

    void recordSuccess(String activityId) { successCount++; }
    void recordFailure(String activityId, String error) {
        failedCount++;
        failedRowIds.add(activityId);
    }
    void recordSkipped(String activityId) { skippedCount++; }

    int getTotalProcessed() {
        return successCount + failedCount + skippedCount;
    }
}
```

### Step 4: Refactor processAllRows()

**Key changes:**
- Accept `FileImportRecord` for tracking
- Catch exceptions instead of rethrowing
- Track successes, failures, and skips
- Return `ProcessingStats` instead of void

```java
private ProcessingStats processAllRows(List<FitActivityData> rows, 
                                      CsvFileHandle handle, 
                                      FileImportRecord importRecord) {
    ProcessingStats stats = new ProcessingStats();
    
    for (FitActivityData row : rows) {
        try {
            GarminRun existing = garminRunRepository.findByActivityId(row.getActivityId());
            
            if (existing != null) {
                GarminRunDTO csvDto = mapToDto(row, handle.getFileName());
                if (hasDataChanged(existing, csvDto)) {
                    garminRunService.update(existing.getId(), csvDto);
                    stats.recordSuccess(row.getActivityId());  // ✅ Track success
                } else {
                    stats.recordSkipped(row.getActivityId());  // ⊘ Track skip
                }
            } else {
                GarminRunDTO dto = mapToDto(row, handle.getFileName());
                Long savedId = garminRunService.create(dto);
                stats.recordSuccess(row.getActivityId());      // ✅ Track success
            }
        } catch (Exception e) {
            // CRITICAL CHANGE: Don't rethrow, capture and continue
            stats.recordFailure(row.getActivityId(), e.getMessage());
            log.error("Failed to process row {}: {}", row.getActivityId(), e.getMessage());
            publishFailedEvent(row, handle.getFileName(), e.getMessage());
        }
    }
    
    // Final count
    List<String> csvActivityIds = rows.stream()
        .map(FitActivityData::getActivityId)
        .toList();
    stats.dbMatchCount = garminRunRepository.countByActivityIdIn(csvActivityIds);
    
    return stats;  // Return metrics for reconciliation
}
```

### Step 5: Add Helper Methods for File Routing

```java
private void moveToQuarantine(CsvFileHandle handle) throws IOException {
    // Implementation: Move file to quarantine folder
    // This requires extending the Google Drive file provider
    log.warn("Moving file to quarantine folder: {}", handle.getFileName());
}

private void moveToFailedFolder(CsvFileHandle handle) throws IOException {
    // Implementation: Move file to failed folder
    // This requires extending the Google Drive file provider
    log.error("Moving file to failed folder: {}", handle.getFileName());
}

private void createRetryManifest(FileImportRecord record, List<String> failedRowIds) {
    // Create a CSV with just the failed rows for retry
    // Upload to /retry folder in Google Drive
    log.info("Created retry manifest for {} with {} failed rows",
        record.getFileName(), failedRowIds.size());
}
```

### Step 6: Update markProcessed() Documentation

The current method now has guards:

```java
private void markProcessed(CsvFileHandle handle) {
    try {
        // This is now only called when reconciliation PASSES
        // So we're confident all rows were successfully processed
        handle.markProcessed();
        log.info("File marked as processed: {}", handle.getFileName());
    } catch (IOException e) {
        log.warn("Failed to mark CSV file as processed: {}", handle.getFileName(), e);
    }
}
```

---

## Google Drive File Provider Enhancements

The current `GoogleDriveCsvFileProvider.markProcessed()` moves files to a "processed" folder. To support the new routing logic, extend it:

```java
public interface CsvFileHandle {
    String getFileName();
    InputStream openStream() throws IOException;
    void markProcessed() throws IOException;
    
    // NEW METHODS:
    void moveToFolder(String folderId) throws IOException;    // Move to any folder
    void moveToQuarantine(String quarantineFolderId) throws IOException;
    void moveToFailed(String failedFolderId) throws IOException;
}
```

Implement in `DriveCsvFileHandle`:

```java
private static class DriveCsvFileHandle implements CsvFileHandle {
    // ... existing code ...
    
    @Override
    public void moveToQuarantine(String quarantineFolderId) throws IOException {
        moveToFolder(quarantineFolderId);
    }

    @Override
    public void moveToFailed(String failedFolderId) throws IOException {
        moveToFolder(failedFolderId);
    }

    private void moveToFolder(String targetFolderId) throws IOException {
        if (drive == null) throw new IOException("Google Drive client not initialized");
        if (targetFolderId == null) {
            log.warn("Target folder ID is null, skipping move for: {}", file.getName());
            return;
        }

        File current = drive.files().get(file.getId()).setFields("parents").execute();
        String removeParents = current.getParents() == null || current.getParents().isEmpty()
            ? null
            : String.join(",", current.getParents());

        drive.files().update(file.getId(), new File())
            .setAddParents(targetFolderId)
            .setRemoveParents(removeParents)
            .execute();

        log.info("Moved file {} to target folder", file.getName());
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
@SpringBootTest
class FileImportReconciliationTest {

    @Test
    void testAllRowsSuccess() {
        // 10 rows, all inserted successfully
        // Expected: COMPLETE_SUCCESS + PASS
    }

    @Test
    void testPartialFailure() {
        // 10 rows, 8 inserted, 2 fail
        // Expected: COMPLETE_WITH_FAILURES + PARTIAL_PASS + quarantine
    }

    @Test
    void testAllRowsFail() {
        // 10 rows, all fail
        // Expected: FAILED + FAIL + move to failed folder
    }

    @Test
    void testDuplicate() {
        // 10 rows, 5 duplicates, 5 new
        // Expected: 5 success, 5 skipped, PASS
    }

    @Test
    void testReconciliationMismatch() {
        // 10 rows expected, only 9 processed (1 lost)
        // Expected: FAILED + FAIL (data loss detected)
    }
}
```

### Integration Tests

```java
@SpringBootTest
class GarminCsvImportIntegrationTest {

    @Test
    void testEndToEndImport() throws IOException {
        // Upload test CSV with 50 rows
        // Process and verify all 50 in DB
        // Verify file moved to /processed
        // Verify FileImportRecord has PASS status
    }

    @Test
    void testEndToEndPartialFailure() throws IOException {
        // Upload test CSV with 50 rows
        // Mock 10 rows to fail
        // Verify 40 in DB, 10 failed
        // Verify file moved to /quarantine
        // Verify FileImportRecord has PARTIAL_PASS status
    }
}
```

---

## Monitoring & Alerts

### Dashboard Queries

```sql
-- Files with reconciliation issues
SELECT * FROM file_import_record
WHERE reconciliation_status IN ('FAIL', 'PARTIAL_PASS')
ORDER BY completed_at DESC;

-- Import success rate
SELECT 
    DATE(processed_at) as date,
    COUNT(*) as total_imports,
    SUM(CASE WHEN reconciliation_status = 'PASS' THEN 1 ELSE 0 END) as successful,
    SUM(CASE WHEN reconciliation_status = 'FAIL' THEN 1 ELSE 0 END) as failed,
    ROUND(100.0 * SUM(CASE WHEN reconciliation_status = 'PASS' THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate_pct
FROM file_import_record
GROUP BY DATE(processed_at)
ORDER BY date DESC;

-- Total rows processed
SELECT 
    SUM(expected_row_count) as total_expected,
    SUM(success_count) as total_imported,
    SUM(failed_count) as total_failed,
    SUM(skipped_count) as total_skipped
FROM file_import_record
WHERE status = 'COMPLETE_SUCCESS';
```

### API Endpoints for Monitoring

```bash
# List all failed imports
GET /api/file-import-records?status=FAILED

# List files in quarantine
GET /api/file-import-records?status=COMPLETE_WITH_FAILURES

# Get reconciliation report for specific file
GET /api/file-import-records/reconciliation-report/activities.csv

# List all records requiring attention
GET /api/file-import-records/alert/requires-attention

# Get reconciliation pass rate
GET /api/file-import-records/status/reconciliation-pass
```

---

## Migration Checklist

- [ ] Create new JPA entities (`FileImportRecord`)
- [ ] Create repository interface
- [ ] Create service layer (interface + implementation)
- [ ] Create reconciliation service
- [ ] Run database migration (V006)
- [ ] Update `GarminCsvImportService` with new dependencies
- [ ] Replace `processFile()` logic
- [ ] Add `ProcessingStats` inner class
- [ ] Refactor `processAllRows()` to collect stats
- [ ] Add file routing methods (moveToQuarantine, moveToFailed)
- [ ] Enhance `CsvFileHandle` interface with new methods
- [ ] Create `FileImportRecordResource` REST controller
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Update Google Drive properties config with quarantine/failed folder IDs
- [ ] Deploy migration
- [ ] Deploy new code
- [ ] Monitor first 100 imports
- [ ] Update operations runbooks

---

## Configuration

Add to `application.yml`:

```yaml
garmin:
  csv-import:
    source: DRIVE
    drive:
      application-name: Runs App CSV Importer
      service-account-key-path: /path/to/service-account.json
      folder-id: "your-import-folder-id"
      processed-folder-id: "your-processed-folder-id"
      # NEW:
      quarantine-folder-id: "your-quarantine-folder-id"    # For partial failures
      failed-folder-id: "your-failed-folder-id"            # For complete failures
      retry-folder-id: "your-retry-folder-id"              # For retry manifests
```

---

## Rollback Plan

If issues arise:

1. **Database**: Keep `FILE_IMPORT_RECORD` table (non-destructive, new table)
2. **Code**: Comment out reconciliation checks, fall back to original `markProcessed()`
3. **Google Drive**: Files in quarantine/failed folders will need manual review

```java
// Rollback: Force all files to processed folder regardless of reconciliation
if (true) {  // Temporary flag
    markProcessed(handle);
    log.warn("ROLLBACK: Marking file as processed without reconciliation checks");
    return;
}
```

---

## Performance Considerations

- **Database queries**: Indexed on `status`, `reconciliation_status`, `file_name`
- **File operations**: Move operations are atomic at Google Drive API level
- **Reconciliation**: Happens after all rows processed (no blocking)
- **Storage**: One record per file, negligible storage overhead

**Expected impact**: Minimal (< 5% slower due to tracking overhead)

---

## Next Steps

1. Review the design document: `IMPORT_RECONCILIATION_DESIGN.md`
2. Implement entities and services (already created)
3. Update `GarminCsvImportService` with new logic
4. Run database migration
5. Test with sample CSV files
6. Monitor reconciliation reports
7. Adjust retry manifest creation as needed
