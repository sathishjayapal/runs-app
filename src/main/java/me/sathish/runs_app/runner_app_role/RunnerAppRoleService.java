package me.sathish.runs_app.runner_app_role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface RunnerAppRoleService {

    Page<RunnerAppRoleDTO> findAll(String filter, Pageable pageable);

    RunnerAppRoleDTO get(Long id);

    Long create(RunnerAppRoleDTO runnerAppRoleDTO);

    void update(Long id, RunnerAppRoleDTO runnerAppRoleDTO);

    void delete(Long id);

    boolean roleNameExists(String roleName);

}
