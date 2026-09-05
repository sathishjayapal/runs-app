package me.sathish.runs_app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.initialization")
@Getter
@Setter
public class InitializationConfig {

    /**
     * Enable sample data initialization. Default: false
     * Set to true only in dev/test profiles
     */
    private boolean enableSampleData = false;

    /**
     * Sample admin user email. Required if enableSampleData is true.
     */
    private String adminEmail;

    /**
     * Sample admin user password. Required if enableSampleData is true.
     * Should come from environment variable, never committed to repo.
     */
    private String adminPassword;

    /**
     * Sample regular user email. Required if enableSampleData is true.
     */
    private String userEmail;

    /**
     * Sample regular user password. Required if enableSampleData is true.
     * Should come from environment variable, never committed to repo.
     */
    private String userPassword;

    /**
     * System user email for automated imports. Required if enableSampleData is true.
     */
    private String systemEmail;

    /**
     * System user password. Required if enableSampleData is true.
     * Should come from environment variable, never committed to repo.
     */
    private String systemPassword;
}
