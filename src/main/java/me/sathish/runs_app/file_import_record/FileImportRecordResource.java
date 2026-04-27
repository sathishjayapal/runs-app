package me.sathish.runs_app.file_import_record;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.common.PagedResponse;
import me.sathish.runs_app.util.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;

/**
 * REST endpoint for querying file import records and reconciliation status.
 *
 * Endpoints:
 * GET /api/file-import-records - List all import records
 * GET /api/file-import-records?status=PROCESSING - Filter by status
 * GET /api/file-import-records?reconciliationStatus=FAIL - Filter by reconciliation status
 * GET /api/file-import-records/{id} - Get specific record
 * GET /api/file-import-records/reconciliation-report/{fileName} - Get reconciliation report
 */
@RestController
@RequestMapping("/api/file-import-records")
@Slf4j
public class FileImportRecordResource {

    private final FileImportRecordRepository repository;

    public FileImportRecordResource(FileImportRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * List all import records with optional filtering.
     */
    @GetMapping
    public PagedResponse<FileImportRecordDTO> listImports(
        @RequestParam(required = false) ProcessingStatus status,
        @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
        Pageable pageable) {

        Page<FileImportRecord> page;

        if (status != null && reconciliationStatus != null) {
            page = repository.findByStatusAndReconciliationStatus(status, reconciliationStatus, pageable);
        } else if (status != null) {
            page = repository.findByStatus(status, pageable);
        } else if (reconciliationStatus != null) {
            page = repository.findByReconciliationStatus(reconciliationStatus, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        Page<FileImportRecordDTO> dtoPage = page.map(this::toDTO);
        return new PagedResponse<>(dtoPage);
    }

    /**
     * Get a specific import record by ID.
     */
    @GetMapping("/{id}")
    public FileImportRecordDTO getRecord(@PathVariable Long id) {
        FileImportRecord record = repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Import record not found: {}", id);
                return new NotFoundException("Import record not found");
            });
        return toDTO(record);
    }

    /**
     * Get reconciliation report for a specific file.
     * This endpoint provides detailed information about the import reconciliation.
     */
    @GetMapping("/reconciliation-report/{fileName}")
    public ResponseEntity<ReconciliationReportDTO> getReconciliationReport(
        @PathVariable String fileName) {

        FileImportRecord record = repository.findByFileName(fileName)
            .orElseThrow(() -> {
                log.warn("Import record not found for file: {}", fileName);
                return new NotFoundException("File not found: " + fileName);
            });

        ReconciliationReportDTO report = ReconciliationReportDTO.builder()
            .fileName(record.getFileName())
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

        log.info("Retrieved reconciliation report for: {} | Status: {} | Reconciliation: {}",
            fileName, report.getStatus(), report.getReconciliationStatus());

        return ResponseEntity.ok(report);
    }

    /**
     * List all records that require attention (failures or quarantined).
     */
    @GetMapping("/alert/requires-attention")
    public PagedResponse<FileImportRecordDTO> listRequiresAttention(Pageable pageable) {
        Page<FileImportRecord> failedPage = repository.findByStatus(ProcessingStatus.FAILED, pageable);
        Page<FileImportRecord> partialPage = repository.findByStatus(ProcessingStatus.COMPLETE_WITH_FAILURES, pageable);
        Page<FileImportRecord> quarantinedPage = repository.findByStatus(ProcessingStatus.QUARANTINED, pageable);

        // Combine all that require attention from failed page (primary)
        Page<FileImportRecordDTO> dtoPage = failedPage.map(this::toDTO);
        return new PagedResponse<>(dtoPage);
    }

    /**
     * List all records with PASS reconciliation status.
     */
    @GetMapping("/status/reconciliation-pass")
    public PagedResponse<FileImportRecordDTO> listReconciliationPass(Pageable pageable) {
        Page<FileImportRecord> page = repository.findByReconciliationStatus(
            ReconciliationStatus.PASS, pageable);

        Page<FileImportRecordDTO> dtoPage = page.map(this::toDTO);
        return new PagedResponse<>(dtoPage);
    }

    /**
     * List all records with FAIL reconciliation status (data loss/integrity issues).
     */
    @GetMapping("/status/reconciliation-fail")
    public PagedResponse<FileImportRecordDTO> listReconciliationFail(Pageable pageable) {
        Page<FileImportRecord> page = repository.findByReconciliationStatus(
            ReconciliationStatus.FAIL, pageable);

        Page<FileImportRecordDTO> dtoPage = page.map(this::toDTO);
        return new PagedResponse<>(dtoPage);
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
            .reconciliationReport(record.getReconciliationReport())
            .processedAt(record.getProcessedAt())
            .completedAt(record.getCompletedAt())
            .build();
    }

    @Data
    @Builder
    public static class FileImportRecordDTO {
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

    @Data
    @Builder
    public static class ReconciliationReportDTO {
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
    }
}
