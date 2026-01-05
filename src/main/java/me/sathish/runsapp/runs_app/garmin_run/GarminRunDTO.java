package me.sathish.runsapp.runs_app.garmin_run;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GarminRunDTO {

    private Long id;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "62.08")
    private BigDecimal activityId;

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

    @Size(max = 40)
    private String updatedBy;

    @NotNull
    private Long createdBy;

}
