package me.sathish.runs_app.garmin_fit_import;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import me.sathish.runs_app.config.RabbitMQConfiguration;
import me.sathish.runs_app.file_import_record.FileImportRecordService;
import me.sathish.runs_app.file_import_record.ReconciliationService;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.garmin_run.GarminRunService;
import me.sathish.runs_app.mail.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Regression test for an incident where Garmin events published to eventstracker were silently
 * dropped: rabbitTemplate.convertAndSend was called with the raw GarminRunEvent object instead of
 * a JSON string. JacksonJsonMessageConverter stamps a __TypeId__ header with the object's runs-app
 * FQN, which cross-service consumers (eventstracker, running a different JAR) can't resolve on
 * their classpath — the message was rejected with no error visible in runs-app, and publish looked
 * like a success. Fixed by always routing through GarminCsvImportService.publishGarminEvent, which
 * serializes to a JSON string. This test fails if that helper ever regresses to a raw-object send.
 */
@ExtendWith(MockitoExtension.class)
class GarminCsvImportServicePublishTest {

    @Mock
    private GarminCsvParser csvParser;

    @Mock
    private GarminRunService garminRunService;

    @Mock
    private GarminRunRepository garminRunRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private GarminCsvImportProperties properties;

    @Mock
    private FileImportRecordService fileImportRecordService;

    @Mock
    private ReconciliationService reconciliationService;

    @Mock
    private MailService mailService;

    @Test
    void publishGarminEvent_sendsJsonStringPayload_notRawObject() throws Exception {
        GarminCsvImportService service = new GarminCsvImportService(
                csvParser,
                garminRunService,
                garminRunRepository,
                rabbitTemplate,
                Collections.emptyList(),
                properties,
                fileImportRecordService,
                reconciliationService,
                mailService,
                new ObjectMapper());

        GarminRunEvent event = new GarminRunEvent();
        event.setEventType("GARMIN_CSV_RUN");
        event.setActivityId("TEST123");
        event.setStatus("SUCCESS");

        service.publishGarminEvent(RabbitMQConfiguration.GARMIN_API_ROUTING_KEY, event, "corr-1");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfiguration.GARMIN_EXCHANGE),
                        eq(RabbitMQConfiguration.GARMIN_API_ROUTING_KEY),
                        payloadCaptor.capture(),
                        any(MessagePostProcessor.class));

        Object payload = payloadCaptor.getValue();
        assertInstanceOf(
                String.class,
                payload,
                "Garmin events must be published as JSON strings, not raw objects — a raw object "
                        + "makes the message converter stamp a __TypeId__ header that cross-service "
                        + "consumers can't resolve, and the message gets silently dropped on their side.");
        assertTrue(((String) payload).contains("TEST123"));
    }
}
