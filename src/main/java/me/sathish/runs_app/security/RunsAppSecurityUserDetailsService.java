package me.sathish.runs_app.security;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class RunsAppSecurityUserDetailsService implements UserDetailsService {

    private final RunAppUserRepository runAppUserRepository;

    public RunsAppSecurityUserDetailsService(final RunAppUserRepository runAppUserRepository) {
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    public RunsAppSecurityUserDetails loadUserByUsername(final String username) {
        final RunAppUser runAppUser = runAppUserRepository.findByEmailIgnoreCaseWithRoles(username);
        if (runAppUser == null) {
            log.warn("user not found: {}", username);
            throw new UsernameNotFoundException("User " + username + " not found");
        }
        final List<SimpleGrantedAuthority> authorities = runAppUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .toList();

        if (authorities.isEmpty()) {
            log.warn("user {} has no roles assigned, defaulting to ROLE_USER", username);
            return new RunsAppSecurityUserDetails(runAppUser.getId(), username, runAppUser.getPassword(),
                    List.of(new SimpleGrantedAuthority(UserRoles.ROLE_USER)));
        }
        
        return new RunsAppSecurityUserDetails(runAppUser.getId(), username, runAppUser.getPassword(), authorities);
    }

}
