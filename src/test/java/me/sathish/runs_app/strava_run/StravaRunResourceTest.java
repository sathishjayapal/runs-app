package me.sathish.runs_app.strava_run;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.restassured.http.ContentType;
import me.sathish.runs_app.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for StravaRun CRUD operations.
 * Tests REST API endpoints with authentication and authorization.
 */
@Sql("/data/stravaRunData.sql")
public class StravaRunResourceTest extends BaseIT {

    @Test
    public void getAllStravaRuns_withAuth_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("content", hasSize(3))
                .body("content[0].runNumber", equalTo(20001))
                .body("content[0].runName", equalTo("Morning Trail Run"))
                .body("page.totalElements", equalTo(3))
                .body("page.size", equalTo(20));
    }

    @Test
    public void getAllStravaRuns_withUserAuth_success() {
        given()
                .sessionId(getUserSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(3));
    }

    @Test
    public void getAllStravaRuns_withoutAuth_unauthorized() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void getAllStravaRuns_withPagination() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(2))
                .body("page.totalElements", equalTo(3))
                .body("page.size", equalTo(2))
                .body("page.number", equalTo(0));
    }

    @Test
    public void getAllStravaRuns_withFilter() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .queryParam("filter", "20001")
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(1))
                .body("content[0].runNumber", equalTo(20001))
                .body("content[0].runName", equalTo("Morning Trail Run"));
    }

    @Test
    public void getStravaRun_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("runNumber", equalTo(20001))
                .body("customerId", equalTo(100))
                .body("runName", equalTo("Morning Trail Run"))
                .body("runDate", equalTo("2026-02-03"))
                .body("miles", equalTo(8))
                .body("startLocation", equalTo(1))
                .body("createdBy", equalTo(10004));
    }

    @Test
    public void getStravaRun_withoutAuth_unauthorized() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void getStravaRun_notFound() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void createStravaRun_success() {
        String requestBody = readResource("/requests/stravaRunRequest.json");

        Long createdId = given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        assertNotNull(createdId);
        assertTrue(createdId > 0);

        // Verify the created run
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/" + createdId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("customerId", equalTo(200))
                .body("runName", equalTo("Test Strava Run"))
                .body("runDate", equalTo("2026-02-20"))
                .body("miles", equalTo(10))
                .body("startLocation", equalTo(5));
    }

    @Test
    public void createStravaRun_withoutAuth_unauthorized() {
        String requestBody = readResource("/requests/stravaRunRequest.json");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void createStravaRun_missingRequiredFields() {
        String requestBody = """
                {
                  "customerId": 100,
                  "runName": "Incomplete Run"
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createStravaRun_runNameTooLong() {
        String longName = "A".repeat(101);
        String requestBody = String.format("""
                {
                  "customerId": 100,
                  "runName": "%s",
                  "runDate": "2026-02-20",
                  "miles": 5,
                  "startLocation": 1,
                  "createdBy": 10004
                }
                """, longName);

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createStravaRun_invalidDateFormat() {
        String requestBody = """
                {
                  "customerId": 100,
                  "runName": "Test Run",
                  "runDate": "invalid-date",
                  "miles": 5,
                  "startLocation": 1,
                  "createdBy": 10004
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void updateStravaRun_success() {
        String updateBody = """
                {
                  "customerId": 100,
                  "runName": "Morning Trail Run - Updated",
                  "runDate": "2026-02-03",
                  "miles": 10,
                  "startLocation": 2,
                  "createdBy": 10004,
                  "updatedBy": 10004
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(equalTo("20001"));

        // Verify the update
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("runName", equalTo("Morning Trail Run - Updated"))
                .body("miles", equalTo(10))
                .body("startLocation", equalTo(2))
                .body("updatedBy", equalTo(10004));
    }

    @Test
    public void updateStravaRun_withoutAuth_unauthorized() {
        String updateBody = """
                {
                  "customerId": 100,
                  "runName": "Updated",
                  "runDate": "2026-02-03",
                  "miles": 10,
                  "startLocation": 1,
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void updateStravaRun_notFound() {
        String updateBody = """
                {
                  "customerId": 100,
                  "runName": "Test",
                  "runDate": "2026-02-20",
                  "miles": 5,
                  "startLocation": 1,
                  "createdBy": 10004
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/stravaRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void deleteStravaRun_success() {
        // First verify the run exists
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/20003")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Delete the run
        given()
                .sessionId(getAdminSession())
                .when()
                .delete("/api/stravaRuns/20003")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify it's gone
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/20003")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());

        // Verify count decreased
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("page.totalElements", equalTo(2));
    }

    @Test
    public void deleteStravaRun_withoutAuth_unauthorized() {
        given()
                .when()
                .delete("/api/stravaRuns/20001")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void deleteStravaRun_notFound() {
        given()
                .sessionId(getAdminSession())
                .when()
                .delete("/api/stravaRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void getCreatedByValues_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/createdByValues")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("$", hasKey("10004"))
                .body("$", hasKey("10005"));
    }

    @Test
    public void getUpdatedByValues_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/stravaRuns/updatedByValues")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("$", hasKey("10004"))
                .body("$", hasKey("10005"));
    }

    @Test
    public void testRepositoryDirectAccess() {
        // Test repository is autowired and functional
        assertEquals(3, stravaRunRepository.count());

        // Test findAll
        var runs = stravaRunRepository.findAll();
        assertEquals(3, runs.size());

        // Test findById
        var run = stravaRunRepository.findById(20001L);
        assertTrue(run.isPresent());
        assertEquals(100L, run.get().getCustomerId());
        assertEquals("Morning Trail Run", run.get().getRunName());
        assertEquals(8, run.get().getMiles());
    }
}
