package me.sathish.runs_app.garmin_fit_import;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GarminRunEvent {
    private String eventType;
    private String activityId;
    private String activityName;
    private LocalDateTime activityDate;
    private String distance;
    private String elapsedTime;
    private Long databaseId;
    private String status; // "SUCCESS", "SKIPPED", "FAILED"
    private String errorMessage; // Only populated for FAILED status
    private String fileName; // Source file name
    private String activityType; // "running", "strength_training", "elliptical"
    private String maxHeartRate;
    private String calories;

    public @NotNull @Pattern(regexp = "^(running|strength_training|elliptical)$", message = "Activity type must be running, strength_training, or elliptical") String getActivityType() {
        return activityType;
    }

    public void setActivityType(@NotNull @Pattern(regexp = "^(running|strength_training|elliptical)$", message = "Activity type must be running, strength_training, or elliptical") String activityType) {
        this.activityType = activityType;
    }
}
