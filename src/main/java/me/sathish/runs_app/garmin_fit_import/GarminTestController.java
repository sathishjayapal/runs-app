package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.config.RabbitMQConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@Slf4j
public class GarminTestController {

    private final RabbitTemplate rabbitTemplate;
    private final GarminFitImportService garminFitImportService;

    public GarminTestController(RabbitTemplate rabbitTemplate, GarminFitImportService garminFitImportService) {
        this.rabbitTemplate = rabbitTemplate;
        this.garminFitImportService = garminFitImportService;
    }

    @GetMapping("/rabbitmq/send-test-message")
    public String sendTestMessage(@RequestParam(defaultValue = "Test message from REST endpoint") String message) {
        try {
            log.info("=== TEST: Sending message via REST endpoint ===");
            log.info("Exchange: {}", RabbitMQConfiguration.GARMIN_EXCHANGE);
            log.info("Routing Key: {}", RabbitMQConfiguration.GARMIN_ROUTING_KEY);
            log.info("Message: {}", message);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                message
            );
            
            log.info("=== TEST: Message sent successfully ===");
            return "Message sent successfully: " + message;
        } catch (Exception e) {
            log.error("=== TEST: Failed to send message ===", e);
            return "Failed to send message: " + e.getMessage();
        }
    }

    @GetMapping("/rabbitmq/send-garmin-event")
    public String sendGarminEvent() {
        try {
            log.info("=== TEST: Sending GarminRunEvent via REST endpoint ===");
            
            GarminRunEvent event = new GarminRunEvent();
            event.setEventType("GARMIN_RUN");
            event.setActivityId("TEST-ACTIVITY-" + System.currentTimeMillis());
            event.setActivityName("Test Run Activity");
            event.setActivityDate(LocalDateTime.now());
            event.setDistance("5.0");
            event.setElapsedTime("00:30:00");
            event.setDatabaseId(999L);
            
            log.info("Event: {}", event);
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.GARMIN_EXCHANGE,
                RabbitMQConfiguration.GARMIN_ROUTING_KEY,
                event
            );
            
            log.info("=== TEST: GarminRunEvent sent successfully ===");
            return "GarminRunEvent sent successfully: " + event.getActivityId();
        } catch (Exception e) {
            log.error("=== TEST: Failed to send GarminRunEvent ===", e);
            return "Failed to send GarminRunEvent: " + e.getMessage();
        }
    }

    @GetMapping("/garmin/trigger-import")
    public String triggerImport() {
        try {
            log.info("=== TEST: Manually triggering Garmin import ===");
            ImportResult result = garminFitImportService.processImportFolder();
            
            String summary = String.format(
                "Import completed - Total: %d, Success: %d, Skipped: %d, Failed: %d",
                result.getTotalProcessed(), result.getSuccessCount(), 
                result.getSkippedCount(), result.getFailedCount()
            );
            
            log.info(summary);
            return summary;
        } catch (Exception e) {
            log.error("=== TEST: Failed to trigger import ===", e);
            return "Failed to trigger import: " + e.getMessage();
        }
    }
}
