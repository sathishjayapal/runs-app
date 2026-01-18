package me.sathish.runs_app.garmin_run;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^(running|strength_training|elliptical)$", message = "Activity type must be running, strength_training, or elliptical")
    private String activityType;

    @NotNull
    private String activityName;

    private String activityDescription;

    @Pattern(regexp = "^\\d{2}:\\d{2}:\\d{2}$", message = "Elapsed time must be in HH:MM:SS format")
    private String elapsedTime;

    @NotNull
    @Pattern(regexp = "^\\d+(\\.\\d+)?$", message = "Distance must be a valid number")
    private String distance;

    @Pattern(regexp = "^\\d+$", message = "Max heart rate must be a valid integer")
    private String maxHeartRate;

    @Pattern(regexp = "^\\d+$", message = "Calories must be a valid integer")
    private String calories;

    @NotNull
    private Long createdBy;

    private Long updateBy;

}
