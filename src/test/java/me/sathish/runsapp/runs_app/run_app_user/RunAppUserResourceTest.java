package me.sathish.runsapp.runs_app.run_app_user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;


public class RunAppUserResourceTest extends BaseIT {

    @Test
    void getAllRunAppUsers_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runAppUsers")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).id", Matchers.equalTo(1000));
    }

    @Test
    void getAllRunAppUsers_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runAppUsers?filter=1001")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).id", Matchers.equalTo(1001));
    }

    @Test
    void getAllRunAppUsers_unauthorized() {
        RestAssured
                .given()
                    .redirects().follow(false)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runAppUsers")
                .then()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .body("code", Matchers.equalTo("AUTHORIZATION_DENIED"));
    }

    @Test
    void getRunAppUser_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runAppUsers/1000")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("name", Matchers.equalTo("Zed diam voluptua."));
    }

    @Test
    void getRunAppUser_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runAppUsers/1666")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createRunAppUser_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runAppUserDTORequest.json"))
                .when()
                    .post("/api/runAppUsers")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, runAppUserRepository.count());
    }

    @Test
    void createRunAppUser_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runAppUserDTORequest_missingField.json"))
                .when()
                    .post("/api/runAppUsers")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("email"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    void updateRunAppUser_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runAppUserDTORequest.json"))
                .when()
                    .put("/api/runAppUsers/1000")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("Duis autem vel.", runAppUserRepository.findById(((long)1000)).orElseThrow().getName());
        assertEquals(2, runAppUserRepository.count());
    }

    @Test
    void deleteRunAppUser_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/runAppUsers/1000")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, runAppUserRepository.count());
    }

}
