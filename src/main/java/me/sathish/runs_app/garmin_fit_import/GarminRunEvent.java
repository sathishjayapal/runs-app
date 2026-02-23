package me.sathish.runs_app.garmin_fit_import;

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
}
