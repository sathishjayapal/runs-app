package me.sathish.runsapp.runs_app.runner_app_role;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import me.sathish.runsapp.runs_app.config.BaseIT;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;


public class RunnerAppRoleResourceTest extends BaseIT {

    @Test
    @Sql("/data/runnerAppRoleData.sql")
    void getAllRunnerAppRoles_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runnerAppRoles")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(2))
                    .body("content.get(0).id", Matchers.equalTo(1500));
    }

    @Test
    @Sql("/data/runnerAppRoleData.sql")
    void getAllRunnerAppRoles_filtered() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runnerAppRoles?filter=1501")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).id", Matchers.equalTo(1501));
    }

    @Test
    @Sql("/data/runnerAppRoleData.sql")
    void getRunnerAppRole_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runnerAppRoles/1500")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("roleName", Matchers.equalTo("Nulla facilisis."));
    }

    @Test
    void getRunnerAppRole_notFound() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/runnerAppRoles/2166")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createRunnerAppRole_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runnerAppRoleDTORequest.json"))
                .when()
                    .post("/api/runnerAppRoles")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(1, runnerAppRoleRepository.count());
    }

    @Test
    void createRunnerAppRole_missingField() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runnerAppRoleDTORequest_missingField.json"))
                .when()
                    .post("/api/runnerAppRoles")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("roleName"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    @Sql("/data/runnerAppRoleData.sql")
    void updateRunnerAppRole_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/runnerAppRoleDTORequest.json"))
                .when()
                    .put("/api/runnerAppRoles/1500")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("No sea takimata.", runnerAppRoleRepository.findById(((long)1500)).orElseThrow().getRoleName());
        assertEquals(2, runnerAppRoleRepository.count());
    }

    @Test
    @Sql("/data/runnerAppRoleData.sql")
    void deleteRunnerAppRole_success() {
        RestAssured
                .given()
                    .auth().preemptive().basic(ROLE_ADMIN, PASSWORD)
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/runnerAppRoles/1500")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, runnerAppRoleRepository.count());
    }

}
