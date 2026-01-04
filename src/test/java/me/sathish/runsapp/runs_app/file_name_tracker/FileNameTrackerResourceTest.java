package me.sathish.runsapp.runs_app.file_name_tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class FileNameTrackerResourceTest extends BaseIT {

    @Test
    @Sql("/data/fileNameTrackerData.sql")
    void getAllFileNameTrackers_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/fileNameTrackers")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).id", Matchers.equalTo(1300));
    }

    @Test
    @Sql("/data/fileNameTrackerData.sql")
    void getAllFileNameTrackers_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/fileNameTrackers?filter=1301")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).id", Matchers.equalTo(1301));
    }

    @Test
    @Sql("/data/fileNameTrackerData.sql")
    void getFileNameTracker_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/fileNameTrackers/1300")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("fileName", Matchers.equalTo("Donec pretium vulputate sapien nec sagittis aliquam malesuada."));
    }

    @Test
    void getFileNameTracker_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/fileNameTrackers/1966")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createFileNameTracker_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/fileNameTrackerDTORequest.json"))
                .when()
                    .post("/api/fileNameTrackers")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, fileNameTrackerRepository.count());
    }

    @Test
    void createFileNameTracker_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/fileNameTrackerDTORequest_missingField.json"))
                .when()
                    .post("/api/fileNameTrackers")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("fileName"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/fileNameTrackerData.sql")
    void updateFileNameTracker_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/fileNameTrackerDTORequest.json"))
                .when()
                    .put("/api/fileNameTrackers/1300")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam.", fileNameTrackerRepository.findById(((long)1300)).orElseThrow().getFileName());
        assertEquals(2, fileNameTrackerRepository.count());
    }

    @Test
    @Sql("/data/fileNameTrackerData.sql")
    void deleteFileNameTracker_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/fileNameTrackers/1300")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, fileNameTrackerRepository.count());
    }

}
