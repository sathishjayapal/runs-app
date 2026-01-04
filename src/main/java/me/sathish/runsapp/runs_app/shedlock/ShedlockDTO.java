package me.sathish.runsapp.runs_app.shedlock;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ShedlockDTO {

    private Long name;

    @NotNull
    private OffsetDateTime lockUntil;

    @NotNull
    private OffsetDateTime lockedAt;

    @NotNull
    private String lockedBy;

}
