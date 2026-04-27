package me.sathish.runs_app.file_import_record;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class FileImportRecordDTO {
    private Long id;
    private String fileName;
    private Integer expectedRows;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private ProcessingStatus status;
    private ReconciliationStatus reconciliationStatus;
    private String reconciliationReport;
    private OffsetDateTime processedAt;
    private OffsetDateTime completedAt;
}
