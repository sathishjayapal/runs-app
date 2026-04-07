package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
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
    private final UnifiedGarminImportService unifiedGarminImportService;
    private final RabbitTemplate rabbitTemplate;

    public GarminFitImportScheduledJob(GarminFitImportService garminFitImportService,
                                       UnifiedGarminImportService unifiedGarminImportService,
                                       RabbitTemplate rabbitTemplate) {
        this.garminFitImportService = garminFitImportService;
        this.unifiedGarminImportService = unifiedGarminImportService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Scheduled job that runs every 5 minutes to process Garmin FIT and CSV files in parallel.
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
        log.info("=== Starting Unified Garmin Import Scheduled Job (FIT + CSV) ===");
        
        try {
            UnifiedImportResult result = unifiedGarminImportService.processAllFiles();
            
            log.info("=== Unified Garmin Import Job Completed ===");
            log.info("Total imported: {}", result.getTotalSuccessCount());
            log.info("Total skipped: {}", result.getTotalSkippedCount());
            log.info("Total failed: {}", result.getTotalFailedCount());
            log.info("Duration: {}ms", result.getTotalDurationMs());
            
            // Create structured event for unified import summary
            UnifiedImportEvent summaryEvent = new UnifiedImportEvent();
            summaryEvent.setEventType("GARMIN_UNIFIED_IMPORT");
            summaryEvent.setTotalImported(result.getTotalSuccessCount());
            summaryEvent.setTotalSkipped(result.getTotalSkippedCount());
            summaryEvent.setTotalFailed(result.getTotalFailedCount());
            summaryEvent.setDurationMs(result.getTotalDurationMs());
            summaryEvent.setFitSuccess(result.getFitResult() != null ? result.getFitResult().getSuccessCount() : 0);
            summaryEvent.setFitSkipped(result.getFitResult() != null ? result.getFitResult().getSkippedCount() : 0);
            summaryEvent.setFitFailed(result.getFitResult() != null ? result.getFitResult().getFailedCount() : 0);
            summaryEvent.setCsvSuccess(result.getCsvResult() != null ? result.getCsvResult().getSuccessCount() : 0);
            summaryEvent.setCsvSkipped(result.getCsvResult() != null ? result.getCsvResult().getSkippedCount() : 0);
            summaryEvent.setCsvFailed(result.getCsvResult() != null ? result.getCsvResult().getFailedCount() : 0);
            summaryEvent.setStatus(result.isFullySuccessful() ? "SUCCESS" : result.hasFailed() ? "PARTIAL_SUCCESS" : "NO_NEW_DATA");
            
            String summaryPayload = String.format(
                "Unified Import: Total imported: %d, Skipped: %d, Failed: %d, Duration: %dms | " +
                "FIT: Success=%d, Skipped=%d, Failed=%d | CSV: Success=%d, Skipped=%d, Failed=%d",
                result.getTotalSuccessCount(), result.getTotalSkippedCount(), result.getTotalFailedCount(),
                result.getTotalDurationMs(),
                summaryEvent.getFitSuccess(), summaryEvent.getFitSkipped(), summaryEvent.getFitFailed(),
                summaryEvent.getCsvSuccess(), summaryEvent.getCsvSkipped(), summaryEvent.getCsvFailed());
            
            log.info(summaryPayload);

            try {
                log.info("=== Attempting to send unified import summary to RabbitMQ ===");
                log.info("Exchange: {}, RoutingKey: {}", 
                    RabbitMQConfiguration.GARMIN_EXCHANGE,
                    RabbitMQConfiguration.GARMIN_ROUTING_KEY);
                
                rabbitTemplate.convertAndSend(
                    RabbitMQConfiguration.GARMIN_EXCHANGE,
                    RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                    summaryEvent);
                
                log.info("=== Unified import summary sent successfully to RabbitMQ ===");
            } catch (Exception e) {
                log.error("=== FAILED to send unified import summary to RabbitMQ ===", e);
            }
            
        } catch (Exception e) {
            log.error("Error during Unified Garmin import job execution", e);
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
