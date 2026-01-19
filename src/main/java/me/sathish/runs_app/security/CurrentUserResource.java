package me.sathish.runs_app.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "basic-auth")
public class CurrentUserResource {

    @GetMapping("/current-user")
    public ResponseEntity<CurrentUserDTO> getCurrentUser(final Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        final List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        final CurrentUserDTO currentUser = new CurrentUserDTO();
        currentUser.setUsername(authentication.getName());
        currentUser.setRoles(roles);

        return ResponseEntity.ok(currentUser);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user", description = "Clears the security context and forces browser to clear cached credentials")
    public ResponseEntity<Void> logout(final HttpServletRequest request,
                                       final HttpServletResponse response) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        response.setHeader("WWW-Authenticate", "Basic realm=\"runsAppSecurity realm\"");
        return ResponseEntity.status(401).build();
    }

}
