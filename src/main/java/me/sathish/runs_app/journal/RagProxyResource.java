package me.sathish.runs_app.journal;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.security.UserRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;


@RestController
@RequestMapping(value = "/api/rag", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@Slf4j
public class RagProxyResource {

    private final RestClient restClient;

    public RagProxyResource(
            @Value("${runs-ai-analyzer.base-url:http://localhost:8081}") final String analyzerBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(analyzerBaseUrl).build();
    }

    /**
     * POST /api/rag/search
     * Body: { "query": "..." }
     * Proxies to runs-ai-analyzer POST /api/v1/rag/search
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> search(@RequestBody final Object body) {
        return forward(HttpMethod.POST, "/api/v1/rag/search", body);
    }

    /**
     * GET /api/rag/recent
     * Returns the most recent AI analyses stored in vector store.
     * Proxies to runs-ai-analyzer GET /api/v1/rag/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<Object> recent() {
        return forward(HttpMethod.GET, "/api/v1/rag/recent", null);
    }

    /**
     * POST /api/rag/analyze
     * Body: { "runs": [...], "forceRefresh": false }
     * Proxies to runs-ai-analyzer POST /api/v1/analysis/analyze/async
     * Returns immediately with { jobId, status, message }
     */
    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> submitAnalysis(@RequestBody final Object body) {
        return forward(HttpMethod.POST, "/api/v1/analysis/analyze/async", body);
    }

    /**
     * GET /api/rag/analyze/status/{jobId}
     * Proxies to runs-ai-analyzer GET /api/v1/analysis/analyze/status/{jobId}
     */
    @GetMapping("/analyze/status/{jobId}")
    public ResponseEntity<Object> analysisStatus(@PathVariable final String jobId) {
        return forward(HttpMethod.GET, "/api/v1/analysis/analyze/status/" + jobId, null);
    }

    /**
     * GET /api/rag/analyze/result/{jobId}
     * Proxies to runs-ai-analyzer GET /api/v1/analysis/analyze/result/{jobId}
     */
    @GetMapping("/analyze/result/{jobId}")
    public ResponseEntity<Object> analysisResult(@PathVariable final String jobId) {
        return forward(HttpMethod.GET, "/api/v1/analysis/analyze/result/" + jobId, null);
    }

    private ResponseEntity<Object> forward(final HttpMethod method, final String path, final Object body) {
        try {
            final RestClient.RequestBodySpec spec = restClient.method(method)
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON);
            if (body != null) {
                spec.body(body);
            }
            return spec.retrieve().toEntity(Object.class);
        } catch (final HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (final Exception e) {
            log.error("RAG proxy failed {} {}: {}", method, path, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"answer\":\"AI service is currently unavailable. Make sure runs-ai-analyzer is running on port 8081.\"}");
        }
    }

}
