package me.sathish.runs_app.run_app_user;

import java.util.Map;
import java.util.stream.Collectors;
import me.sathish.runs_app.events.BeforeDeleteRunAppUser;
import me.sathish.runs_app.runner_app_role.RunnerAppRole;
import me.sathish.runs_app.runner_app_role.RunnerAppRoleRepository;
import me.sathish.runs_app.util.CustomCollectors;
import me.sathish.runs_app.util.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RunAppUserServiceImpl implements RunAppUserService {

    private final RunAppUserRepository runAppUserRepository;
    private final RunnerAppRoleRepository runnerAppRoleRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;

    public RunAppUserServiceImpl(final RunAppUserRepository runAppUserRepository,
            final RunnerAppRoleRepository runnerAppRoleRepository,
            final ApplicationEventPublisher publisher, final PasswordEncoder passwordEncoder) {
        this.runAppUserRepository = runAppUserRepository;
        this.runnerAppRoleRepository = runnerAppRoleRepository;
        this.publisher = publisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RunAppUserDTO> findAll(final String filter, final Pageable pageable) {
        Page<RunAppUser> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = runAppUserRepository.findAllById(longFilter, pageable);
        } else {
            page = runAppUserRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(runAppUser -> mapToDTO(runAppUser, new RunAppUserDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public RunAppUserDTO get(final Long id) {
        return runAppUserRepository.findById(id)
                .map(runAppUser -> mapToDTO(runAppUser, new RunAppUserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    @Transactional
    public Long create(final RunAppUserDTO runAppUserDTO) {
        final RunAppUser runAppUser = new RunAppUser();
        mapToEntity(runAppUserDTO, runAppUser);
        return runAppUserRepository.save(runAppUser).getId();
    }

    @Override
    @Transactional
    public void update(final Long id, final RunAppUserDTO runAppUserDTO) {
        final RunAppUser runAppUser = runAppUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(runAppUserDTO, runAppUser);
        runAppUserRepository.save(runAppUser);
    }

    @Override
    public void delete(final Long id) {
        final RunAppUser runAppUser = runAppUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteRunAppUser(id));
        runAppUserRepository.delete(runAppUser);
    }

    private RunAppUserDTO mapToDTO(final RunAppUser runAppUser, final RunAppUserDTO runAppUserDTO) {
        runAppUserDTO.setId(runAppUser.getId());
        runAppUserDTO.setEmail(runAppUser.getEmail());
        runAppUserDTO.setName(runAppUser.getName());
        runAppUserDTO.setRoles(runAppUser.getRoles().stream()
                .map(RunnerAppRole::getId)
                .collect(Collectors.toList()));
        return runAppUserDTO;
    }

    private RunAppUser mapToEntity(final RunAppUserDTO runAppUserDTO, final RunAppUser runAppUser) {
        runAppUser.setEmail(runAppUserDTO.getEmail());
        runAppUser.setPassword(passwordEncoder.encode(runAppUserDTO.getPassword()));
        runAppUser.setName(runAppUserDTO.getName());
        
        if (runAppUserDTO.getRoles() != null) {
            runAppUser.getRoles().clear();
            for (Long roleId : runAppUserDTO.getRoles()) {
                final RunnerAppRole role = runnerAppRoleRepository.findById(roleId)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
                runAppUser.getRoles().add(role);
            }
        }
        
        return runAppUser;
    }

    @Override
    public Map<Long, String> getRunAppUserValues() {
        return runAppUserRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(RunAppUser::getId, RunAppUser::getEmail));
    }

}
