package me.sathish.runsapp.runs_app.config;

import me.sathish.runsapp.runs_app.role.Role;
import me.sathish.runsapp.runs_app.role.RoleRepository;
import me.sathish.runsapp.runs_app.strava_run.StravaRun;
import me.sathish.runsapp.runs_app.strava_run.StravaRunRepository;
import me.sathish.runsapp.runs_app.user.User;
import me.sathish.runsapp.runs_app.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StravaRunRepository stravaRunRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            // Get roles (created by Flyway migration)
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("USER role not found"));

            // Create admin user
            User admin = new User();
            admin.setEmail("admin@runsapp.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("Admin User");
            admin.setRole(adminRole);
            admin.setCreatedAt(OffsetDateTime.now());
            admin.setUpdatedAt(OffsetDateTime.now());
            admin = userRepository.save(admin);

            // Create regular users
            User user1 = new User();
            user1.setEmail("john.doe@example.com");
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setName("John Doe");
            user1.setRole(userRole);
            user1.setCreatedAt(OffsetDateTime.now());
            user1.setUpdatedAt(OffsetDateTime.now());
            user1 = userRepository.save(user1);

            User user2 = new User();
            user2.setEmail("jane.smith@example.com");
            user2.setPassword(passwordEncoder.encode("password123"));
            user2.setName("Jane Smith");
            user2.setRole(userRole);
            user2.setCreatedAt(OffsetDateTime.now());
            user2.setUpdatedAt(OffsetDateTime.now());
            user2 = userRepository.save(user2);

            // Create Strava runs
            createStravaRun(stravaRunRepository, "Morning Run - Central Park",
                    user1.getId(), LocalDate.now().minusDays(1), 5, 1L, user1);

            createStravaRun(stravaRunRepository, "Evening Trail Run",
                    user1.getId(), LocalDate.now().minusDays(3), 8, 2L, user1);

            createStravaRun(stravaRunRepository, "Weekend Long Run",
                    user2.getId(), LocalDate.now().minusDays(7), 13, 3L, user2);

            System.out.println("✅ Test data initialized successfully!");
            System.out.println("👤 Admin: admin@runsapp.com / admin123 (ADMIN)");
            System.out.println("👤 User1: john.doe@example.com / password123 (USER)");
            System.out.println("👤 User2: jane.smith@example.com / password123 (USER)");
        };
    }

    private void createStravaRun(StravaRunRepository repository, String name,
                                 Long customerId, LocalDate date, Integer miles,
                                 Long locationId, User createdBy) {
        StravaRun run = new StravaRun();
        run.setRunName(name);
        run.setCustomerId(customerId);
        run.setRunDate(date);
        run.setMiles(miles);
        run.setStartLocation(locationId);
        run.setCreatedBy(createdBy);
        run.setCreatedAt(OffsetDateTime.now());
        run.setUpdatedAt(OffsetDateTime.now());
        run.setUpdatedBy(createdBy);
        repository.save(run);
    }
}
