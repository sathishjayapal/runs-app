package me.sathish.runs_app.rifl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The RIFL request boundary. On entry: parse headers, check lease, advance the
 * client's watermark, and look up the {@link ResultTracker}. On a hit, replay the
 * cached response and short-circuit the chain. On a miss, wrap the response so
 * we can capture the controller's body, then cache it for future retries.
 */
@Component
public class RiflFilter extends OncePerRequestFilter {

    static final String HDR_CLIENT_ID = "X-Client-Id";
    static final String HDR_SEQ = "X-Sequence-Number";
    static final String HDR_FIRST_INCOMPLETE = "X-First-Incomplete";
    static final String HDR_REPLAY = "X-Rifl-Replay";

    private static final String PATH_PREFIX = "/api/garminRuns";

    private final LeaseManager leaseManager;
    private final ResultTracker resultTracker;

    public RiflFilter(LeaseManager leaseManager, ResultTracker resultTracker) {
        this.leaseManager = leaseManager;
        this.resultTracker = resultTracker;
    }

    private static void replay(HttpServletResponse response, CompletionRecord record) throws IOException {
        response.setStatus(record.httpStatus());
        response.setHeader(HDR_REPLAY, "true");
        response.setContentType("application/json");
        byte[] bytes = record.bodyJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private static Long parseLong(HttpServletRequest request, String header) {
        String raw = request.getHeader(header);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(PATH_PREFIX)) {
            return true;
        }
        String method = request.getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long clientId = parseLong(request, HDR_CLIENT_ID);
        Long seq = parseLong(request, HDR_SEQ);
        Long firstIncomplete = parseLong(request, HDR_FIRST_INCOMPLETE);
        if (clientId == null || seq == null || firstIncomplete == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required RIFL headers: " + HDR_CLIENT_ID + ", " + HDR_SEQ + ", " + HDR_FIRST_INCOMPLETE);
            return;
        }
        if (!leaseManager.isAlive(clientId)) {
            throw new StaleRpcException(clientId);
        }
        resultTracker.advanceFirstIncomplete(clientId, firstIncomplete);

        RpcId id = new RpcId(clientId, seq);
        var cached = resultTracker.lookup(id);
        if (cached.isPresent()) {
            replay(response, cached.get());
            return;
        }

        RiflResponseWrapper wrapper = new RiflResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
        } finally {
            int status = wrapper.getStatus();
            String body = wrapper.capturedBody();
            if (status >= 200 && status < 300) {
                resultTracker.record(id, status, body);
            }
            wrapper.copyBodyToResponse();
        }
    }
}
