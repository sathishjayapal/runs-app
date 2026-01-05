package me.sathish.runsapp.runs_app.runner_app_role;

import me.sathish.runsapp.runs_app.events.BeforeDeleteRunAppUser;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUser;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runsapp.runs_app.util.NotFoundException;
import me.sathish.runsapp.runs_app.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class RunnerAppRoleServiceImpl implements RunnerAppRoleService {

    private final RunnerAppRoleRepository runnerAppRoleRepository;
    private final RunAppUserRepository runAppUserRepository;

    public RunnerAppRoleServiceImpl(final RunnerAppRoleRepository runnerAppRoleRepository,
            final RunAppUserRepository runAppUserRepository) {
        this.runnerAppRoleRepository = runnerAppRoleRepository;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    public Page<RunnerAppRoleDTO> findAll(final String filter, final Pageable pageable) {
        Page<RunnerAppRole> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = runnerAppRoleRepository.findAllById(longFilter, pageable);
        } else {
            page = runnerAppRoleRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(runnerAppRole -> mapToDTO(runnerAppRole, new RunnerAppRoleDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    public RunnerAppRoleDTO get(final Long id) {
        return runnerAppRoleRepository.findById(id)
                .map(runnerAppRole -> mapToDTO(runnerAppRole, new RunnerAppRoleDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final RunnerAppRoleDTO runnerAppRoleDTO) {
        final RunnerAppRole runnerAppRole = new RunnerAppRole();
        mapToEntity(runnerAppRoleDTO, runnerAppRole);
        return runnerAppRoleRepository.save(runnerAppRole).getId();
    }

    @Override
    public void update(final Long id, final RunnerAppRoleDTO runnerAppRoleDTO) {
        final RunnerAppRole runnerAppRole = runnerAppRoleRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(runnerAppRoleDTO, runnerAppRole);
        runnerAppRoleRepository.save(runnerAppRole);
    }

    @Override
    public void delete(final Long id) {
        final RunnerAppRole runnerAppRole = runnerAppRoleRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        runnerAppRoleRepository.delete(runnerAppRole);
    }

    private RunnerAppRoleDTO mapToDTO(final RunnerAppRole runnerAppRole,
            final RunnerAppRoleDTO runnerAppRoleDTO) {
        runnerAppRoleDTO.setId(runnerAppRole.getId());
        runnerAppRoleDTO.setRoleName(runnerAppRole.getRoleName());
        runnerAppRoleDTO.setRunnerUserRoles(runnerAppRole.getRunnerUserRoles() == null ? null : runnerAppRole.getRunnerUserRoles().getId());
        return runnerAppRoleDTO;
    }

    private RunnerAppRole mapToEntity(final RunnerAppRoleDTO runnerAppRoleDTO,
            final RunnerAppRole runnerAppRole) {
        runnerAppRole.setRoleName(runnerAppRoleDTO.getRoleName());
        final RunAppUser runnerUserRoles = runnerAppRoleDTO.getRunnerUserRoles() == null ? null : runAppUserRepository.findById(runnerAppRoleDTO.getRunnerUserRoles())
                .orElseThrow(() -> new NotFoundException("runnerUserRoles not found"));
        runnerAppRole.setRunnerUserRoles(runnerUserRoles);
        return runnerAppRole;
    }

    @Override
    public boolean roleNameExists(final String roleName) {
        return runnerAppRoleRepository.existsByRoleNameIgnoreCase(roleName);
    }

    @EventListener(BeforeDeleteRunAppUser.class)
    public void on(final BeforeDeleteRunAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final RunnerAppRole runnerUserRolesRunnerAppRole = runnerAppRoleRepository.findFirstByRunnerUserRolesId(event.getId());
        if (runnerUserRolesRunnerAppRole != null) {
            referencedException.setKey("runAppUser.runnerAppRole.runnerUserRoles.referenced");
            referencedException.addParam(runnerUserRolesRunnerAppRole.getId());
            throw referencedException;
        }
    }

}
