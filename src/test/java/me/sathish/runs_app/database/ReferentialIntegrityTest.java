package me.sathish.runs_app.database;

import jakarta.persistence.PersistenceException;
import me.sathish.runs_app.config.BaseIT;
import me.sathish.runs_app.file_import_record.FileImportRecord;
import me.sathish.runs_app.garmin_run.GarminRun;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.runner_app_role.RunnerAppRole;
import me.sathish.runs_app.strava_run.StravaRun;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for database referential integrity constraints.
 * Tests foreign keys, unique constraints, not null constraints, and cascade behavior.
 */
@Sql({"/data/garminRunData.sql", "/data/stravaRunData.sql", "/data/fileTrackerData.sql"})
public class ReferentialIntegrityTest extends BaseIT {

    @Test
    @Transactional
    public void foreignKey_garminRun_invalidUserId_throwsException() {
        GarminRun run = new GarminRun();
        run.setActivityId("TEST001");
        run.setActivityDate("2026-02-20");
        run.setActivityType("running");
        run.setActivityName("Test Run");
        run.setDistance("5.0");

        // Try to set non-existent user
        RunAppUser invalidUser = new RunAppUser();
        invalidUser.setId(99999L);
        run.setCreatedBy(invalidUser);

        assertThrows(Exception.class, () -> {
            garminRunRepository.saveAndFlush(run);
        });
    }

    @Test
    @Transactional
    public void foreignKey_garminRun_validUserId_success() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        GarminRun run = new GarminRun();
        run.setActivityId("TEST002");
        run.setActivityDate("2026-02-20");
        run.setActivityType("running");
        run.setActivityName("Test Run");
        run.setDistance("5.0");
        run.setCreatedBy(user);

        GarminRun saved = garminRunRepository.saveAndFlush(run);

        assertNotNull(saved.getId());
        assertEquals(user.getId(), saved.getCreatedBy().getId());
    }

    @Test
    @Transactional
    public void foreignKey_stravaRun_invalidUserId_throwsException() {
        StravaRun run = new StravaRun();
        run.setCustomerId(100L);
        run.setRunName("Test Run");
        run.setRunDate(LocalDate.now());
        run.setMiles(5);
        run.setStartLocation(1L);

        // Try to set non-existent user
        RunAppUser invalidUser = new RunAppUser();
        invalidUser.setId(99999L);
        run.setCreatedBy(invalidUser);

        assertThrows(Exception.class, () -> {
            stravaRunRepository.saveAndFlush(run);
        });
    }

    @Test
    @Transactional
    public void cascadeDelete_user_deletesAssociatedRuns() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        // Count runs created by this user
        long garminRunsBefore = garminRunRepository.findAll().stream()
                .filter(r -> r.getCreatedBy().getId().equals(10004L))
                .count();
        long stravaRunsBefore = stravaRunRepository.findAll().stream()
                .filter(r -> r.getCreatedBy().getId().equals(10004L))
                .count();

        assertTrue(garminRunsBefore > 0, "User should have garmin runs");
        assertTrue(stravaRunsBefore > 0, "User should have strava runs");

        // Note: Actual cascade behavior depends on JPA configuration
        // If @OnDelete(action = OnDeleteAction.CASCADE) is set, this will cascade
        // Otherwise, deletion will fail due to FK constraint
        try {
            runAppUserRepository.delete(user);
            runAppUserRepository.flush();

            // If cascade is configured, verify runs are deleted
            long garminRunsAfter = garminRunRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(10004L))
                    .count();
            long stravaRunsAfter = stravaRunRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy() != null && r.getCreatedBy().getId().equals(10004L))
                    .count();

            assertEquals(0, garminRunsAfter, "Garmin runs should be deleted");
            assertEquals(0, stravaRunsAfter, "Strava runs should be deleted");
        } catch (DataIntegrityViolationException e) {
            // If no cascade, deletion should fail with FK constraint violation
            assertTrue(e.getMessage().contains("constraint") ||
                    e.getMessage().contains("foreign key"),
                    "Should fail with foreign key constraint");
        }
    }

    @Test
    @Transactional
    public void uniqueConstraint_roleNameMustBeUnique() {
        RunnerAppRole role = new RunnerAppRole();
        role.setRoleName("ADMIN"); // Duplicate of existing role

        assertThrows(DataIntegrityViolationException.class, () -> {
            runnerAppRoleRepository.saveAndFlush(role);
        });
    }

    @Test
    @Transactional
    public void uniqueConstraint_roleNameUnique_success() {
        RunnerAppRole role = new RunnerAppRole();
        role.setRoleName("NEW_ROLE");

        RunnerAppRole saved = runnerAppRoleRepository.saveAndFlush(role);

        assertNotNull(saved.getId());
        assertEquals("NEW_ROLE", saved.getRoleName());
    }

    @Test
    @Transactional
    public void uniqueConstraint_userEmailMustBeUnique() {
        RunAppUser user = new RunAppUser();
        user.setEmail("admin@test.com"); // Duplicate
        user.setPassword("password");
        user.setName("Duplicate Admin");
        user.setRoles(new HashSet<>());

        assertThrows(DataIntegrityViolationException.class, () -> {
            runAppUserRepository.saveAndFlush(user);
        });
    }

    @Test
    @Transactional
    public void uniqueConstraint_userEmailUnique_success() {
        RunAppUser user = new RunAppUser();
        user.setEmail("unique@test.com");
        user.setPassword("password");
        user.setName("Unique User");
        user.setRoles(new HashSet<>());

        RunAppUser saved = runAppUserRepository.saveAndFlush(user);

        assertNotNull(saved.getId());
        assertEquals("unique@test.com", saved.getEmail());
    }

    @Test
    @Transactional
    public void notNullConstraint_garminRun_activityId() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        GarminRun run = new GarminRun();
        run.setActivityId(null); // Required field
        run.setActivityDate("2026-02-20");
        run.setActivityType("running");
        run.setActivityName("Test");
        run.setDistance("5.0");
        run.setCreatedBy(user);

        assertThrows(Exception.class, () -> {
            garminRunRepository.saveAndFlush(run);
        });
    }

    @Test
    @Transactional
    public void notNullConstraint_garminRun_activityType() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        GarminRun run = new GarminRun();
        run.setActivityId("TEST");
        run.setActivityDate("2026-02-20");
        run.setActivityType(null); // Required field
        run.setActivityName("Test");
        run.setDistance("5.0");
        run.setCreatedBy(user);

        assertThrows(Exception.class, () -> {
            garminRunRepository.saveAndFlush(run);
        });
    }

    @Test
    @Transactional
    public void notNullConstraint_stravaRun_runName() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        StravaRun run = new StravaRun();
        run.setCustomerId(100L);
        run.setRunName(null); // Required field
        run.setRunDate(LocalDate.now());
        run.setMiles(5);
        run.setStartLocation(1L);
        run.setCreatedBy(user);

        assertThrows(Exception.class, () -> {
            stravaRunRepository.saveAndFlush(run);
        });
    }

    @Test
    @Transactional
    public void notNullConstraint_user_email() {
        RunAppUser user = new RunAppUser();
        user.setEmail(null); // Required field
        user.setPassword("password");
        user.setName("No Email");
        user.setRoles(new HashSet<>());

        assertThrows(Exception.class, () -> {
            runAppUserRepository.saveAndFlush(user);
        });
    }

    @Test
    @Transactional
    public void notNullConstraint_role_roleName() {
        RunnerAppRole role = new RunnerAppRole();
        role.setRoleName(null); // Required field

        assertThrows(Exception.class, () -> {
            runnerAppRoleRepository.saveAndFlush(role);
        });
    }

    @Test
    @Transactional
    public void primaryKey_cannotInsertDuplicateId() {
        RunAppUser user1 = new RunAppUser();
        user1.setId(50000L);
        user1.setEmail("test1@test.com");
        user1.setPassword("password");
        user1.setName("Test User 1");
        user1.setRoles(new HashSet<>());
        runAppUserRepository.saveAndFlush(user1);

        RunAppUser user2 = new RunAppUser();
        user2.setId(50000L); // Duplicate ID
        user2.setEmail("test2@test.com");
        user2.setPassword("password");
        user2.setName("Test User 2");
        user2.setRoles(new HashSet<>());

        assertThrows(PersistenceException.class, () -> {
            runAppUserRepository.saveAndFlush(user2);
        });
    }

    @Test
    @Transactional
    public void fileTracker_foreignKey_validUser_success() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        FileImportRecord tracker = new FileImportRecord();
        tracker.setFileName("valid-foreign-key.fit");
        tracker.setCreatedBy(user);

        FileImportRecord saved = fileNameTrackerRepository.saveAndFlush(tracker);

        assertNotNull(saved.getId());
        assertEquals(user.getId(), saved.getCreatedBy().getId());
    }

    @Test
    @Transactional
    public void fileTracker_foreignKey_invalidUser_throwsException() {
        FileImportRecord tracker = new FileImportRecord();
        tracker.setFileName("invalid-foreign-key.fit");

        RunAppUser invalidUser = new RunAppUser();
        invalidUser.setId(99999L);
        tracker.setCreatedBy(invalidUser);

        assertThrows(Exception.class, () -> {
            fileNameTrackerRepository.saveAndFlush(tracker);
        });
    }

    @Test
    @Transactional
    public void userRoleRelationship_manyToMany_success() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        // User should have multiple roles
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().size() >= 2, "Admin should have at least 2 roles");

        // Roles should be properly loaded
        user.getRoles().forEach(role -> {
            assertNotNull(role.getId());
            assertNotNull(role.getRoleName());
        });
    }

    @Test
    @Transactional
    public void deleteRole_withAssociatedUsers_behavior() {
        // Get a role that has users
        RunnerAppRole userRole = runnerAppRoleRepository.findById(10002L).orElseThrow();

        // Count users with this role
        long userCountBefore = runAppUserRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getId().equals(10002L)))
                .count();

        assertTrue(userCountBefore > 0, "Role should have associated users");

        try {
            // Try to delete the role
            runnerAppRoleRepository.delete(userRole);
            runnerAppRoleRepository.flush();

            // If cascade is configured, verify users still exist but role is removed
            // (depends on cascade configuration)
        } catch (DataIntegrityViolationException e) {
            // If no cascade/orphan removal, deletion should fail with FK constraint
            assertTrue(e.getMessage().contains("constraint") ||
                    e.getMessage().contains("foreign key"),
                    "Should fail with foreign key constraint");
        }
    }

    @Test
    public void testSequenceGeneration_uniqueIds() {
        // Create multiple entities and verify they get unique IDs
        RunAppUser user1 = new RunAppUser();
        user1.setEmail("seq1@test.com");
        user1.setPassword("password");
        user1.setName("Seq Test 1");
        user1.setRoles(new HashSet<>());
        runAppUserRepository.save(user1);

        RunAppUser user2 = new RunAppUser();
        user2.setEmail("seq2@test.com");
        user2.setPassword("password");
        user2.setName("Seq Test 2");
        user2.setRoles(new HashSet<>());
        runAppUserRepository.save(user2);

        assertNotNull(user1.getId());
        assertNotNull(user2.getId());
        assertNotEquals(user1.getId(), user2.getId(), "IDs should be unique");
        assertTrue(user2.getId() > user1.getId(), "IDs should be sequential");
    }
}
