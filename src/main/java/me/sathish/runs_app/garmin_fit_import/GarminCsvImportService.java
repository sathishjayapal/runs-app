
package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
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
            if (garminRunRepository.existsByActivityId(row.getActivityId())) {
                log.debug("Activity already exists in DB, skipping: {} {}", row.getActivityDate(), row.getActivityName());
                result.addSkipped(row.getActivityDate() + " " + row.getActivityName());

                publishSkippedEvent(row, handle.getFileName());
            } else {
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
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                event);
            log.debug("Published SUCCESS event for CSV activity: {}", dto.getActivityId());
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
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                event);
            log.debug("Published SKIPPED event for CSV activity: {}", data.getActivityId());
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
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                event);
            log.debug("Published FAILED event for CSV activity: {}", data.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish FAILED event for CSV activity: {}", data.getActivityId(), e);
        }
    }

    private void publishSummary(ImportResult result, long csvTotal, long dbMatchCount) {
        String reconciliation = buildReconciliationStatus(csvTotal, dbMatchCount);

        String summary = result.getSuccessCount() == 0
                ? String.format(
                        "CSV import complete. No new records added. Skipped: %d (already in DB), Failed: %d | Reconciliation: %s",
                        result.getSkippedCount(), result.getFailedCount(), reconciliation)
                : String.format(
                        "CSV import complete. Imported: %d, Skipped: %d, Failed: %d | Reconciliation: %s",
                        result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount(), reconciliation);

        log.info(summary);
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
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
}
