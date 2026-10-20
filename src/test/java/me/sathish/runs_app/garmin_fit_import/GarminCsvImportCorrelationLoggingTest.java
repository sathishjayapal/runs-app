package me.sathish.runs_app.garmin_fit_import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import me.sathish.runs_app.file_import_record.FileImportRecordService;
import me.sathish.runs_app.file_import_record.ReconciliationService;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.garmin_run.GarminRunService;
import me.sathish.runs_app.mail.MailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * The per-file correlation ID must be in the logging MDC while a file is imported, so JSON logs carry
 * it as a top-level field that Loki can filter on (same key eventstracker uses), and it must not leak
 * to log lines written outside the import.
 */
@ExtendWith(MockitoExtension.class)
class GarminCsvImportCorrelationLoggingTest {

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

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        serviceLogger = (Logger) LoggerFactory.getLogger(GarminCsvImportService.class);
        // ListAppender reads the MDC lazily; capture it at log time or it is already cleared when asserted.
        appender = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        serviceLogger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void importPutsTheCorrelationIdInTheMdcAndRemovesItAfterwards() {
        when(properties.getSource()).thenReturn(GarminCsvImportProperties.Source.LOCAL);

        // A file whose stream cannot be opened: the import logs, publishes a summary and returns,
        // which is enough to exercise the MDC scope without any CSV parsing.
        CsvFileHandle brokenFile = new CsvFileHandle() {
            @Override
            public String getFileName() {
                return "Activities.csv";
            }

            @Override
            public InputStream openStream() throws IOException {
                throw new IOException("boom");
            }
        };
        CsvFileProvider provider = new CsvFileProvider() {
            @Override
            public GarminCsvImportProperties.Source getSourceType() {
                return GarminCsvImportProperties.Source.LOCAL;
            }

            @Override
            public List<CsvFileHandle> listCsvFiles(String folderOverride) {
                return List.of(brokenFile);
            }
        };
        GarminCsvImportService service = new GarminCsvImportService(
                csvParser,
                garminRunService,
                garminRunRepository,
                rabbitTemplate,
                List.of(provider),
                properties,
                fileImportRecordService,
                reconciliationService,
                mailService,
                new ObjectMapper());

        service.processImportFolder(null);

        String key = GarminCsvImportService.CORRELATION_ID_MDC_KEY;

        ILoggingEvent starting = find("Starting import for file=");
        String correlationId = starting.getMDCPropertyMap().get(key);
        assertNotNull(correlationId, "the MDC must hold the correlation ID while the file is imported");
        assertTrue(
                starting.getFormattedMessage().contains("correlationId=" + correlationId),
                "the message keeps the ID too, so existing searches still work");

        assertEquals(
                correlationId,
                find("Failed to parse CSV file").getMDCPropertyMap().get(key),
                "later lines of the same import carry the same ID");

        assertNull(
                find("Starting Garmin CSV import using source").getMDCPropertyMap().get(key),
                "lines logged before the per-file scope must not carry an ID");
        assertNull(MDC.get(key), "the MDC must be cleaned up after the import");
    }

    private ILoggingEvent find(String messagePrefix) {
        return appender.list.stream()
                .filter(e -> e.getFormattedMessage().startsWith(messagePrefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no log line starting with: " + messagePrefix));
    }
}
