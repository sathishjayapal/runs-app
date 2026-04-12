package me.sathish.runs_app.config;

import me.sathish.runs_app.garmin_fit_import.GarminCsvImportProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;


@Configuration
@EnableAsync
@EnableConfigurationProperties(GarminCsvImportProperties.class)
public class AppConfig {
}