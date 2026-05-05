package me.sathish.runs_app.scheduling;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.garmin_fit_import.GarminCsvImportService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GarminCsvImportScheduler {

    private final GarminCsvImportService garminCsvImportService;

    @Value("${garmin.csv-import.enabled:true}")
    private boolean csvImportEnabled;

    public GarminCsvImportScheduler(GarminCsvImportService garminCsvImportService) {
        this.garminCsvImportService = garminCsvImportService;
    }

    /**
     *
     */
    // Temporarily disabled while troubleshooting import issues.
    // @Scheduled(cron = "${garmin.csv-import.schedule:0 */3 * * * *}")
    @SchedulerLock(
        name = "garminCsvImport",
        lockAtMostFor = "5h",      // Max 5 hours (prevents long-running imports from blocking)
        lockAtLeastFor = "10m"     // Min 10 minutes between runs
    )
    public void importGarminCsvFiles() {
        if (!csvImportEnabled) {
            log.debug("Garmin CSV import is disabled");
            return;
        }

        try {
            log.info("Starting scheduled Garmin CSV import from Google Drive...");
            var result = garminCsvImportService.processImportFolder(null);
            log.info("Garmin CSV import completed. Success: {}, Skipped: {}, Failed: {}",
                result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());
        } catch (Exception e) {
            log.error("Scheduled Garmin CSV import failed", e);
        }
    }
}
