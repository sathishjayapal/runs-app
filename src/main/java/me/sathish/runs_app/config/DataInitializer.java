package me.sathish.runs_app.config;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.garmin_run.GarminRun;
import me.sathish.runs_app.garmin_run.GarminRunRepository;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runs_app.runner_app_role.RunnerAppRole;
import me.sathish.runs_app.runner_app_role.RunnerAppRoleRepository;
import me.sathish.runs_app.strava_run.StravaRun;
import me.sathish.runs_app.strava_run.StravaRunRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            RunAppUserRepository userRepository,
            RunnerAppRoleRepository roleRepository,
            StravaRunRepository stravaRunRepository,
            GarminRunRepository garminRunRepository,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Database already initialized. Skipping data initialization.");
                return;
            }

            log.info("Initializing database with sample data...");

            RunnerAppRole adminRole = new RunnerAppRole();
            adminRole.setRoleName("ROLE_ADMIN");
            adminRole.setDescription("Administrator role with full access");
            adminRole = roleRepository.save(adminRole);
            log.info("Created role: {}", adminRole.getRoleName());

            RunnerAppRole userRole = new RunnerAppRole();
            userRole.setRoleName("ROLE_USER");
            userRole.setDescription("Standard user role");
            userRole = roleRepository.save(userRole);
            log.info("Created role: {}", userRole.getRoleName());

            RunAppUser adminUser = new RunAppUser();
            adminUser.setEmail("admin@runsapp.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setName("Admin User");
            adminUser.getRoles().add(adminRole);
            adminUser.getRoles().add(userRole);
            adminUser = userRepository.save(adminUser);
            log.info("Created admin user: {} with roles: ROLE_ADMIN, ROLE_USER", adminUser.getEmail());

            RunAppUser regularUser = new RunAppUser();
            regularUser.setEmail("runner@runsapp.com");
            regularUser.setPassword(passwordEncoder.encode("runner123"));
            regularUser.setName("Regular Runner");
            regularUser.getRoles().add(userRole);
            regularUser = userRepository.save(regularUser);
            log.info("Created regular user: {} with role: ROLE_USER", regularUser.getEmail());

            StravaRun stravaRun1 = new StravaRun();
            stravaRun1.setCustomerId(1001L);
            stravaRun1.setRunName("Morning Run - Central Park");
            stravaRun1.setRunDate(LocalDate.now().minusDays(7));
            stravaRun1.setMiles(5);
            stravaRun1.setStartLocation(12345L);
            stravaRun1.setCreatedBy(regularUser);
            stravaRunRepository.save(stravaRun1);
            log.info("Created Strava run: {}", stravaRun1.getRunName());

            StravaRun stravaRun2 = new StravaRun();
            stravaRun2.setCustomerId(1001L);
            stravaRun2.setRunName("Evening Run - Riverside Trail");
            stravaRun2.setRunDate(LocalDate.now().minusDays(5));
            stravaRun2.setMiles(8);
            stravaRun2.setStartLocation(12346L);
            stravaRun2.setCreatedBy(regularUser);
            stravaRunRepository.save(stravaRun2);
            log.info("Created Strava run: {}", stravaRun2.getRunName());

            StravaRun stravaRun3 = new StravaRun();
            stravaRun3.setCustomerId(1002L);
            stravaRun3.setRunName("Long Run - Marathon Training");
            stravaRun3.setRunDate(LocalDate.now().minusDays(2));
            stravaRun3.setMiles(13);
            stravaRun3.setStartLocation(12347L);
            stravaRun3.setCreatedBy(adminUser);
            stravaRunRepository.save(stravaRun3);
            log.info("Created Strava run: {}", stravaRun3.getRunName());

            GarminRun garminRun1 = new GarminRun();
            garminRun1.setActivityId(new BigDecimal("12345678.90"));
            garminRun1.setActivityDate("2026-01-04T06:30:00Z");
            garminRun1.setActivityType("Running");
            garminRun1.setActivityName("Morning Interval Training");
            garminRun1.setActivityDescription("Speed work with 400m intervals");
            garminRun1.setElapsedTime("00:45:30");
            garminRun1.setDistance("6.5 km");
            garminRun1.setMaxHeartRate("185");
            garminRun1.setCalories("450");
            garminRun1.setCreatedBy(regularUser);
            garminRunRepository.save(garminRun1);
            log.info("Created Garmin run: {}", garminRun1.getActivityName());

            GarminRun garminRun2 = new GarminRun();
            garminRun2.setActivityId(new BigDecimal("12345679.00"));
            garminRun2.setActivityDate("2026-01-06T17:00:00Z");
            garminRun2.setActivityType("Running");
            garminRun2.setActivityName("Easy Recovery Run");
            garminRun2.setActivityDescription("Low intensity recovery session");
            garminRun2.setElapsedTime("00:35:15");
            garminRun2.setDistance("5.0 km");
            garminRun2.setMaxHeartRate("155");
            garminRun2.setCalories("320");
            garminRun2.setCreatedBy(regularUser);
            garminRunRepository.save(garminRun2);
            log.info("Created Garmin run: {}", garminRun2.getActivityName());

            GarminRun garminRun3 = new GarminRun();
            garminRun3.setActivityId(new BigDecimal("12345680.50"));
            garminRun3.setActivityDate("2026-01-09T07:00:00Z");
            garminRun3.setActivityType("Running");
            garminRun3.setActivityName("Hill Repeats Workout");
            garminRun3.setActivityDescription("10 x 200m hill sprints");
            garminRun3.setElapsedTime("00:52:45");
            garminRun3.setDistance("8.2 km");
            garminRun3.setMaxHeartRate("192");
            garminRun3.setCalories("580");
            garminRun3.setCreatedBy(adminUser);
            garminRunRepository.save(garminRun3);
            log.info("Created Garmin run: {}", garminRun3.getActivityName());

            log.info("Database initialization completed successfully!");
            log.info("Sample credentials - Admin: admin@runsapp.com / admin123");
            log.info("Sample credentials - User: runner@runsapp.com / runner123");
        };
    }
}
