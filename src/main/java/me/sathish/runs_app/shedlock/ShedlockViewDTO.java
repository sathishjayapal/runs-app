package me.sathish.runs_app.shedlock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class ShedlockViewDTO {

    @NotNull
    @Size(max = 64)
    private String name;

    @NotNull
    private LocalDateTime lockUntil;

    @NotNull
    private LocalDateTime lockedAt;

    @NotNull
    @Size(max = 255)
    private String lockedBy;

}
