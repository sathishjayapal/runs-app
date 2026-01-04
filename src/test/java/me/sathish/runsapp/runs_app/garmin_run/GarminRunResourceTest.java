package me.sathish.runsapp.runs_app.garmin_run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class GarminRunResourceTest extends BaseIT {

    @Test
    @Sql("/data/garminRunData.sql")
    void getAllGarminRuns_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/garminRuns")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).id", Matchers.equalTo(1100));
    }

    @Test
    @Sql("/data/garminRunData.sql")
    void getAllGarminRuns_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/garminRuns?filter=1101")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).id", Matchers.equalTo(1101));
    }

    @Test
    @Sql("/data/garminRunData.sql")
    void getGarminRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/garminRuns/1100")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("activityId", Matchers.equalTo("58.08"));
    }

    @Test
    void getGarminRun_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/garminRuns/1766")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createGarminRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/garminRunDTORequest.json"))
                .when()
                    .post("/api/garminRuns")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, garminRunRepository.count());
    }

    @Test
    void createGarminRun_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/garminRunDTORequest_missingField.json"))
                .when()
                    .post("/api/garminRuns")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("activityId"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/garminRunData.sql")
    void updateGarminRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/garminRunDTORequest.json"))
                .when()
                    .put("/api/garminRuns/1100")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals(new BigDecimal("73.08"), garminRunRepository.findById(((long)1100)).orElseThrow().getActivityId());
        assertEquals(2, garminRunRepository.count());
    }

    @Test
    @Sql("/data/garminRunData.sql")
    void deleteGarminRun_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/garminRuns/1100")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, garminRunRepository.count());
    }

}
