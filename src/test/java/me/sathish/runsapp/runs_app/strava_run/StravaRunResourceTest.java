package me.sathish.runsapp.runs_app.strava_run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class StravaRunResourceTest extends BaseIT {

    @Test
    @Sql("/data/stravaRunData.sql")
    void getAllStravaRuns_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/stravaRuns")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("size()", Matchers.equalTo(2))
                    .body("get(0).runNumber", Matchers.equalTo(1400));
    }

    @Test
    @Sql("/data/stravaRunData.sql")
    void getStravaRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/stravaRuns/1400")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("customerId", Matchers.equalTo(15));
    }

    @Test
    void getStravaRun_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/stravaRuns/2066")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createStravaRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/stravaRunDTORequest.json"))
                .when()
                    .post("/api/stravaRuns")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, stravaRunRepository.count());
    }

    @Test
    void createStravaRun_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/stravaRunDTORequest_missingField.json"))
                .when()
                    .post("/api/stravaRuns")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("customerId"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/stravaRunData.sql")
    void updateStravaRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/stravaRunDTORequest.json"))
                .when()
                    .put("/api/stravaRuns/1400")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals(((long)70), stravaRunRepository.findById(((long)1400)).orElseThrow().getCustomerId());
        assertEquals(2, stravaRunRepository.count());
    }

    @Test
    @Sql("/data/stravaRunData.sql")
    void deleteStravaRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/stravaRuns/1400")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, stravaRunRepository.count());
    }

}
