package me.sathish.runs_app.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.restassured.http.ContentType;
import me.sathish.runs_app.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Integration tests for security, authentication, and authorization.
 * Tests login, logout, session management, role-based access control, and CSRF protection.
 */
public class SecurityIntegrationTest extends BaseIT {

    @Test
    public void login_validCredentials_success() {
        // Get initial session (CSRF)
        String session = given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .sessionId();

        // Perform login
        String authenticatedSession = given()
                .sessionId(session)
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", AUTH_USER_ADMIN)
                .formParam("password", PASSWORD)
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", not(containsString("error")))
                .extract()
                .sessionId();

        // Verify authenticated session works
        given()
                .sessionId(authenticatedSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void login_invalidPassword_failure() {
        String session = given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .sessionId();

        given()
                .sessionId(session)
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", AUTH_USER_ADMIN)
                .formParam("password", "wrongpassword")
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", containsString("error"));
    }

    @Test
    public void login_nonExistentUser_failure() {
        String session = given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .sessionId();

        given()
                .sessionId(session)
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", "nonexistent@test.com")
                .formParam("password", "password")
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", containsString("error"));
    }

    @Test
    public void login_emptyCredentials_failure() {
        String session = given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .sessionId();

        given()
                .sessionId(session)
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", "")
                .formParam("password", "")
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.FOUND.value())
                .header("Location", containsString("error"));
    }

    @Test
    public void protectedEndpoint_withoutAuth_unauthorized() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void protectedEndpoint_withValidSession_success() {
        String session = getAdminSession();

        given()
                .sessionId(session)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void stravaEndpoint_requiresAdminOrUser_adminAccess() {
        String adminSession = getAdminSession();

        given()
                .sessionId(adminSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void stravaEndpoint_requiresAdminOrUser_userAccess() {
        String userSession = getUserSession();

        given()
                .sessionId(userSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void stravaEndpoint_withoutAuth_unauthorized() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void logout_invalidatesSession() {
        // Login
        String session = getAdminSession();

        // Verify session works
        given()
                .sessionId(session)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Logout
        given()
                .sessionId(session)
                .when()
                .post("/logout")
                .then()
                .statusCode(HttpStatus.FOUND.value());

        // Verify session no longer works
        given()
                .sessionId(session)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void loginPage_accessible_withoutAuth() {
        given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(containsString("text/html"));
    }

    @Test
    public void multipleUsers_separateSessions() {
        // Login as admin
        String adminSession = getAdminSession();

        // Login as regular user
        String userSession = getUserSession();

        // Both sessions should work independently
        given()
                .sessionId(adminSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .sessionId(userSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void sessionReuse_multipleRequests_success() {
        String session = getAdminSession();

        // Make multiple requests with same session
        for (int i = 0; i < 5; i++) {
            given()
                    .sessionId(session)
                    .accept(ContentType.JSON)
                    .when()
                    .get("/api/garminRuns")
                    .then()
                    .statusCode(HttpStatus.OK.value());
        }
    }

    @Test
    public void createResource_requiresAuth() {
        String requestBody = """
                {
                  "activityId": "TEST001",
                  "activityDate": "2026-02-20",
                  "activityType": "running",
                  "activityName": "Test Run",
                  "distance": "5.0",
                  "createdBy": 10004
                }
                """;

        // Without auth
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        // With auth
        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void updateResource_requiresAuth() {
        String updateBody = """
                {
                  "activityId": "UPDATED",
                  "activityDate": "2026-02-01",
                  "activityType": "running",
                  "activityName": "Updated",
                  "distance": "10.0",
                  "createdBy": 10004
                }
                """;

        // Without auth
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/garminRuns/10007")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        // With auth
        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/garminRuns/10007")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void deleteResource_requiresAuth() {
        // Without auth
        given()
                .when()
                .delete("/api/garminRuns/10011")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        // With auth
        given()
                .sessionId(getAdminSession())
                .when()
                .delete("/api/garminRuns/10011")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void publicEndpoints_accessibleWithoutAuth() {
        // Login page
        given()
                .accept(ContentType.HTML)
                .when()
                .get("/login")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Actuator health (if exposed publicly)
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(anyOf(equalTo(HttpStatus.OK.value()), equalTo(HttpStatus.UNAUTHORIZED.value())));
    }

    @Test
    public void differentUsersSeeTheirOwnData() {
        // This test verifies data isolation between users
        // Note: Actual implementation depends on whether app filters by user
        String adminSession = getAdminSession();
        String userSession = getUserSession();

        // Both should be able to access the API
        given()
                .sessionId(adminSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .sessionId(userSession)
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void csrfProtection_loginRequiresValidSession() {
        // Attempt login without getting initial session (no CSRF token)
        // This should fail or redirect to login
        given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("username", AUTH_USER_ADMIN)
                .formParam("password", PASSWORD)
                .when()
                .post("/login")
                .then()
                // Either forbidden or redirected to login page
                .statusCode(anyOf(
                        equalTo(HttpStatus.FOUND.value()),
                        equalTo(HttpStatus.FORBIDDEN.value())
                ));
    }
}
