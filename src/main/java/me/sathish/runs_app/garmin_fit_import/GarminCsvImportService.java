
package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
import me.sathish.runs_app.garmin_run.GarminRun;
import me.sathish.runs_app.garmin_run.GarminRunDTO;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.garmin_run.GarminRunService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GarminCsvImportService {

    private final GarminCsvParser csvParser;
    private final GarminRunService garminRunService;
    private final GarminRunRepository garminRunRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Map<GarminCsvImportProperties.Source, CsvFileProvider> providersBySource;
    private final GarminCsvImportProperties properties;

    public GarminCsvImportService(GarminCsvParser csvParser,
                                  GarminRunService garminRunService,
                                  GarminRunRepository garminRunRepository,
                                  RabbitTemplate rabbitTemplate,
                                  List<CsvFileProvider> csvFileProviders,
                                  GarminCsvImportProperties properties) {
        this.csvParser = csvParser;
        this.garminRunService = garminRunService;
        this.garminRunRepository = garminRunRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.providersBySource = csvFileProviders.stream()
                .collect(Collectors.toUnmodifiableMap(CsvFileProvider::getSourceType, Function.identity()));
    }

    @Transactional
    public ImportResult processImportFolder(String folderOverride) {
        GarminCsvImportProperties.Source source = properties.getSource();
        CsvFileProvider provider = providersBySource.get(source);

        ImportResult result = new ImportResult();

        if (provider == null) {
            log.warn("No CSV file provider registered for source {}", source);
            publishSummary(result, 0, 0);
            return result;
        }

        log.info("Starting Garmin CSV import using source {}", source);

        List<CsvFileHandle> csvFiles;
        try {
            csvFiles = provider.listCsvFiles(folderOverride);
        } catch (IOException e) {
            log.error("Failed to list CSV files for source {}", source, e);
            publishSummary(result, 0, 0);
            return result;
        }

        if (csvFiles.isEmpty()) {
            log.info("No CSV files available for source {}", source);
            publishSummary(result, 0, 0);
            return result;
        }

        for (CsvFileHandle handle : csvFiles) {
            processFile(handle, result);
        }

        return result;
    }

    private void processFile(CsvFileHandle handle, ImportResult result) {
        List<FitActivityData> rows;
        try (InputStream stream = handle.openStream()) {
            rows = csvParser.parse(stream, handle.getFileName());
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", handle.getFileName(), e);
            result.addFailed(handle.getFileName(), e.getMessage());
            publishSummary(result, 0, 0);
            return;
        }

        if (rows.isEmpty()) {
            log.info("No parseable rows found in: {}", handle.getFileName());
            publishSummary(result, 0, 0);
            markProcessed(handle);
            return;
        }

        List<String> csvActivityIds = rows.stream().map(FitActivityData::getActivityId).toList();

        for (FitActivityData row : rows) {
            GarminRun existingRun = garminRunRepository.findByActivityId(row.getActivityId());
            
            if (existingRun != null) {
                // Activity exists - check if data has changed
                GarminRunDTO csvDto = mapToDto(row, handle.getFileName());
                
                if (hasDataChanged(existingRun, csvDto)) {
                    // Data changed - update the existing record
                    try {
                        garminRunService.update(existingRun.getId(), csvDto);
                        result.addUpdated(row.getActivityDate() + " " + row.getActivityName());
                        log.info("Updated CSV activity: {} (DB id: {})", row.getActivityId(), existingRun.getId());
                        
                        publishUpdatedEvent(csvDto, existingRun.getId(), handle.getFileName());
                    } catch (Exception e) {
                        log.error("Failed to update CSV activity: {}", row.getActivityId(), e);
                        result.addFailed(row.getActivityId(), e.getMessage());
                        publishFailedEvent(row, handle.getFileName(), e.getMessage());
                    }
                } else {
                    // Data unchanged - skip
                    log.debug("Activity already exists with same data, skipping: {} {}", row.getActivityDate(), row.getActivityName());
                    result.addSkipped(row.getActivityDate() + " " + row.getActivityName());
                    publishSkippedEvent(row, handle.getFileName());
                }
            } else {
                // New activity - insert
                try {
                    GarminRunDTO dto = mapToDto(row, handle.getFileName());
                    Long savedId = garminRunService.create(dto);
                    publishActivityEvent(dto, savedId, handle.getFileName());
                    result.addSuccess(row.getActivityDate() + " " + row.getActivityName());
                    log.info("Imported CSV activity: {} (DB id: {})", row.getActivityId(), savedId);
                } catch (Exception e) {
                    log.error("Failed to save CSV activity: {}", row.getActivityId(), e);
                    result.addFailed(row.getActivityId(), e.getMessage());
                    publishFailedEvent(row, handle.getFileName(), e.getMessage());
                }
            }
        }

        long dbMatchCount = garminRunRepository.countByActivityIdIn(csvActivityIds);
        publishSummary(result, csvActivityIds.size(), dbMatchCount);
        markProcessed(handle);
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

    private void publishActivityEvent(GarminRunDTO dto, Long savedId, String fileName) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(dto.getActivityId());
            event.setActivityName(dto.getActivityName());
            event.setActivityDate(LocalDateTime.now());
            event.setDistance(dto.getDistance());
            event.setElapsedTime(dto.getElapsedTime());
            event.setDatabaseId(savedId);
            event.setStatus("SUCCESS");
            event.setFileName(fileName);
            event.setActivityType(dto.getActivityType());
            event.setMaxHeartRate(dto.getMaxHeartRate());
            event.setCalories(dto.getCalories());
            
            // Publish to API queue (eventstracker for audit)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                event);
            log.debug("Published SUCCESS event to API queue for CSV activity: {}", dto.getActivityId());
            
            // Publish to OPS queue (runs-ai-analyzer for analysis)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY,
                event);
            log.debug("Published SUCCESS event to OPS queue for CSV activity: {}", dto.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish event for CSV activity: {}", dto.getActivityId(), e);
        }
    }

    private void publishSkippedEvent(FitActivityData data, String fileName) {
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
                event);
            log.debug("Published SKIPPED event to API queue for CSV activity: {}", data.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish SKIPPED event for CSV activity: {}", data.getActivityId(), e);
        }
    }

    private void publishFailedEvent(FitActivityData data, String fileName, String errorMessage) {
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
                event);
            log.debug("Published FAILED event to API queue for CSV activity: {}", data.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish FAILED event for CSV activity: {}", data.getActivityId(), e);
        }
    }

    private void publishSummary(ImportResult result, long csvTotal, long dbMatchCount) {
        String reconciliation = buildReconciliationStatus(csvTotal, dbMatchCount);

        String summary = String.format(
                "CSV import complete. Imported: %d, Updated: %d, Skipped: %d, Failed: %d | Reconciliation: %s",
                result.getSuccessCount(), result.getUpdatedCount(), result.getSkippedCount(), result.getFailedCount(), reconciliation);

        log.info(summary);
        try {
            // Publish summary to API queue only (eventstracker for audit)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                summary);
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

    private void publishUpdatedEvent(GarminRunDTO dto, Long updatedId, String fileName) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(dto.getActivityId());
            event.setActivityName(dto.getActivityName());
            event.setActivityDate(LocalDateTime.now());
            event.setDistance(dto.getDistance());
            event.setElapsedTime(dto.getElapsedTime());
            event.setDatabaseId(updatedId);
            event.setStatus("UPDATED");
            event.setFileName(fileName);
            event.setActivityType(dto.getActivityType());
            event.setMaxHeartRate(dto.getMaxHeartRate());
            event.setCalories(dto.getCalories());
            
            // Publish to API queue (eventstracker for audit)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_API_ROUTING_KEY,
                event);
            log.debug("Published UPDATED event to API queue for CSV activity: {}", dto.getActivityId());
            
            // Publish to OPS queue (runs-ai-analyzer for analysis)
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY,
                event);
            log.debug("Published UPDATED event to OPS queue for CSV activity: {}", dto.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish UPDATED event for CSV activity: {}", dto.getActivityId(), e);
        }
    }
}
