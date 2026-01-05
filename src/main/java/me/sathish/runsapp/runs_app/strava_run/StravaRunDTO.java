package me.sathish.runsapp.runs_app.strava_run;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class StravaRunDTO {

    private Long runNumber;

    @NotNull
    private Long customerId;

    @NotNull
    @Size(max = 100)
    private String runName;

    @NotNull
    private LocalDate runDate;

    @NotNull
    private Integer miles;

    @NotNull
    private Long startLocation;

    @NotNull
    private Long createdBy;

    @NotNull
    private Long updatedBy;

}
