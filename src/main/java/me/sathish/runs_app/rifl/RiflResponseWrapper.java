package me.sathish.runs_app.rifl;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Thin alias around Spring's {@link ContentCachingResponseWrapper} so the filter
 * can reach the response body + status after the controller has returned.
 */
class RiflResponseWrapper extends ContentCachingResponseWrapper {
    RiflResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    String capturedBody() {
        byte[] bytes = getContentAsByteArray();
        return bytes.length == 0 ? "" : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
