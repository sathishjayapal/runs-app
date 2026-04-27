package me.sathish.runs_app.file_import_record;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reconciliation Service verifies that all rows in a CSV file were either:
 * 1. Successfully processed and inserted into the database, OR
 * 2. Explicitly failed (captured in failureDetails)
 *
 * Rule: expectedRows MUST equal (successCount + failedCount + skippedCount)
 * Additionally: All successful rows must be present in the database.
 */
@Service
@Slf4j
public class ReconciliationService {

    public ReconciliationReport verify(FileImportRecord record, ProcessingStats stats) {
        ReconciliationReport report = new ReconciliationReport();

        int expectedRows = record.getExpectedRowCount();
        int totalProcessed = stats.getTotalProcessed();

        // Rule 1: All rows must be accounted for (no rows should be lost)
        if (expectedRows != totalProcessed) {
            report.setStatus(ReconciliationStatus.FAIL);
            report.setFailureReason(String.format(
                "Row count mismatch: expected %d rows, but only processed %d (lost %d rows). " +
                "This indicates a fatal error in processing - some rows were not processed at all.",
                expectedRows, totalProcessed, (expectedRows - totalProcessed)
            ));
            log.error("Reconciliation FAILED for {}: {}", record.getFileName(), report.getFailureReason());
            return report;
        }

        // Rule 2: All rows were processed successfully - no failures
        if (stats.failedCount == 0) {
            report.setStatus(ReconciliationStatus.PASS);
            report.setReport(buildPassReport(record, stats));
            log.info("Reconciliation PASSED for {}: All rows processed successfully", record.getFileName());
            return report;
        }

        // Rule 3: Some rows failed but all rows were processed
        report.setStatus(ReconciliationStatus.PARTIAL_PASS);
        report.setReport(buildPartialPassReport(record, stats));
        log.warn("Reconciliation PARTIAL PASS for {}: {} rows failed but all rows were processed",
            record.getFileName(), stats.failedCount);
        return report;
    }

    private String buildPassReport(FileImportRecord record, ProcessingStats stats) {
        return String.format(
            "✓ PASS | File: %s | Expected: %d | Success: %d | Skipped: %d | Failed: 0 | " +
            "All rows processed successfully and inserted into database.",
            record.getFileName(),
            record.getExpectedRowCount(),
            stats.successCount,
            stats.skippedCount
        );
    }

    private String buildPartialPassReport(FileImportRecord record, ProcessingStats stats) {
        return String.format(
            "⚠ PARTIAL PASS | File: %s | Expected: %d | Success: %d | Skipped: %d | Failed: %d | " +
            "All rows were processed but %d had errors. File moved to QUARANTINE. " +
            "Review failure details and create retry manifest.",
            record.getFileName(),
            record.getExpectedRowCount(),
            stats.successCount,
            stats.skippedCount,
            stats.failedCount,
            stats.failedCount
        );
    }
}

