package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Slf4j
public class GarminFitImportScheduledJob {

    private final GarminFitImportService garminFitImportService;
    private final RabbitTemplate rabbitTemplate;

    public GarminFitImportScheduledJob(GarminFitImportService garminFitImportService,
                                       RabbitTemplate rabbitTemplate) {
        this.garminFitImportService = garminFitImportService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Scheduled job that runs every 5 minutes to process Garmin FIT files.
     * Uses ShedLock to ensure only one instance runs at a time in distributed environments.
     * 
     * Lock configuration:
     * - lockAtMostFor: 4 minutes (job should complete within this time)
     * - lockAtLeastFor: 30 seconds (minimum lock duration to prevent rapid re-execution)
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @SchedulerLock(
            name = "garminFitImportJob",
            lockAtMostFor = "4m",
            lockAtLeastFor = "30s"
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
            String summaryPayload = String.format(
                "Total files processed: %d, Successfully imported: %d, Skipped: %d, Failed: %d",
                result.getTotalProcessed(), result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());
            log.info(summaryPayload);

            try {
                log.info("=== Attempting to send message to RabbitMQ ===");
                log.info("Exchange: x.sathishprojects.events");
                log.info("Routing Key: garmin.operations.crud");
                log.info("Payload: {}", summaryPayload);
                
                rabbitTemplate.convertAndSend(
                    "x.sathishprojects.events",
                    "garmin.operations.crud",
                    summaryPayload);
                
                log.info("=== Message sent successfully to RabbitMQ ===");
            } catch (Exception e) {
                log.error("=== FAILED to send message to RabbitMQ ===", e);
            }


            if (result.getFailedCount() > 0) {
                log.error("Failed files details:");
                result.getFailedFiles().forEach((file, error) -> 
                    log.error("  - {}: {}", file, error));
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
    
    /**
     * RabbitMQ listener for GARMIN_RUN domain events.
     * Processes incoming run events from the EventTracker service.
     * User: sathish (valid EventTracker domain user)
     */
//    @RabbitListener(queues = "x.garmin.operations")
//    @Transactional
//    public void processGarminRunEvent(String eventPayload) {
//        log.info("=== Received GARMIN_RUN event from RabbitMQ ===");
//        log.info("Event payload: {}", eventPayload);
//
//        try {
//            // Process the received event
//            log.info("Successfully processed GARMIN_RUN event from runs-app");
//        } catch (Exception e) {
//            log.error("Error processing GARMIN_RUN event from RabbitMQ", e);
//            throw e; // Re-throw to trigger message requeue if needed
//        }
//    }
}
