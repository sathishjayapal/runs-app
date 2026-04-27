package me.sathish.runs_app.file_import_record;

/**
 * Represents the lifecycle status of file import processing.
 */
public enum ProcessingStatus {
    PENDING,                    // Not yet processed
    PROCESSING,                 // Currently processing
    COMPLETE_SUCCESS,          // All rows processed successfully
    COMPLETE_WITH_FAILURES,    // Some rows failed but all rows were processed
    FAILED,                     // Processing failed (either all rows failed or fatal error)
    RETRY_IN_PROGRESS,         // Currently retrying failed rows
    QUARANTINED                // Manual review required
}
