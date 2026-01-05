package me.sathish.runsapp.runs_app.security;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUser;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class RunsAppSecurityUserDetailsService implements UserDetailsService {

    private final RunAppUserRepository runAppUserRepository;

    public RunsAppSecurityUserDetailsService(final RunAppUserRepository runAppUserRepository) {
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    public RunsAppSecurityUserDetails loadUserByUsername(final String username) {
        final RunAppUser runAppUser = runAppUserRepository.findByEmailIgnoreCase(username);
        if (runAppUser == null) {
            log.warn("user not found: {}", username);
            throw new UsernameNotFoundException("User " + username + " not found");
        }
        final String role = "roleUser".equals(username) ? UserRoles.ROLE_USER : UserRoles.ROLE_ADMIN;
        final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        return new RunsAppSecurityUserDetails(runAppUser.getId(), username, runAppUser.getPassword(), authorities);
    }

}
