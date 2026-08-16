
package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
import me.sathish.runs_app.file_import_record.*;
import me.sathish.runs_app.garmin_run.GarminRun;
import me.sathish.runs_app.garmin_run.GarminRunDTO;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.garmin_run.GarminRunService;
import me.sathish.runs_app.mail.MailService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.amqp.core.MessagePostProcessor;

@Service
@Slf4j
public class GarminCsvImportService {

    private final GarminCsvParser csvParser;
    private final GarminRunService garminRunService;
    private final GarminRunRepository garminRunRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Map<GarminCsvImportProperties.Source, CsvFileProvider> providersBySource;
    private final GarminCsvImportProperties properties;
    private final FileImportRecordService fileImportRecordService;
    private final ReconciliationService reconciliationService;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    public GarminCsvImportService(GarminCsvParser csvParser,
                                  GarminRunService garminRunService,
                                  GarminRunRepository garminRunRepository,
                                  RabbitTemplate rabbitTemplate,
                                  List<CsvFileProvider> csvFileProviders,
                                  GarminCsvImportProperties properties,
                                  FileImportRecordService fileImportRecordService,
                                  ReconciliationService reconciliationService,
                                  MailService mailService,
                                  ObjectMapper objectMapper) {
        this.csvParser = csvParser;
        this.garminRunService = garminRunService;
        this.garminRunRepository = garminRunRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.fileImportRecordService = fileImportRecordService;
        this.reconciliationService = reconciliationService;
        this.mailService = mailService;
        this.objectMapper = objectMapper;
        this.providersBySource = csvFileProviders.stream()
                .collect(Collectors.toUnmodifiableMap(CsvFileProvider::getSourceType, Function.identity()));
    }

    public ImportResult processImportFolder(String folderOverride) {
        GarminCsvImportProperties.Source source = properties.getSource();
        CsvFileProvider provider = providersBySource.get(source);

        ImportResult result = new ImportResult();

        if (provider == null) {
            log.warn("No CSV file provider registered for source {}", source);
            publishSummary(result, 0, 0, UUID.randomUUID().toString());
            return result;
        }

        log.info("Starting Garmin CSV import using source {}", source);

        List<CsvFileHandle> csvFiles;
        try {
            csvFiles = provider.listCsvFiles(folderOverride);
        } catch (IOException e) {
            log.error("Failed to list CSV files for source {}", source, e);
            publishSummary(result, 0, 0, UUID.randomUUID().toString());
            return result;
        }

        if (csvFiles.isEmpty()) {
            log.info("No CSV files available for source {}", source);
            publishSummary(result, 0, 0, UUID.randomUUID().toString());
            return result;
        }

        for (CsvFileHandle handle : csvFiles) {
            processFileWithReconciliation(handle, result);
        }

        return result;
    }

    /**
     * THREE-PHASE ATOMIC FILE PROCESSING WITH RECONCILIATION
     *
     * Phase 1: INITIALIZE - Parse CSV and create tracking record
     * Phase 2: PROCESS - Import all rows without throwing (track success/fail/skip)
     * Phase 3: RECONCILE & ROUTE - Verify all rows accounted for, route file appropriately
     */
    private void processFileWithReconciliation(CsvFileHandle handle, ImportResult result) {
        String fileName = handle.getFileName();
        // One correlation ID per file so its full journey (publish -> eventstracker consume ->
        // persist) can be traced as a single thread in sathishlogger.
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting import for file={} correlationId={}", fileName, correlationId);

        // PHASE 1: INITIALIZE - Parse CSV and create tracking record
        List<FitActivityData> rows;
        try (InputStream stream = handle.openStream()) {
            rows = csvParser.parse(stream, fileName);
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", fileName, e);
            result.addFailed(fileName, e.getMessage());
            publishSummary(result, 0, 0, correlationId);
            return;
        }

        if (rows.isEmpty()) {
            log.info("No parseable rows found in: {}", fileName);
            publishSummary(result, 0, 0, correlationId);
            markProcessed(handle);
            return;
        }

        // Create import record with expected row count
        FileImportRecord importRecord = fileImportRecordService.createImportRecord(fileName, rows.size());
        log.info("Created import record for {} with {} expected rows", fileName, rows.size());

        // PHASE 2: PROCESS - Process all rows without throwing exceptions
        ProcessingStats stats = processAllRows(rows, handle, importRecord, result, correlationId);

        // PHASE 3: RECONCILE & ROUTE - Verify counts and route file to appropriate folder
        reconcileAndRoute(fileName, importRecord, stats, handle, result, correlationId);
    }

    /**
     * PHASE 2: Process all rows, tracking success/fail/skip without rethrowing.
     * Returns ProcessingStats with counts and failure details.
     */
    private ProcessingStats processAllRows(List<FitActivityData> rows, CsvFileHandle handle,
                                          FileImportRecord importRecord, ImportResult result, String correlationId) {
        ProcessingStats stats = new ProcessingStats();
        List<String> csvActivityIds = rows.stream().map(FitActivityData::getActivityId).toList();

        for (FitActivityData row : rows) {
            try {
                GarminRun existingRun = garminRunRepository.findByActivityId(row.getActivityId());

                if (existingRun != null) {
                    GarminRunDTO csvDto = mapToDto(row, handle.getFileName());

                    if (hasDataChanged(existingRun, csvDto)) {
                        // Update existing record
                        garminRunService.update(existingRun.getId(), csvDto);
                        stats.recordSuccess();
                        result.addUpdated(row.getActivityDate() + " " + row.getActivityName());
                        publishUpdatedEvent(csvDto, existingRun.getId(), handle.getFileName(), correlationId);
                        log.debug("Updated CSV activity: {}", row.getActivityId());
                    } else {
                        // Skip unchanged record
                        stats.recordSkip();
                        result.addSkipped(row.getActivityDate() + " " + row.getActivityName());
                        publishSkippedEvent(row, handle.getFileName(), correlationId);
                        log.debug("Skipped unchanged activity: {}", row.getActivityId());
                    }
                } else {
                    // Insert new activity
                    GarminRunDTO dto = mapToDto(row, handle.getFileName());
                    Long savedId = garminRunService.create(dto);
                    stats.recordSuccess();
                    result.addSuccess(row.getActivityDate() + " " + row.getActivityName());
                    publishActivityEvent(dto, savedId, handle.getFileName(), correlationId);
                    log.debug("Imported CSV activity: {}", row.getActivityId());
                }
            } catch (Exception e) {
                // Record failure WITHOUT rethrowing (continues processing remaining rows)
                stats.recordFailure(row.getActivityId(), e.getMessage());
                result.addFailed(row.getActivityId(), e.getMessage());
                publishFailedEvent(row, handle.getFileName(), e.getMessage(), correlationId);
                log.warn("Failed to process activity {}: {}", row.getActivityId(), e.getMessage());
            }
        }

        // Count DB records for validation
        long dbMatchCount = garminRunRepository.countByActivityIdIn(csvActivityIds);
        stats.setDbMatchCount(dbMatchCount);

        log.info("Processed {} rows: {} success, {} failed, {} skipped (DB match: {})",
                rows.size(), stats.successCount, stats.failedCount, stats.skippedCount, dbMatchCount);

        return stats;
    }

    /**
     * PHASE 3: Reconcile (expected == processed) and route file to appropriate folder.
     *
     * PASS: expected == success + failed + skipped → move to /processed folder
     * PARTIAL_PASS: expected == success + failed + skipped but failed > 0 → move to /quarantine, create retry manifest, check email
     * FAIL: expected != processed → move to /failed folder
     */
    private void reconcileAndRoute(String fileName, FileImportRecord importRecord, ProcessingStats stats,
                                   CsvFileHandle handle, ImportResult result, String correlationId) {
        // Verify reconciliation
        ReconciliationReport reconcReport = reconciliationService.verify(importRecord, stats);
        ReconciliationStatus reconcStatus = reconcReport.getStatus();
        log.info("Reconciliation for {}: {} (Expected: {}, Processed: {})",
                fileName, reconcStatus, importRecord.getExpectedRowCount(), stats.getTotalProcessed());

        if (reconcStatus == ReconciliationStatus.PASS) {
            // All rows processed, all successful → move to /processed folder
            fileImportRecordService.markAsCompleted(fileName, stats.successCount, stats.failedCount,
                    stats.skippedCount, ProcessingStatus.COMPLETE_SUCCESS, ReconciliationStatus.PASS,
                    "All rows processed successfully");
            markProcessed(handle);
            publishSummary(result, stats.getTotalProcessed(), stats.dbMatchCount, correlationId);
            log.info("File PASSED reconciliation and moved to processed folder: {}", fileName);

        } else if (reconcStatus == ReconciliationStatus.PARTIAL_PASS) {
            // All rows processed but some failed → move to /quarantine folder with retry manifest
            fileImportRecordService.markAsCompleted(fileName, stats.successCount, stats.failedCount,
                    stats.skippedCount, ProcessingStatus.COMPLETE_WITH_FAILURES, ReconciliationStatus.PARTIAL_PASS,
                    "All rows processed but " + stats.failedCount + " rows failed");

            moveToQuarantine(handle);
            createRetryManifest(fileName, stats);
            checkAndSendFailureAlert(fileName);
            publishSummary(result, stats.getTotalProcessed(), stats.dbMatchCount, correlationId);
            log.warn("File FAILED reconciliation (partial pass) and moved to quarantine: {} ({} failures)",
                    fileName, stats.failedCount);

        } else {
            // Some rows lost during processing → move to /failed folder
            fileImportRecordService.markAsFailed(fileName, ProcessingStatus.FAILED, ReconciliationStatus.FAIL,
                    "Expected " + importRecord.getExpectedRowCount() + " rows but only processed " +
                            stats.getTotalProcessed() + " rows");

            moveToFailedFolder(handle);
            publishSummary(result, stats.getTotalProcessed(), stats.dbMatchCount, correlationId);
            log.error("File FAILED reconciliation (lost rows) and moved to failed folder: {} (Expected: {}, Got: {})",
                    fileName, importRecord.getExpectedRowCount(), stats.getTotalProcessed());
        }
    }

    /**
     * Check if failure alert should be sent (retry count >= max AND has failures AND not yet sent).
     * If yes, increment retry count and send email alert.
     */
    private void checkAndSendFailureAlert(String fileName) {
        try {
            FileImportRecord record = fileImportRecordService.getByFileName(fileName);
            int maxRetryAttempts = properties.getAlert().getEmail().getMaxRetryAttempts();

            // Increment retry count
            fileImportRecordService.incrementRetryCount(fileName);

            // Reload record to get updated retry count
            record = fileImportRecordService.getByFileName(fileName);

            // Check if alert should be sent
            if (fileImportRecordService.shouldSendEmailAlert(record, maxRetryAttempts)) {
                sendFailureAlert(record);
            }
        } catch (Exception e) {
            log.error("Failed to check/send failure alert for: {}", fileName, e);
        }
    }

    /**
     * Send email alert for file failures.
     * Checks configuration and builds detailed email with failure information.
     */
    private void sendFailureAlert(FileImportRecord record) {
        try {
            GarminCsvImportProperties.Alert.AlertEmail emailConfig = properties.getAlert().getEmail();

            if (!emailConfig.isEnabled() || !emailConfig.hasRecipients() || mailService == null) {
                log.debug("Email alerts disabled or no recipients configured");
                return;
            }

            String subject = String.format("Garmin CSV Import Alert: %s (Retry #%d)",
                    record.getFileName(), record.getRetryCount());

            String body = String.format(
                    "File: %s\n" +
                            "Status: %s\n" +
                            "Reconciliation: %s\n" +
                            "\n" +
                            "Row Counts:\n" +
                            "  Expected: %d\n" +
                            "  Success: %d\n" +
                            "  Failed: %d\n" +
                            "  Skipped: %d\n" +
                            "\n" +
                            "Retry Attempt: %d of %d\n" +
                            "\n" +
                            "Details: See reconciliation report for failed rows\n" +
                            "API Endpoint: GET /api/file-import-records/reconciliation-report/%s\n",
                    record.getFileName(),
                    record.getStatus(),
                    record.getReconciliationStatus(),
                    record.getExpectedRowCount(),
                    record.getSuccessCount(),
                    record.getFailedCount(),
                    record.getSkippedCount(),
                    record.getRetryCount(),
                    emailConfig.getMaxRetryAttempts(),
                    record.getFileName()
            );

            // Send to each recipient
            for (String recipient : emailConfig.getRecipientsList()) {
                mailService.sendMail(recipient, subject, body);
                log.info("Sent failure alert email to: {} for file: {}", recipient, record.getFileName());
            }

            // Mark alert as sent
            fileImportRecordService.markEmailAlertSent(record.getFileName());

        } catch (Exception e) {
            log.error("Failed to send failure alert email for file: {}", record.getFileName(), e);
        }
    }

    /**
     * Create retry manifest file with failed row details.
     * This manifest can be used to reprocess just the failed rows.
     */
    private void createRetryManifest(String fileName, ProcessingStats stats) {
        try {
            if (stats.failedRowDetails.isEmpty()) {
                log.debug("No failed rows to create manifest for: {}", fileName);
                return;
            }

            String manifestJson = objectMapper.writeValueAsString(stats.failedRowDetails);
            fileImportRecordService.updateFailureDetails(fileName, manifestJson);
            log.info("Created retry manifest for {} with {} failed rows", fileName, stats.failedRowDetails.size());

            // TODO: Move retry manifest to retry folder (requires CsvFileProvider enhancement)
            // moveRetryManifestToFolder(fileName, stats.failedRowDetails);

        } catch (Exception e) {
            log.error("Failed to create retry manifest for: {}", fileName, e);
        }
    }

    private void moveToQuarantine(CsvFileHandle handle) {
        try {
            // TODO: Implement actual folder movement to quarantine
            // handle.moveToFolder(properties.getDrive().getQuarantineFolderId());
            log.info("TODO: Move file to quarantine folder: {}", handle.getFileName());
        } catch (Exception e) {
            log.warn("Failed to move file to quarantine: {}", handle.getFileName(), e);
        }
    }

    private void moveToFailedFolder(CsvFileHandle handle) {
        try {
            // TODO: Implement actual folder movement to failed folder
            // handle.moveToFolder(properties.getDrive().getFailedFolderId());
            log.info("TODO: Move file to failed folder: {}", handle.getFileName());
        } catch (Exception e) {
            log.warn("Failed to move file to failed folder: {}", handle.getFileName(), e);
        }
    }

    private GarminRunDTO mapToDto(FitActivityData data, String sourceFile) {
        GarminRunDTO dto = new GarminRunDTO();
        dto.setActivityId(data.getActivityId());
        dto.setActivityDate(data.getActivityDate());
        dto.setActivityType(data.getActivityType());
        dto.setActivityName(data.getActivityName());
        dto.setActivityDescription("Imported from CSV: " + sourceFile);
        dto.setElapsedTime(data.getFormattedElapsedTime());
        dto.setDistance(data.getDistanceMiles() != null ? String.format("%.2f", data.getDistanceMiles()) : "0.00");
        dto.setMaxHeartRate(data.getMaxHeartRate() != null ? String.valueOf(data.getMaxHeartRate()) : null);
        dto.setCalories(data.getCalories() != null ? String.valueOf(data.getCalories()) : null);
        dto.setCreatedBy(properties.getSystemUserId());
        return dto;
    }

    private MessagePostProcessor withCorrelationId(String correlationId) {
        return message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            return message;
        };
    }

    private void publishActivityEvent(GarminRunDTO dto, Long savedId, String fileName, String correlationId) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(dto.getActivityId());
            event.setActivityName(dto.getActivityName());
            event.setActivityDate(Instant.now());
            event.setDistance(dto.getDistance());
            event.setElapsedTime(dto.getElapsedTime());
            event.setDatabaseId(savedId);
            event.setStatus("SUCCESS");
            event.setFileName(fileName);
            event.setActivityType(dto.getActivityType());
            event.setMaxHeartRate(dto.getMaxHeartRate());
            event.setCalories(dto.getCalories());

            // Publish to API queue (eventstracker for audit) as a JSON object so the broker serializes it once
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                    event,
                    withCorrelationId(correlationId));
            log.debug("Published SUCCESS event to API queue for CSV activity: {}", dto.getActivityId());

            // Publish to OPS queue (runs-ai-analyzer for analysis) as JSON text for the existing String-based listener
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY,
                objectMapper.writeValueAsString(event),
                withCorrelationId(correlationId));
            log.debug("Published SUCCESS event to OPS queue for CSV activity: {}", dto.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish event for CSV activity: {}", dto.getActivityId(), e);
        }
    }

    private void publishSkippedEvent(FitActivityData data, String fileName, String correlationId) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(data.getActivityId());
            event.setActivityName(data.getActivityName());
            event.setStatus("SKIPPED");
            event.setFileName(fileName);
            event.setErrorMessage("Activity already exists in database");

            // Publish to API queue only (eventstracker for audit)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                event,
                withCorrelationId(correlationId));
            log.debug("Published SKIPPED event to API queue for CSV activity: {}", data.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish SKIPPED event for CSV activity: {}", data.getActivityId(), e);
        }
    }

    private void publishFailedEvent(FitActivityData data, String fileName, String errorMessage, String correlationId) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(data.getActivityId());
            event.setActivityName(data.getActivityName());
            event.setStatus("FAILED");
            event.setFileName(fileName);
            event.setErrorMessage(errorMessage);

            // Publish to API queue only (eventstracker for audit)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                event,
                withCorrelationId(correlationId));
            log.debug("Published FAILED event to API queue for CSV activity: {}", data.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish FAILED event for CSV activity: {}", data.getActivityId(), e);
        }
    }

    private void publishSummary(ImportResult result, long csvTotal, long dbMatchCount, String correlationId) {
        String reconciliation = buildReconciliationStatus(csvTotal, dbMatchCount);

        String summary = String.format(
                "CSV import complete. Imported: %d, Updated: %d, Skipped: %d, Failed: %d | Reconciliation: %s",
                result.getSuccessCount(), result.getUpdatedCount(), result.getSkippedCount(), result.getFailedCount(), reconciliation);

        log.info(summary);
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_SUMMARY");
            event.setStatus(result.getFailedCount() > 0 ? "PARTIAL" : "SUCCESS");
            event.setErrorMessage(summary);
            event.setActivityDate(Instant.now());

            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                event,
                withCorrelationId(correlationId));
        } catch (Exception e) {
            log.error("Failed to publish CSV import summary to RabbitMQ", e);
        }
    }

    private String buildReconciliationStatus(long csvTotal, long dbMatchCount) {
        if (csvTotal == 0) return "N/A (no rows in CSV)";
        return dbMatchCount == csvTotal
                ? String.format("PASS (%d/%d in DB)", dbMatchCount, csvTotal)
                : String.format("FAIL (%d/%d in DB)", dbMatchCount, csvTotal);
    }

    private void markProcessed(CsvFileHandle handle) {
        try {
            handle.markProcessed();
        } catch (IOException e) {
            log.warn("Failed to mark CSV file as processed: {}", handle.getFileName(), e);
        }
    }

    /**
     * Compares existing database record with CSV data to detect changes.
     * Compares key fields: activityName, activityType, distance, elapsedTime, calories, maxHeartRate
     */
    private boolean hasDataChanged(GarminRun existingRun, GarminRunDTO csvDto) {
        // Compare activity name
        if (!safeEquals(existingRun.getActivityName(), csvDto.getActivityName())) {
            log.debug("Activity name changed: '{}' -> '{}'", existingRun.getActivityName(), csvDto.getActivityName());
            return true;
        }

        // Compare activity type
        if (!safeEquals(existingRun.getActivityType(), csvDto.getActivityType())) {
            log.debug("Activity type changed: '{}' -> '{}'", existingRun.getActivityType(), csvDto.getActivityType());
            return true;
        }

        // Compare distance
        if (!safeEquals(existingRun.getDistance(), csvDto.getDistance())) {
            log.debug("Distance changed: '{}' -> '{}'", existingRun.getDistance(), csvDto.getDistance());
            return true;
        }

        // Compare elapsed time
        if (!safeEquals(existingRun.getElapsedTime(), csvDto.getElapsedTime())) {
            log.debug("Elapsed time changed: '{}' -> '{}'", existingRun.getElapsedTime(), csvDto.getElapsedTime());
            return true;
        }

        // Compare calories
        if (!safeEquals(existingRun.getCalories(), csvDto.getCalories())) {
            log.debug("Calories changed: '{}' -> '{}'", existingRun.getCalories(), csvDto.getCalories());
            return true;
        }

        // Compare max heart rate
        if (!safeEquals(existingRun.getMaxHeartRate(), csvDto.getMaxHeartRate())) {
            log.debug("Max heart rate changed: '{}' -> '{}'", existingRun.getMaxHeartRate(), csvDto.getMaxHeartRate());
            return true;
        }

        return false;
    }

    /**
     * Null-safe string comparison
     */
    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private void publishUpdatedEvent(GarminRunDTO dto, Long updatedId, String fileName, String correlationId) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(dto.getActivityId());
            event.setActivityName(dto.getActivityName());
            event.setActivityDate(Instant.now());
            event.setDistance(dto.getDistance());
            event.setElapsedTime(dto.getElapsedTime());
            event.setDatabaseId(updatedId);
            event.setStatus("UPDATED");
            event.setFileName(fileName);
            event.setActivityType(dto.getActivityType());
            event.setMaxHeartRate(dto.getMaxHeartRate());
            event.setCalories(dto.getCalories());

            // Publish to API queue (eventstracker for audit) as a JSON object so the broker serializes it once
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                    event,
                    withCorrelationId(correlationId));
            log.debug("Published UPDATED event to API queue for CSV activity: {}", dto.getActivityId());

            // Publish to OPS queue (runs-ai-analyzer for analysis) as JSON text for the existing String-based listener
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY,
                objectMapper.writeValueAsString(event),
                withCorrelationId(correlationId));
            log.debug("Published UPDATED event to OPS queue for CSV activity: {}", dto.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish UPDATED event for CSV activity: {}", dto.getActivityId(), e);
        }
    }

}
