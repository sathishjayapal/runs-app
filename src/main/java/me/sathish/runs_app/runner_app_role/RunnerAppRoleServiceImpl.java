package me.sathish.runs_app.runner_app_role;

import java.util.Map;
import me.sathish.runs_app.events.BeforeDeleteRunnerAppRole;
import me.sathish.runs_app.util.CustomCollectors;
import me.sathish.runs_app.util.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class RunnerAppRoleServiceImpl implements RunnerAppRoleService {

    private final RunnerAppRoleRepository runnerAppRoleRepository;
    private final ApplicationEventPublisher publisher;

    public RunnerAppRoleServiceImpl(final RunnerAppRoleRepository runnerAppRoleRepository,
            final ApplicationEventPublisher publisher) {
        this.runnerAppRoleRepository = runnerAppRoleRepository;
        this.publisher = publisher;
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
        publisher.publishEvent(new BeforeDeleteRunnerAppRole(id));
        runnerAppRoleRepository.delete(runnerAppRole);
    }

    private RunnerAppRoleDTO mapToDTO(final RunnerAppRole runnerAppRole,
            final RunnerAppRoleDTO runnerAppRoleDTO) {
        runnerAppRoleDTO.setId(runnerAppRole.getId());
        runnerAppRoleDTO.setRoleName(runnerAppRole.getRoleName());
        return runnerAppRoleDTO;
    }

    private RunnerAppRole mapToEntity(final RunnerAppRoleDTO runnerAppRoleDTO,
            final RunnerAppRole runnerAppRole) {
        runnerAppRole.setRoleName(runnerAppRoleDTO.getRoleName());
        return runnerAppRole;
    }

    @Override
    public boolean roleNameExists(final String roleName) {
        return runnerAppRoleRepository.existsByRoleNameIgnoreCase(roleName);
    }

    @Override
    public Map<Long, String> getRunnerAppRoleValues() {
        return runnerAppRoleRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(RunnerAppRole::getId, RunnerAppRole::getRoleName));
    }

}
