package me.sathish.runs_app.scheduling;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.garmin_fit_import.GarminCsvImportService;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GarminCsvImportScheduler implements CommandLineRunner {

    private final GarminCsvImportService garminCsvImportService;
    private final RunAppUserRepository runAppUserRepository;
    @Value("${app.garmin.csv-import-enabled}")
    private boolean csvImportEnabled;

    public GarminCsvImportScheduler(GarminCsvImportService garminCsvImportService, RunAppUserRepository runAppUserRepository) {
        this.garminCsvImportService = garminCsvImportService;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Value("${app.garmin.csv-import-schedule}")
    private String csvImportSchedule;

    @Override
    public void run(String... args) {
        log.warn("GARMIN_SCHEDULE_CHECK enabled={} schedule='{}'", csvImportEnabled, csvImportSchedule);
    }

    @Scheduled(fixedDelay = 50000)
    public void heartbeat() {
        log.warn("SCHEDULER_HEARTBEAT tick");
    }

    @Scheduled(cron = "${app.garmin.csv-import-schedule}")
    @SchedulerLock(
        name = "garminCsvImport",
        lockAtMostFor = "20m",     // Crash-safety ceiling; not a re-trigger gate
        lockAtLeastFor = "2m"      // Must stay below the cron interval or ShedLock silently skips ticks
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
