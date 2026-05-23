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
@RequestMapping(value = "/api/journal", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@Slf4j
public class JournalProxyResource {

    private final RestClient restClient;

    public JournalProxyResource(
            @Value("${runs-ai-analyzer.base-url:http://localhost:8081}") final String analyzerBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(analyzerBaseUrl).build();
    }

    @GetMapping("/recent")
    public ResponseEntity<Object> getRecent() {
        return forward(HttpMethod.GET, "/api/v1/journal/recent", null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable final Long id) {
        return forward(HttpMethod.GET, "/api/v1/journal/" + id, null);
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Object> getByActivity(@PathVariable final String activityId) {
        return forward(HttpMethod.GET, "/api/v1/journal/activity/" + activityId, null);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> create(@RequestBody final Object body) {
        return forward(HttpMethod.POST, "/api/v1/journal", body);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable final Long id, @RequestBody final Object body) {
        return forward(HttpMethod.PUT, "/api/v1/journal/" + id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable final Long id) {
        return forward(HttpMethod.DELETE, "/api/v1/journal/" + id, null);
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
            log.error("Journal proxy failed {} {}: {}", method, path, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Journal service is currently unavailable");
        }
    }

}