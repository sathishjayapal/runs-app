package me.sathish.runs_app.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * The AI Insights page calls POST /api/rag/ask. The proxy must forward it to the analyzer's
 * /api/v1/rag/ask and hand back the coach's answer, and must turn "analyzer unreachable" into a 200 with an
 * error text (the page renders it in the answer card) instead of a 5xx that sends the user to the error screen.
 * Uses a real local HTTP server in place of runs-ai-analyzer.
 */
class RagProxyResourceAskTest {

    private HttpServer analyzer;

    @AfterEach
    void stopAnalyzer() {
        if (analyzer != null) {
            analyzer.stop(0);
        }
    }

    private String startAnalyzer(final int status, final String responseJson, final AtomicReference<String> seenPath,
            final AtomicReference<String> seenBody) throws IOException {
        analyzer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        analyzer.createContext("/", exchange -> {
            seenPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            seenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        analyzer.start();
        return "http://localhost:" + analyzer.getAddress().getPort();
    }

    @Test
    @SuppressWarnings("unchecked")
    void forwardsTheQuestionToTheAnalyzerAndReturnsTheCoachAnswer() throws Exception {
        final AtomicReference<String> path = new AtomicReference<>();
        final AtomicReference<String> body = new AtomicReference<>();
        final String baseUrl = startAnalyzer(200,
                "{\"query\":\"What next?\",\"answer\":\"Keep it easy.\",\"sources\":[\"Journal entry 2026-10-12\"]}",
                path, body);

        final ResponseEntity<Object> response = new RagProxyResource(baseUrl).ask(Map.of("query", "What next?"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("POST /api/v1/rag/ask", path.get());
        assertTrue(body.get().contains("What next?"), "the question must reach the analyzer: " + body.get());
        final Map<String, Object> answer = (Map<String, Object>) response.getBody();
        assertNotNull(answer);
        assertEquals("Keep it easy.", answer.get("answer"));
    }

    @Test
    void passesAnAnalyzerClientErrorStraightThrough() throws Exception {
        final String baseUrl = startAnalyzer(400, "{\"title\":\"Validation Failed\"}",
                new AtomicReference<>(), new AtomicReference<>());

        final ResponseEntity<Object> response = new RagProxyResource(baseUrl).ask(Map.of("query", ""));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenTheAnalyzerIsUnreachableItAnswersWithAnErrorTextNotA5xx() {
        final ResponseEntity<Object> response = new RagProxyResource("http://localhost:1").ask(Map.of("query", "x"));

        assertEquals(200, response.getStatusCode().value());
        final Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(String.valueOf(body.get("error")).contains("not reachable"));
    }
}
