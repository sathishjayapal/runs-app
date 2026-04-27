package me.sathish.runs_app.file_import_record;

/**
 * Reconciliation status indicates whether the import data matches expectations.
 *
 * Rule: expectedRowCount == (successCount + failedCount + skippedCount)
 * Additionally: All successful rows must be present in the database.
 */
public enum ReconciliationStatus {
    PENDING,       // Not yet reconciled
    PASS,          // ✓ All rows match, all rows in DB
    FAIL,          // ✗ Count mismatch or rows missing from DB
    PARTIAL_PASS   // ⚠ All rows accounted for but some failed (data integrity OK, but needs review)
}
