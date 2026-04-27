# File Import Reconciliation Design

## Problem Statement
Files are being moved to the "processed" folder before all rows are successfully inserted into the database. When individual row inserts fail (caught in try-catch), the entire file is still marked as processed, leading to:
- Data loss (failed rows go unnoticed)
- No audit trail of partial imports
- No way to retry failed rows

## Solution: Two-Phase Commit with Reconciliation Tracking

### Architecture

```
Phase 1: Initialization
├─ Create FileImportRecord
├─ Count expected rows in CSV
└─ Status = PROCESSING

Phase 2: Row Processing (Transactional)
├─ For each row:
│  ├─ Try insert
│  ├─ If success → successCount++
│  └─ If fail → failedCount++ (collect error details)
└─ Track all results atomically

Phase 3: Reconciliation Verification
├─ Assert: expectedRows == (successCount + failedCount + skippedCount)
├─ Status based on results:
│  ├─ All success → COMPLETE_SUCCESS → move to /processed
│  ├─ Some failed → COMPLETE_WITH_FAILURES → move to /quarantine + create retry manifest
│  └─ All failed → FAILED → move to /failed + alert
└─ Create reconciliation audit record
```

---

## Implementation Steps

### Step 1: Extend FileNameTracker with Metrics

Create new entity `FileImportRecord` that extends tracking:

```java
@Entity
@Getter @Setter
public class FileImportRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Integer expectedRowCount;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer failedCount = 0;

    @Column(nullable = false)
    private Integer skippedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String failureDetails;  // JSON array of failed rows with errors

    @Column(columnDefinition = "TEXT")
    private String reconciliationReport;  // Detailed reconciliation report

    @CreatedDate
    private OffsetDateTime processedAt;

    @LastModifiedDate
    private OffsetDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private RunAppUser createdBy;
}
```

### Step 2: Create Enums

```java
public enum ProcessingStatus {
    PENDING,                    // Not yet processed
    PROCESSING,                 // Currently processing
    COMPLETE_SUCCESS,          // All rows successful
    COMPLETE_WITH_FAILURES,    // Some rows failed
    FAILED,                     // All rows failed
    RETRY_IN_PROGRESS,         // Retrying failed rows
    QUARANTINED                // Manual review needed
}

public enum ReconciliationStatus {
    PENDING,       // Not yet reconciled
    PASS,          // All rows match (expectedRows == success + failed + skipped)
    FAIL,          // Count mismatch (data lost or stuck)
    PARTIAL_PASS   // Most rows processed but some failed
}
```

### Step 3: Update GarminCsvImportService

**Key changes:**
1. Track import record before processing
2. Use atomic row processing with proper error handling
3. Verify reconciliation BEFORE marking as processed
4. Route file based on reconciliation result

```java
@Service
@Slf4j
public class GarminCsvImportService {
    
    private final FileImportRecordService fileImportRecordService;
    private final ReconciliationService reconciliationService;
    // ... other dependencies
    
    @Transactional
    public ImportResult processImportFolder(String folderOverride) {
        // ... existing code ...
        
        for (CsvFileHandle handle : csvFiles) {
            processFileWithReconciliation(handle, result);
        }
        return result;
    }

    private void processFileWithReconciliation(CsvFileHandle handle, ImportResult result) {
        List<FitActivityData> rows;
        try (InputStream stream = handle.openStream()) {
            rows = csvParser.parse(stream, handle.getFileName());
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", handle.getFileName(), e);
            markFileAsFailed(handle, e.getMessage());
            result.addFailed(handle.getFileName(), e.getMessage());
            return;
        }

        if (rows.isEmpty()) {
            log.info("No parseable rows found in: {}", handle.getFileName());
            markFileAsCompleted(handle, 0, 0, 0);
            markProcessed(handle);  // Safe to process empty file
            return;
        }

        // ===== PHASE 1: Initialize import record =====
        FileImportRecord importRecord = fileImportRecordService.createImportRecord(
            handle.getFileName(), 
            rows.size()
        );

        try {
            // ===== PHASE 2: Process all rows (collect failures) =====
            ProcessingStats stats = processAllRows(rows, handle, importRecord);

            // ===== PHASE 3: Verify reconciliation =====
            ReconciliationReport reconcReport = reconciliationService.verify(
                importRecord,
                stats
            );

            // ===== PHASE 4: Route file based on reconciliation =====
            if (reconcReport.getStatus() == ReconciliationStatus.PASS) {
                log.info("Reconciliation PASSED for {}: all {} rows processed successfully", 
                    handle.getFileName(), stats.getTotalProcessed());
                
                markFileAsCompleted(handle, 
                    stats.successCount, 
                    stats.failedCount, 
                    stats.skippedCount);
                markProcessed(handle);  // Move to /processed
                result.addSuccess(handle.getFileName());
                
            } else if (reconcReport.getStatus() == ReconciliationStatus.PARTIAL_PASS) {
                log.warn("Reconciliation PARTIAL for {}: {} success, {} failed, {} skipped",
                    handle.getFileName(), 
                    stats.successCount, 
                    stats.failedCount, 
                    stats.skippedCount);
                
                markFileAsPartiallyProcessed(handle, stats, reconcReport);
                moveToQuarantine(handle);  // Move to /quarantine instead
                createRetryManifest(importRecord, stats.failedRows);
                result.addFailed(handle.getFileName(), 
                    String.format("Partial: %d failed", stats.failedCount));
                    
            } else {
                log.error("Reconciliation FAILED for {}: {} expected, {} found",
                    handle.getFileName(), 
                    importRecord.getExpectedRowCount(),
                    stats.getTotalProcessed());
                
                markFileAsFailed(handle, "Reconciliation failed: " + reconcReport.getFailureReason());
                moveToFailedFolder(handle);  // Move to /failed
                result.addFailed(handle.getFileName(), "Reconciliation failed");
            }

        } catch (Exception e) {
            log.error("Fatal error processing file {}", handle.getFileName(), e);
            markFileAsFailed(handle, e.getMessage());
            moveToFailedFolder(handle);
            result.addFailed(handle.getFileName(), e.getMessage());
        }
    }

    /**
     * Process all rows and collect results atomically.
     * Does NOT move file or modify import status.
     */
    private ProcessingStats processAllRows(List<FitActivityData> rows, 
                                          CsvFileHandle handle, 
                                          FileImportRecord importRecord) {
        ProcessingStats stats = new ProcessingStats();
        List<String> csvActivityIds = rows.stream()
            .map(FitActivityData::getActivityId)
            .toList();

        for (FitActivityData row : rows) {
            try {
                GarminRun existingRun = garminRunRepository.findByActivityId(row.getActivityId());
                
                if (existingRun != null) {
                    GarminRunDTO csvDto = mapToDto(row, handle.getFileName());
                    if (hasDataChanged(existingRun, csvDto)) {
                        garminRunService.update(existingRun.getId(), csvDto);
                        stats.recordSuccess(row.getActivityId());
                        publishUpdatedEvent(csvDto, existingRun.getId(), handle.getFileName());
                    } else {
                        stats.recordSkipped(row.getActivityId());
                        publishSkippedEvent(row, handle.getFileName());
                    }
                } else {
                    GarminRunDTO dto = mapToDto(row, handle.getFileName());
                    Long savedId = garminRunService.create(dto);
                    stats.recordSuccess(row.getActivityId());
                    publishActivityEvent(dto, savedId, handle.getFileName());
                }
            } catch (Exception e) {
                // CRITICAL: Catch and track, don't rethrow
                log.error("Failed to process row {}: {}", row.getActivityId(), e.getMessage());
                stats.recordFailure(row.getActivityId(), e.getMessage());
                publishFailedEvent(row, handle.getFileName(), e.getMessage());
            }
        }

        // Final DB count for verification
        stats.dbMatchCount = garminRunRepository.countByActivityIdIn(csvActivityIds);
        return stats;
    }

    private void markFileAsCompleted(CsvFileHandle handle, 
                                     int success, int failed, int skipped) {
        // Update import record: COMPLETE_SUCCESS + PASS reconciliation
        fileImportRecordService.markAsCompleted(
            handle.getFileName(),
            success, failed, skipped,
            ProcessingStatus.COMPLETE_SUCCESS,
            ReconciliationStatus.PASS
        );
    }

    private void markFileAsPartiallyProcessed(CsvFileHandle handle, 
                                              ProcessingStats stats,
                                              ReconciliationReport report) {
        fileImportRecordService.markAsCompleted(
            handle.getFileName(),
            stats.successCount, stats.failedCount, stats.skippedCount,
            ProcessingStatus.COMPLETE_WITH_FAILURES,
            ReconciliationStatus.PARTIAL_PASS,
            report.getReport()
        );
    }

    private void markFileAsFailed(CsvFileHandle handle, String reason) {
        fileImportRecordService.markAsFailed(
            handle.getFileName(),
            ProcessingStatus.FAILED,
            ReconciliationStatus.FAIL,
            reason
        );
    }

    // Helper to move to quarantine instead of processed
    private void moveToQuarantine(CsvFileHandle handle) throws IOException {
        // Implementation: similar to markProcessed() but moves to quarantine folder
        // handle.moveToFolder(quarantineFolderId);
    }

    private void moveToFailedFolder(CsvFileHandle handle) throws IOException {
        // Implementation: move to /failed folder for manual inspection
        // handle.moveToFolder(failedFolderId);
    }

    private void createRetryManifest(FileImportRecord importRecord, List<String> failedActivityIds) {
        // Create a manifest file with failed row details for retry
        // This can be uploaded back to Google Drive /retry folder
    }
}

// ===== Supporting Classes =====

@Getter
class ProcessingStats {
    int successCount = 0;
    int failedCount = 0;
    int skippedCount = 0;
    long dbMatchCount = 0;
    List<String> failedRows = new ArrayList<>();

    void recordSuccess(String activityId) { successCount++; }
    void recordFailure(String activityId, String error) { 
        failedCount++; 
        failedRows.add(activityId);
    }
    void recordSkipped(String activityId) { skippedCount++; }

    int getTotalProcessed() { 
        return successCount + failedCount + skippedCount; 
    }
}
```

### Step 4: Create ReconciliationService

```java
@Service
@Slf4j
public class ReconciliationService {

    public ReconciliationReport verify(FileImportRecord record, ProcessingStats stats) {
        ReconciliationReport report = new ReconciliationReport();
        
        int expectedRows = record.getExpectedRowCount();
        int totalProcessed = stats.getTotalProcessed();
        int dbCount = (int) stats.dbMatchCount;

        // Rule 1: All rows must be accounted for
        if (expectedRows != totalProcessed) {
            report.setStatus(ReconciliationStatus.FAIL);
            report.setFailureReason(String.format(
                "Row count mismatch: expected %d, processed %d (lost %d rows)",
                expectedRows, totalProcessed, (expectedRows - totalProcessed)
            ));
            return report;
        }

        // Rule 2: Database must have all successful rows
        if (dbCount < stats.successCount) {
            report.setStatus(ReconciliationStatus.FAIL);
            report.setFailureReason(String.format(
                "Database count mismatch: expected %d in DB, found %d",
                stats.successCount, dbCount
            ));
            return report;
        }

        // Rule 3: Classify the result
        if (stats.failedCount == 0) {
            // All rows successful
            report.setStatus(ReconciliationStatus.PASS);
            report.setReport(buildPassReport(record, stats));
        } else {
            // Some rows failed but all rows were processed
            report.setStatus(ReconciliationStatus.PARTIAL_PASS);
            report.setReport(buildPartialPassReport(record, stats));
        }

        return report;
    }

    private String buildPassReport(FileImportRecord record, ProcessingStats stats) {
        return String.format(
            "File: %s | Expected: %d | Success: %d | Skipped: %d | Failed: 0 | Status: ✓ PASS",
            record.getFileName(), 
            record.getExpectedRowCount(),
            stats.successCount,
            stats.skippedCount
        );
    }

    private String buildPartialPassReport(FileImportRecord record, ProcessingStats stats) {
        return String.format(
            "File: %s | Expected: %d | Success: %d | Skipped: %d | Failed: %d | Status: ⚠ PARTIAL | Action: QUARANTINE + RETRY",
            record.getFileName(),
            record.getExpectedRowCount(),
            stats.successCount,
            stats.skippedCount,
            stats.failedCount
        );
    }
}

@Getter @Setter
class ReconciliationReport {
    ReconciliationStatus status;
    String report;
    String failureReason;
}
```

### Step 5: Create FileImportRecordService

```java
@Service
@Slf4j
public class FileImportRecordService {

    private final FileImportRecordRepository repository;

    public FileImportRecord createImportRecord(String fileName, int expectedRowCount) {
        FileImportRecord record = new FileImportRecord();
        record.setFileName(fileName);
        record.setExpectedRowCount(expectedRowCount);
        record.setStatus(ProcessingStatus.PROCESSING);
        record.setReconciliationStatus(ReconciliationStatus.PENDING);
        return repository.save(record);
    }

    public void markAsCompleted(String fileName, int success, int failed, int skipped,
                               ProcessingStatus status, ReconciliationStatus reconcStatus) {
        markAsCompleted(fileName, success, failed, skipped, status, reconcStatus, "");
    }

    public void markAsCompleted(String fileName, int success, int failed, int skipped,
                               ProcessingStatus status, ReconciliationStatus reconcStatus,
                               String reconcReport) {
        FileImportRecord record = repository.findByFileName(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));
        
        record.setSuccessCount(success);
        record.setFailedCount(failed);
        record.setSkippedCount(skipped);
        record.setStatus(status);
        record.setReconciliationStatus(reconcStatus);
        record.setReconciliationReport(reconcReport);
        record.setCompletedAt(OffsetDateTime.now());
        
        repository.save(record);
        log.info("Import record updated: {} | Status: {} | Recon: {}",
            fileName, status, reconcStatus);
    }

    public void markAsFailed(String fileName, ProcessingStatus status, 
                            ReconciliationStatus reconcStatus, String reason) {
        FileImportRecord record = repository.findByFileName(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));
        
        record.setStatus(status);
        record.setReconciliationStatus(reconcStatus);
        record.setReconciliationReport(reason);
        record.setCompletedAt(OffsetDateTime.now());
        
        repository.save(record);
        log.error("Import failed: {} | Reason: {}", fileName, reason);
    }
}
```

### Step 6: Create Diagnostic Endpoint

```java
@RestController
@RequestMapping("/api/import-records")
public class FileImportRecordResource {

    private final FileImportRecordRepository repository;

    @GetMapping
    public Page<FileImportRecordDTO> listImports(
        @RequestParam(defaultValue = "PROCESSING") String status,
        @RequestParam(defaultValue = "0") int page,
        Pageable pageable) {
        
        ProcessingStatus statusFilter = ProcessingStatus.valueOf(status);
        return repository.findByStatus(statusFilter, pageable)
            .map(this::toDTO);
    }

    @GetMapping("/{id}")
    public FileImportRecordDTO getRecord(@PathVariable Long id) {
        return repository.findById(id)
            .map(this::toDTO)
            .orElseThrow(NotFoundException::new);
    }

    @GetMapping("/reconciliation-report/{fileName}")
    public ReconciliationReportDTO getReconciliationReport(@PathVariable String fileName) {
        FileImportRecord record = repository.findByFileName(fileName)
            .orElseThrow(() -> new NotFoundException("File not found: " + fileName));
        
        return ReconciliationReportDTO.builder()
            .fileName(fileName)
            .status(record.getStatus())
            .reconciliationStatus(record.getReconciliationStatus())
            .expectedRows(record.getExpectedRowCount())
            .successCount(record.getSuccessCount())
            .failedCount(record.getFailedCount())
            .skippedCount(record.getSkippedCount())
            .report(record.getReconciliationReport())
            .processedAt(record.getProcessedAt())
            .completedAt(record.getCompletedAt())
            .build();
    }

    private FileImportRecordDTO toDTO(FileImportRecord record) {
        return FileImportRecordDTO.builder()
            .id(record.getId())
            .fileName(record.getFileName())
            .expectedRows(record.getExpectedRowCount())
            .successCount(record.getSuccessCount())
            .failedCount(record.getFailedCount())
            .skippedCount(record.getSkippedCount())
            .status(record.getStatus())
            .reconciliationStatus(record.getReconciliationStatus())
            .completedAt(record.getCompletedAt())
            .build();
    }
}
```

---

## Benefits of This Design

| Aspect | Before | After |
|--------|--------|-------|
| **Atomicity** | Files move before all rows processed | Files only move after reconciliation passes |
| **Auditability** | No tracking of import metrics | Complete audit trail with counts & status |
| **Error Recovery** | Failed rows lost forever | Captured in quarantine, retry manifest created |
| **Reconciliation** | Logs only, no database tracking | Database-backed with verification logic |
| **Monitoring** | No visibility | API endpoints for reconciliation reports |
| **Failure Handling** | Same folder for all failures | Separate quarantine/failed folders |

---

## Database Migrations

```sql
-- Create FileImportRecord table
CREATE TABLE file_import_record (
    id BIGINT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    expected_row_count INTEGER NOT NULL,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reconciliation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_details TEXT,
    reconciliation_report TEXT,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_by_id BIGINT NOT NULL,
    UNIQUE(file_name),
    FOREIGN KEY(created_by_id) REFERENCES run_app_user(id)
);
```

---

## Testing Strategy

1. **Happy Path**: All rows process successfully
2. **Partial Failure**: 80% success, 20% failed
3. **Complete Failure**: All rows fail
4. **Reconciliation Mismatch**: Rows lost in processing
5. **Retry**: Failed rows reprocessed successfully

---

## Rollout Plan

1. ✅ Deploy Phase 1: `FileImportRecord` entity + repository
2. ✅ Deploy Phase 2: `ReconciliationService` + updated `GarminCsvImportService`
3. ✅ Deploy Phase 3: Add diagnostic API endpoint
4. ✅ Monitor: Check reconciliation reports, tune retry logic
5. ✅ Cleanup: Add scheduled job to archive old records after 30 days
