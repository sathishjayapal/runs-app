package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class GarminFitImportScheduledJob {

    private final GarminFitImportService garminFitImportService;

    public GarminFitImportScheduledJob(GarminFitImportService garminFitImportService) {
        this.garminFitImportService = garminFitImportService;
    }

    /**
     * Scheduled job that runs every 30 minutes to process Garmin FIT files.
     * Uses ShedLock to ensure only one instance runs at a time in distributed environments.
     * 
     * Lock configuration:
     * - lockAtMostFor: 25 minutes (job should complete within this time)
     * - lockAtLeastFor: 1 minute (minimum lock duration to prevent rapid re-execution)
     */
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes at :00 and :30
    @SchedulerLock(
            name = "garminFitImportJob",
            lockAtMostFor = "25m",
            lockAtLeastFor = "1m"
    )
    public void processGarminFitFiles() {
        log.info("=== Starting Garmin FIT Import Scheduled Job ===");
        
        try {
            ImportResult result = garminFitImportService.processImportFolder();
            
            log.info("=== Garmin FIT Import Job Completed ===");
            log.info("Total files processed: {}", result.getTotalProcessed());
            log.info("Successfully imported: {}", result.getSuccessCount());
            log.info("Skipped (already processed): {}", result.getSkippedCount());
            log.info("Failed: {}", result.getFailedCount());
            
            if (result.getFailedCount() > 0) {
                log.warn("Failed files details:");
                result.getFailedFiles().forEach((file, error) -> 
                    log.warn("  - {}: {}", file, error));
            }
            
        } catch (Exception e) {
            log.error("Error during Garmin FIT import job execution", e);
        }
    }
    
    /**
     * Manual trigger method for testing or on-demand imports.
     * Can be called via REST endpoint or JMX.
     */
    public ImportResult triggerManualImport() {
        log.info("Manual Garmin FIT import triggered");
        return garminFitImportService.processImportFolder();
    }
}
