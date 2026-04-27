package me.sathish.runs_app.file_import_record;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * DTO for reconciliation report endpoint.
 * Provides detailed information about file import reconciliation.
 */
@Data
@Builder
public class ReconciliationReportDTO {
    private String fileName;
    private ProcessingStatus status;
    private ReconciliationStatus reconciliationStatus;
    private Integer expectedRows;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private String report;
    private OffsetDateTime processedAt;
    private OffsetDateTime completedAt;

    /**
     * Get total rows that were actually processed.
     */
    public Integer getTotalProcessed() {
        return (successCount != null ? successCount : 0) +
               (failedCount != null ? failedCount : 0) +
               (skippedCount != null ? skippedCount : 0);
    }

    /**
     * Check if import is ready for processing (pending or processing status).
     */
    public boolean isInProgress() {
        return status == ProcessingStatus.PENDING || status == ProcessingStatus.PROCESSING;
    }

    /**
     * Check if import requires attention (failures or quarantine).
     */
    public boolean requiresAttention() {
        return status == ProcessingStatus.COMPLETE_WITH_FAILURES ||
               status == ProcessingStatus.FAILED ||
               status == ProcessingStatus.QUARANTINED ||
               reconciliationStatus == ReconciliationStatus.FAIL;
    }
}
