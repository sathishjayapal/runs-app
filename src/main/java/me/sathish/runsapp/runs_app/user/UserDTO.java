package me.sathish.runsapp.runs_app.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UserDTO {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String email;

    @NotNull
    @Size(max = 100)
    private String password;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    @Size(max = 20)
    private String role;

    @NotNull
    private OffsetDateTime createdAt;

}
