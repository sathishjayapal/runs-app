package me.sathish.runs_app.garmin_run;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.restassured.http.ContentType;
import me.sathish.runs_app.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration tests for GarminRun CRUD operations.
 * Tests REST API endpoints for creating, reading, updating, and deleting Garmin run activities.
 */
@Sql("/data/garminRunData.sql")
public class GarminRunResourceTest extends BaseIT {

    @Test
    public void getAllGarminRuns_success() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("content", hasSize(5))
                .body("content[0].activityId", equalTo("GARMIN001"))
                .body("totalElements", equalTo(5))
                .body("size", equalTo(20));
    }

    @Test
    public void getAllGarminRuns_withPagination() {
        given()
                .accept(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(2))
                .body("totalElements", equalTo(5))
                .body("size", equalTo(2))
                .body("number", equalTo(0));
    }

    @Test
    public void getAllGarminRuns_withFilter() {
        given()
                .accept(ContentType.JSON)
                .queryParam("filter", "10007")
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(10007))
                .body("content[0].activityName", equalTo("Morning 10K"));
    }

    @Test
    public void getGarminRun_success() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/10007")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("id", equalTo(10007))
                .body("activityId", equalTo("GARMIN001"))
                .body("activityDate", equalTo("2026-02-01"))
                .body("activityType", equalTo("running"))
                .body("activityName", equalTo("Morning 10K"))
                .body("distance", equalTo("10.5"))
                .body("maxHeartRate", equalTo("175"))
                .body("calories", equalTo("680"))
                .body("createdBy", equalTo(10004));
    }

    @Test
    public void getGarminRun_notFound() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void createGarminRun_success() {
        String requestBody = readResource("/requests/garminRunRequest.json");

        Long createdId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        assertNotNull(createdId);
        assertTrue(createdId > 0);

        // Verify the created run
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/" + createdId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activityId", equalTo("12345678901"))
                .body("activityName", equalTo("Morning Run"))
                .body("activityType", equalTo("running"))
                .body("distance", equalTo("5.2"))
                .body("elapsedTime", equalTo("00:28:45"))
                .body("maxHeartRate", equalTo("165"))
                .body("calories", equalTo("420"));
    }

    @Test
    public void createGarminRun_missingRequiredFields() {
        String requestBody = readResource("/requests/garminRunRequest_missingField.json");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createGarminRun_invalidActivityType() {
        String requestBody = readResource("/requests/garminRunRequest_invalidType.json");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("activityType", containsString("Activity type must be running, strength_training, or elliptical"));
    }

    @Test
    public void createGarminRun_invalidElapsedTimeFormat() {
        String requestBody = """
                {
                  "activityId": "TEST001",
                  "activityDate": "2026-02-20",
                  "activityType": "running",
                  "activityName": "Test Run",
                  "elapsedTime": "invalid_time",
                  "distance": "5.0",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("elapsedTime", containsString("HH:MM:SS format"));
    }

    @Test
    public void createGarminRun_invalidDistanceFormat() {
        String requestBody = """
                {
                  "activityId": "TEST002",
                  "activityDate": "2026-02-20",
                  "activityType": "running",
                  "activityName": "Test Run",
                  "distance": "invalid_distance",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("distance", containsString("must be a valid number"));
    }

    @Test
    public void updateGarminRun_success() {
        String updateBody = """
                {
                  "activityId": "GARMIN001_UPDATED",
                  "activityDate": "2026-02-01",
                  "activityType": "running",
                  "activityName": "Morning 10K - Updated",
                  "activityDescription": "Updated description",
                  "elapsedTime": "00:50:00",
                  "distance": "10.8",
                  "maxHeartRate": "180",
                  "calories": "700",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/garminRuns/10007")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(equalTo("10007"));

        // Verify the update
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/10007")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("activityId", equalTo("GARMIN001_UPDATED"))
                .body("activityName", equalTo("Morning 10K - Updated"))
                .body("distance", equalTo("10.8"))
                .body("elapsedTime", equalTo("00:50:00"))
                .body("maxHeartRate", equalTo("180"))
                .body("calories", equalTo("700"));
    }

    @Test
    public void updateGarminRun_notFound() {
        String updateBody = """
                {
                  "activityId": "TEST",
                  "activityDate": "2026-02-20",
                  "activityType": "running",
                  "activityName": "Test",
                  "distance": "5.0",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/garminRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void deleteGarminRun_success() {
        // First verify the run exists
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/10011")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Delete the run
        given()
                .when()
                .delete("/api/garminRuns/10011")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify it's gone
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/10011")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());

        // Verify count decreased
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalElements", equalTo(4));
    }

    @Test
    public void deleteGarminRun_notFound() {
        given()
                .when()
                .delete("/api/garminRuns/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void getCreatedByValues_success() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/createdByValues")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("$", hasKey("10004"))
                .body("$", hasKey("10005"));
    }

    @Test
    public void getUpdateByValues_success() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/garminRuns/updateByValues")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("$", hasKey("10004"))
                .body("$", hasKey("10005"));
    }

    @Test
    public void testActivityTypeValidation_running() {
        String requestBody = """
                {
                  "activityId": "TEST_RUN",
                  "activityDate": "2026-02-20",
                  "activityType": "running",
                  "activityName": "Test Running",
                  "distance": "5.0",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void testActivityTypeValidation_strengthTraining() {
        String requestBody = """
                {
                  "activityId": "TEST_STRENGTH",
                  "activityDate": "2026-02-20",
                  "activityType": "strength_training",
                  "activityName": "Test Strength",
                  "distance": "0.0",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void testActivityTypeValidation_elliptical() {
        String requestBody = """
                {
                  "activityId": "TEST_ELLIPTICAL",
                  "activityDate": "2026-02-20",
                  "activityType": "elliptical",
                  "activityName": "Test Elliptical",
                  "distance": "3.0",
                  "createdBy": 10004
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/garminRuns")
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void testRepositoryDirectAccess() {
        // Test repository is autowired and functional
        assertEquals(5, garminRunRepository.count());

        // Test findAll
        var runs = garminRunRepository.findAll();
        assertEquals(5, runs.size());

        // Test findById
        var run = garminRunRepository.findById(10007L);
        assertTrue(run.isPresent());
        assertEquals("GARMIN001", run.get().getActivityId());
        assertEquals("Morning 10K", run.get().getActivityName());
    }
}
