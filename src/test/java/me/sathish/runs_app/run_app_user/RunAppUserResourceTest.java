package me.sathish.runs_app.run_app_user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.restassured.http.ContentType;
import me.sathish.runs_app.config.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Integration tests for RunAppUser CRUD operations.
 * Tests user management, role assignment, and validation.
 */
public class RunAppUserResourceTest extends BaseIT {

    @Test
    public void getAllUsers_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("content", hasSize(3))
                .body("content[0].email", equalTo("admin@test.com"))
                .body("totalElements", equalTo(3));
    }

    @Test
    public void getAllUsers_withPagination() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(2))
                .body("totalElements", equalTo(3))
                .body("size", equalTo(2));
    }

    @Test
    public void getAllUsers_withFilter() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .queryParam("filter", "10004")
                .when()
                .get("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(10004))
                .body("content[0].email", equalTo("admin@test.com"));
    }

    @Test
    public void getUser_success() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/10004")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("id", equalTo(10004))
                .body("email", equalTo("admin@test.com"))
                .body("name", equalTo("Admin User"))
                .body("roles", hasSize(2))
                .body("roles", hasItems(10001, 10002));
    }

    @Test
    public void getUser_notFound() {
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void createUser_success() {
        String requestBody = readResource("/requests/userRequest.json");

        Long createdId = given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        assertNotNull(createdId);
        assertTrue(createdId > 0);

        // Verify the created user
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/" + createdId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("email", equalTo("newuser@test.com"))
                .body("name", equalTo("New User"))
                .body("roles", hasSize(1))
                .body("roles[0]", equalTo(10002));
    }

    @Test
    public void createUser_duplicateEmail_conflict() {
        String requestBody = """
                {
                  "email": "admin@test.com",
                  "password": "password",
                  "name": "Duplicate Admin",
                  "roles": [10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(anyOf(
                        equalTo(HttpStatus.CONFLICT.value()),
                        equalTo(HttpStatus.BAD_REQUEST.value())
                ));
    }

    @Test
    public void createUser_missingEmail_badRequest() {
        String requestBody = """
                {
                  "password": "password",
                  "name": "No Email User",
                  "roles": [10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createUser_missingPassword_badRequest() {
        String requestBody = """
                {
                  "email": "nopass@test.com",
                  "name": "No Password User",
                  "roles": [10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createUser_missingName_badRequest() {
        String requestBody = """
                {
                  "email": "noname@test.com",
                  "password": "password",
                  "roles": [10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void createUser_noRoles_badRequest() {
        String requestBody = """
                {
                  "email": "noroles@test.com",
                  "password": "password",
                  "name": "No Roles User",
                  "roles": []
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("roles", containsString("At least one role is required"));
    }

    @Test
    public void createUser_multipleRoles_success() {
        String requestBody = """
                {
                  "email": "multirole@test.com",
                  "password": "password",
                  "name": "Multi Role User",
                  "roles": [10001, 10002, 10003]
                }
                """;

        Long createdId = given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        // Verify multiple roles assigned
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/" + createdId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("roles", hasSize(3))
                .body("roles", hasItems(10001, 10002, 10003));
    }

    @Test
    public void createUser_emailTooLong_badRequest() {
        String longEmail = "a".repeat(95) + "@test.com"; // > 100 chars
        String requestBody = String.format("""
                {
                  "email": "%s",
                  "password": "password",
                  "name": "Long Email User",
                  "roles": [10002]
                }
                """, longEmail);

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void updateUser_success() {
        String updateBody = """
                {
                  "email": "admin@test.com",
                  "password": "newpassword",
                  "name": "Admin User Updated",
                  "roles": [10001, 10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/runAppUsers/10004")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(equalTo("10004"));

        // Verify the update
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/10004")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("Admin User Updated"));
    }

    @Test
    public void updateUser_changeRoles_success() {
        String updateBody = """
                {
                  "email": "user@test.com",
                  "password": "password",
                  "name": "Regular User",
                  "roles": [10001, 10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/runAppUsers/10005")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Verify roles updated
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/10005")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("roles", hasSize(2))
                .body("roles", hasItems(10001, 10002));
    }

    @Test
    public void updateUser_notFound() {
        String updateBody = """
                {
                  "email": "test@test.com",
                  "password": "password",
                  "name": "Test User",
                  "roles": [10002]
                }
                """;

        given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/runAppUsers/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void deleteUser_success() {
        // First create a user to delete
        String createBody = """
                {
                  "email": "todelete@test.com",
                  "password": "password",
                  "name": "To Delete",
                  "roles": [10002]
                }
                """;

        Long userId = given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        // Delete the user
        given()
                .sessionId(getAdminSession())
                .when()
                .delete("/api/runAppUsers/" + userId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify it's gone
        given()
                .sessionId(getAdminSession())
                .accept(ContentType.JSON)
                .when()
                .get("/api/runAppUsers/" + userId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void deleteUser_notFound() {
        given()
                .sessionId(getAdminSession())
                .when()
                .delete("/api/runAppUsers/99999")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testRepositoryDirectAccess() {
        // Test repository is autowired and functional
        assertEquals(3, runAppUserRepository.count());

        // Test findAll
        var users = runAppUserRepository.findAll();
        assertEquals(3, users.size());

        // Test findById
        var user = runAppUserRepository.findById(10004L);
        assertTrue(user.isPresent());
        assertEquals("admin@test.com", user.get().getEmail());
        assertEquals("Admin User", user.get().getName());
        assertNotNull(user.get().getPassword());

        // Verify roles loaded
        assertFalse(user.get().getRoles().isEmpty());
    }

    @Test
    public void testPasswordEncryption() {
        // Create a user
        String requestBody = """
                {
                  "email": "encrypted@test.com",
                  "password": "PlainTextPassword123",
                  "name": "Encrypted User",
                  "roles": [10002]
                }
                """;

        Long userId = given()
                .sessionId(getAdminSession())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/runAppUsers")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(Long.class);

        // Verify password is encrypted in database
        var user = runAppUserRepository.findById(userId);
        assertTrue(user.isPresent());

        // Password should be BCrypt hashed (starts with $2a$ or $2b$)
        String storedPassword = user.get().getPassword();
        assertNotNull(storedPassword);
        assertNotEquals("PlainTextPassword123", storedPassword);
        assertTrue(storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$"));
    }
}
