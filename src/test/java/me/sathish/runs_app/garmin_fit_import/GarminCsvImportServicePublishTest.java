package me.sathish.runs_app.garmin_fit_import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Regression test for two incidents in the same code path, both confirmed against the live
 * broker/eventstracker on 2026-08-16/17:
 *
 * <p>1. Publishing the raw GarminRunEvent object via convertAndSend let JacksonJsonMessageConverter
 * stamp a __TypeId__ header with runs-app's FQN, which eventstracker (different JAR) couldn't
 * resolve — message rejected with ClassNotFoundException, no error visible here.
 *
 * <p>2. The first fix (serializing to a JSON String, still via convertAndSend) did not actually
 * solve it: the RabbitTemplate's configured MessageConverter is JacksonJsonMessageConverter, which
 * re-serializes ANY object passed to convertAndSend, including a String — double-encoding the JSON.
 * eventstracker's Garmin listener takes the raw AMQP Message and manually decodes the body as
 * UTF-8 text (bypassing Spring's inbound conversion), so it received the literal quoted/escaped
 * wrapper text and failed with MismatchedInputException trying to parse it as a JSON object.
 *
 * <p>The only fix that's correct for any consumer shape is to bypass the converter entirely:
 * build the Message by hand and call rabbitTemplate.send (not convertAndSend), so the wire bytes
 * are the exact, single-encoded JSON with no extra wrapping and no __TypeId__ header. This test
 * asserts on the Message actually handed to send().
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
    void publishGarminEvent_sendsRawSingleEncodedJsonWithNoTypeHeader() throws Exception {
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

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate)
                .send(
                        eq(RabbitMQConfiguration.GARMIN_EXCHANGE),
                        eq(RabbitMQConfiguration.GARMIN_API_ROUTING_KEY),
                        messageCaptor.capture());

        Message message = messageCaptor.getValue();
        String wireBody = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(
                wireBody.startsWith("{") && wireBody.endsWith("}"),
                "Wire body must be clean, single-encoded JSON object text (starts with '{', ends "
                        + "with '}') — not double-encoded (would start with an escaped quote) and not "
                        + "some other wrapper. Actual: " + wireBody);
        assertTrue(wireBody.contains("TEST123"));
        assertEquals("corr-1", message.getMessageProperties().getCorrelationId());
        assertNull(
                message.getMessageProperties().getHeaders().get("__TypeId__"),
                "send() must bypass the MessageConverter entirely — no __TypeId__ header should ever "
                        + "be stamped, since that's what a different-JAR consumer can't resolve.");
    }

    /**
     * The OPS queue (runs-ai-analyzer's @Payload String listener) deliberately keeps the original
     * convertAndSend(..., objectMapper.writeValueAsString(event), ...) pattern — unverified live,
     * but never reported broken and unchanged by this incident's fix. This test locks in that the
     * two routing keys are NOT unified onto the same send() path without that verification.
     */
    @Test
    void publishGarminEvent_opsQueue_stillUsesConvertAndSendWithStringPayload() throws Exception {
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
        event.setActivityId("TEST456");
        event.setStatus("SUCCESS");

        service.publishGarminEvent(RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY, event, "corr-2");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfiguration.GARMIN_EXCHANGE),
                        eq(RabbitMQConfiguration.GARMIN_OPS_ROUTING_KEY),
                        payloadCaptor.capture(),
                        any(MessagePostProcessor.class));

        Object payload = payloadCaptor.getValue();
        assertInstanceOf(String.class, payload);
        assertTrue(((String) payload).contains("TEST456"));
    }
}
