package me.sathish.runs_app.config;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.file_import_record.FileImportRecordRepository;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runs_app.runner_app_role.RunnerAppRoleRepository;
import me.sathish.runs_app.strava_run.StravaRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "server.port=0",
    "spring.mail.host=localhost",
    "spring.mail.port=2525",
    "spring.docker.compose.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5445/runsapp_db",
    "spring.datasource.username=postgres",
    "spring.datasource.password=P4ssword!"
})
@Sql({"/data/clearAll.sql", "/data/userData.sql"})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Slf4j
public abstract class BaseIT {

    public static final String AUTH_USER_ADMIN = "admin@test.com";
    public static final String AUTH_USER_REGULAR = "user@test.com";
    public static final String PASSWORD = "password";
    private static String sessionToken = null;
    @Autowired
    public GarminRunRepository garminRunRepository;
    @Autowired
    public StravaRunRepository stravaRunRepository;
    @Autowired
    public RunAppUserRepository runAppUserRepository;
    @Autowired
    public RunnerAppRoleRepository runnerAppRoleRepository;
    @Autowired
    public FileImportRecordRepository fileNameTrackerRepository;
    @Autowired
    private Environment environment;
    @Autowired
    private ApplicationContext applicationContext;
    @LocalServerPort
    private int port;
    private volatile int actualPort = 0;

    @BeforeEach
    public void setupPort() throws InterruptedException {
        // Clear session cache for test isolation
        sessionToken = null;


        Thread.sleep(100);

        log.debug("=== DEBUG: setupPort() called ===");
        log.debug("@LocalServerPort field: {}", port);
        log.debug("local.server.port: {}", environment.getProperty("local.server.port"));
        log.debug("server.port: {}", environment.getProperty("server.port"));

        // Try @LocalServerPort first
        if (port > 0) {
            actualPort = port;
            log.debug("Using @LocalServerPort: {}", actualPort);
            RestAssured.port = actualPort;
            return;
        }

        // Try environment properties
        String localServerPort = environment.getProperty("local.server.port");
        if (localServerPort != null && !localServerPort.equals("0")) {
            try {
                actualPort = Integer.parseInt(localServerPort);
                log.debug("Using local.server.port: {}", actualPort);
                RestAssured.port = actualPort;
                return;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse local.server.port: {}", localServerPort);
            }
        }

        // Last resort: scan for the actual port
        log.debug("Scanning for actual server port...");
        actualPort = findServerPort();
        if (actualPort > 0) {
            log.debug("Found server listening on port: {}", actualPort);
            RestAssured.port = actualPort;
            return;
        }

        throw new IllegalStateException("Cannot determine server port");
    }

    private int findServerPort() {
        // Scan common Spring Boot test port ranges
        for (int testPort = 8080; testPort <= 8090; testPort++) {
            if (isPortListening(testPort)) {
                return testPort;
            }
        }

        // Scan higher random port ranges
        for (int testPort = 49152; testPort <= 49200; testPort++) {
            if (isPortListening(testPort)) {
                return testPort;
            }
        }

        return 0;
    }

    private boolean isPortListening(int testPort) {
        try (Socket socket = new Socket("localhost", testPort)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    protected int getActualPort() {
        return actualPort;
    }

    @SneakyThrows
    public String readResource(final String resourceName) {
        return StreamUtils.copyToString(
                getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }

    /**
     * Get an authenticated session for the admin user.
     * Caches the session to avoid repeated logins.
     */
    public String getAdminSession() {
        if (sessionToken == null) {
            sessionToken = login(AUTH_USER_ADMIN, PASSWORD);
        }
        return sessionToken;
    }

    /**
     * Get an authenticated session for a regular user.
     */
    public String getUserSession() {
        return login(AUTH_USER_REGULAR, PASSWORD);
    }

    /**
     * Perform login and return session ID.
     * CSRF is disabled in the security config so we POST directly without a pre-flight GET.
     */
    private String login(String username, String password) {
        return RestAssured.given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", username)
                .formParam("password", password)
                .port(getActualPort())
                .redirects().follow(false)
                .when()
                .post("/login")
                .sessionId();
    }

    /**
     * Clear cached session (useful when testing auth changes).
     */
    protected void clearSession() {
        sessionToken = null;
    }
}
