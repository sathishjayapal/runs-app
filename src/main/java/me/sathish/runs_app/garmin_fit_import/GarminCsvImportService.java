package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
import me.sathish.runs_app.garmin_run.GarminRunDTO;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.garmin_run.GarminRunService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class GarminCsvImportService {

    private final GarminCsvParser csvParser;
    private final GarminRunService garminRunService;
    private final GarminRunRepository garminRunRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.garmin.import.csv-folder}")
    private String defaultCsvFolder;

    @Value("${app.garmin.import.systemUserId}")
    private Long systemUserId;

    public GarminCsvImportService(GarminCsvParser csvParser,
                                   GarminRunService garminRunService,
                                   GarminRunRepository garminRunRepository,
                                   RabbitTemplate rabbitTemplate) {
        this.csvParser = csvParser;
        this.garminRunService = garminRunService;
        this.garminRunRepository = garminRunRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public ImportResult processImportFolder(String folderOverride) {
        String folder = (folderOverride != null && !folderOverride.isBlank()) ? folderOverride : defaultCsvFolder;
        log.info("Starting Garmin CSV import from folder: {}", folder);

        ImportResult result = new ImportResult();

        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("CSV import folder does not exist or is not a directory: {}", folder);
            publishSummary(result, 0, 0);
            return result;
        }

        File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            log.info("No CSV files found in folder: {}", folder);
            publishSummary(result, 0, 0);
            return result;
        }

        for (File csvFile : csvFiles) {
            processFile(csvFile, result);
        }

        return result;
    }

    private void processFile(File csvFile, ImportResult result) {
        List<FitActivityData> rows;
        try {
            rows = csvParser.parse(csvFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to parse CSV file: {}", csvFile.getName(), e);
            result.addFailed(csvFile.getName(), e.getMessage());
            publishSummary(result, 0, 0);
            return;
        }

        if (rows.isEmpty()) {
            log.info("No parseable rows found in: {}", csvFile.getName());
            publishSummary(result, 0, 0);
            return;
        }

        List<String> csvActivityIds = rows.stream().map(FitActivityData::getActivityId).toList();

        for (FitActivityData row : rows) {
            if (garminRunRepository.existsByActivityId(row.getActivityId())) {
                log.debug("Activity already exists in DB, skipping: {} {}", row.getActivityDate(), row.getActivityName());
                result.addSkipped(row.getActivityDate() + " " + row.getActivityName());
            } else {
                try {
                    GarminRunDTO dto = mapToDto(row, csvFile.getName());
                    Long savedId = garminRunService.create(dto);
                    publishActivityEvent(dto, savedId);
                    result.addSuccess(row.getActivityDate() + " " + row.getActivityName());
                    log.info("Imported CSV activity: {} (DB id: {})", row.getActivityId(), savedId);
                } catch (Exception e) {
                    log.error("Failed to save CSV activity: {}", row.getActivityId(), e);
                    result.addFailed(row.getActivityId(), e.getMessage());
                }
            }
        }

        long dbMatchCount = garminRunRepository.countByActivityIdIn(csvActivityIds);
        publishSummary(result, csvActivityIds.size(), dbMatchCount);
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
        dto.setCreatedBy(systemUserId);
        return dto;
    }

    private void publishActivityEvent(GarminRunDTO dto, Long savedId) {
        try {
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_CSV_RUN");
            event.setActivityId(dto.getActivityId());
            event.setActivityName(dto.getActivityName());
            event.setActivityDate(LocalDateTime.now());
            event.setDistance(dto.getDistance());
            event.setElapsedTime(dto.getElapsedTime());
            event.setDatabaseId(savedId);
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                event);
            log.debug("Published event for CSV activity: {}", dto.getActivityId());
        } catch (Exception e) {
            log.error("Failed to publish event for CSV activity: {}", dto.getActivityId(), e);
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
}
