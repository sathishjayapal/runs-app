package me.sathish.runs_app.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Logback appender that forwards log events to the sathishlogger REST API.
 *
 * Copied from sathish-projects-logger's appender.SathishLoggerAppender rather than taken as a
 * Maven dependency: that project is built on Spring Boot 3.5.9 and bundles its own
 * bootstrap.yml/application.yml, which collide with runs-app's Spring Boot 4.0.1
 * auto-configuration when pulled in as a jar dependency. This class itself has no Spring
 * dependency, only logback-classic and jackson-databind (both already on this classpath).
 *
 * Configuration in logback-spring.xml:
 * <pre>
 *   &lt;appender name="SATHISHLOGGER" class="me.sathish.runs_app.logging.SathishLoggerAppender"&gt;
 *     &lt;serviceUrl&gt;http://localhost:8080&lt;/serviceUrl&gt;
 *     &lt;applicationName&gt;runs-app&lt;/applicationName&gt;
 *     &lt;minimumLevel&gt;WARN&lt;/minimumLevel&gt;
 *   &lt;/appender&gt;
 * </pre>
 */
public class SathishLoggerAppender extends AppenderBase<ILoggingEvent> {
    @Value("${SATHISHLOGGER_URL}")
    private String serviceUrl;

    private String applicationName = "unknown-app";
    private String minimumLevel = "WARN";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final BlockingQueue<ILoggingEvent> queue = new ArrayBlockingQueue<>(512);
    private ExecutorService worker;

    // --- Logback lifecycle ---

    @Override
    public void start() {
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sathishlogger-appender");
            t.setDaemon(true);
            return t;
        });
        worker.submit(this::drainQueue);
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (worker != null) {
            worker.shutdownNow();
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!levelEnabled(event)) {
            return;
        }
        // Non-blocking offer — drop if queue is full rather than stalling the caller
        if (!queue.offer(event)) {
            addWarn("SathishLoggerAppender queue full, dropping log event");
        }
    }

    // --- Background drain ---

    private void drainQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ILoggingEvent event = queue.take();
                sendToService(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendToService(ILoggingEvent event) {
        try {
            String body = buildPayload(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/api/logs/log"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                addWarn("SathishLoggerAppender received HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            addWarn("SathishLoggerAppender failed to send log: " + e.getMessage());
        }
    }

    private String buildPayload(ILoggingEvent event) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("applicationName", applicationName);
        node.put("logLevel", event.getLevel().toString());
        node.put("message", event.getFormattedMessage());
        node.put("loggerName", event.getLoggerName());
        node.put("threadName", event.getThreadName());

        String correlationId = event.getMDCPropertyMap().get("correlationId");
        if (correlationId != null) {
            node.put("correlationId", correlationId);
        }

        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            node.put("exceptionMessage", throwable.getMessage());
            node.put("stackTrace", formatStackTrace(throwable));
        }

        return objectMapper.writeValueAsString(node);
    }

    private String formatStackTrace(IThrowableProxy throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClassName()).append(": ").append(throwable.getMessage()).append("\n");
        for (var frame : throwable.getStackTraceElementProxyArray()) {
            sb.append("\tat ").append(frame.getSTEAsString()).append("\n");
        }
        return sb.toString();
    }

    private boolean levelEnabled(ILoggingEvent event) {
        int eventLevel = event.getLevel().toInt();
        int minLevel = ch.qos.logback.classic.Level.toLevel(minimumLevel).toInt();
        return eventLevel >= minLevel;
    }

    // --- Setters for Logback XML configuration ---

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setMinimumLevel(String minimumLevel) {
        this.minimumLevel = minimumLevel;
    }
}
