package me.sathish.runs_app.garmin_run;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GarminRunDTO {

    private Long id;

    @NotNull
    private String activityId;

    @NotNull
    private String activityDate;

    @NotNull
    private String activityType;

    @NotNull
    private String activityName;

    private String activityDescription;

    private String elapsedTime;

    @NotNull
    private String distance;

    private String maxHeartRate;

    private String calories;

    @NotNull
    private Long createdBy;

    private Long updateBy;

}
