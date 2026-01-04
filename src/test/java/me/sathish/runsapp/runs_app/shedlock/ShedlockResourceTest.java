package me.sathish.runsapp.runs_app.shedlock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class ShedlockResourceTest extends BaseIT {

    @Test
    @Sql("/data/shedlockData.sql")
    void getAllShedlocks_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/shedlocks")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).name", Matchers.equalTo(1200));
    }

    @Test
    @Sql("/data/shedlockData.sql")
    void getAllShedlocks_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/shedlocks?filter=1201")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).name", Matchers.equalTo(1201));
    }

    @Test
    @Sql("/data/shedlockData.sql")
    void getShedlock_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/shedlocks/1200")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("lockedBy", Matchers.equalTo("Erat pellentesque adipiscing commodo elit."));
    }

    @Test
    void getShedlock_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/shedlocks/1866")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createShedlock_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/shedlockDTORequest.json"))
                .when()
                    .post("/api/shedlocks")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, shedlockRepository.count());
    }

    @Test
    void createShedlock_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/shedlockDTORequest_missingField.json"))
                .when()
                    .post("/api/shedlocks")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("lockUntil"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/shedlockData.sql")
    void updateShedlock_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/shedlockDTORequest.json"))
                .when()
                    .put("/api/shedlocks/1200")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("Tortor consequat id porta nibh venenatis cras sed.", shedlockRepository.findById(((long)1200)).orElseThrow().getLockedBy());
        assertEquals(2, shedlockRepository.count());
    }

    @Test
    @Sql("/data/shedlockData.sql")
    void deleteShedlock_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/shedlocks/1200")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, shedlockRepository.count());
    }

}
