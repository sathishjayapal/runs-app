package me.sathish.runs_app.rifl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * A save rejected by the RIFL filter used to leave no log line at all (sendError bypasses the
 * exception-handling library), which made a 400 in the browser impossible to explain from Loki.
 */
class RiflFilterRejectionLoggingTest {

    private final LeaseManager leaseManager = new LeaseManager();
    private final ResultTracker resultTracker = mock(ResultTracker.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final RiflFilter filter = new RiflFilter(leaseManager, resultTracker);

    private final Logger logger = (Logger) LoggerFactory.getLogger(RiflFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void missingHeadersAreRejectedWith400AndLoggedAsWarn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/garminRuns/10827");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        verifyNoInteractions(chain);
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("PUT /api/garminRuns/10827"));
        assertTrue(event.getFormattedMessage().contains("X-Client-Id=null"));
    }

    @Test
    void unknownLeaseIsLoggedAsWarnAndThrowsStaleRpc() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/garminRuns/10827");
        request.addHeader("X-Client-Id", "999");
        request.addHeader("X-Sequence-Number", "1");
        request.addHeader("X-First-Incomplete", "1");

        assertThrows(StaleRpcException.class, () -> filter.doFilter(request, new MockHttpServletResponse(), chain));

        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("clientId=999"));
    }

    @Test
    void readRequestsAreNotFilteredAndNotLogged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/garminRuns/10827");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertTrue(appender.list.isEmpty());
    }
}
