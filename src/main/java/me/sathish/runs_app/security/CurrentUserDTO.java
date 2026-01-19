package me.sathish.runs_app.security;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class CurrentUserDTO {

    private String username;
    private List<String> roles;

}
