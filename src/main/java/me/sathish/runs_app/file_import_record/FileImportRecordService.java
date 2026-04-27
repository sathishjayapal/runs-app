package me.sathish.runs_app.file_import_record;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FileImportRecordService {

    /**
     * Create a new import record for tracking file processing.
     * Called before processing starts.
     */
    FileImportRecord createImportRecord(String fileName, int expectedRowCount);

    /**
     * Mark import as completed with success counts.
     * Called after all rows are processed and reconciliation passes.
     */
    void markAsCompleted(String fileName, int success, int failed, int skipped,
                        ProcessingStatus status, ReconciliationStatus reconcStatus);

    /**
     * Mark import as completed with additional reconciliation report.
     */
    void markAsCompleted(String fileName, int success, int failed, int skipped,
                        ProcessingStatus status, ReconciliationStatus reconcStatus,
                        String reconcReport);

    /**
     * Mark import as failed with reason.
     */
    void markAsFailed(String fileName, ProcessingStatus status,
                     ReconciliationStatus reconcStatus, String reason);

    /**
     * Get import record by file name.
     */
    FileImportRecord getByFileName(String fileName);

    /**
     * List all import records with given status.
     */
    Page<FileImportRecord> findByStatus(ProcessingStatus status, Pageable pageable);

    /**
     * List all import records with given reconciliation status.
     */
    Page<FileImportRecord> findByReconciliationStatus(ReconciliationStatus status, Pageable pageable);

    /**
     * Increment retry count for a file that failed to process completely.
     * Updates lastRetryAt timestamp.
     * Called when a file needs to be retried after partial failure.
     */
    void incrementRetryCount(String fileName);

    /**
     * Check if an email alert should be sent for a file.
     * Returns true if: retryCount >= maxAttempts AND failedCount > 0 AND emailAlertSentAt == null
     */
    boolean shouldSendEmailAlert(FileImportRecord record, int maxRetryAttempts);

    /**
     * Mark that an email alert has been sent for a file.
     * Prevents duplicate alerts from being sent.
     */
    void markEmailAlertSent(String fileName);

    /**
     * Update failure details (JSON) for a file import record.
     * Used to store detailed failure information for troubleshooting.
     */
    void updateFailureDetails(String fileName, String failureDetailsJson);
}
