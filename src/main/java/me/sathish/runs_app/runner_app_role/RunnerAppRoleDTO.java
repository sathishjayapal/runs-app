package me.sathish.runs_app.runner_app_role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RunnerAppRoleDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    @RunnerAppRoleRoleNameUnique
    private String roleName;

    @Size(max = 255)
    private String description;

}
