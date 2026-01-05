package me.sathish.runsapp.runs_app.runner_app_role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RunnerAppRoleRepository extends JpaRepository<RunnerAppRole, Long> {

    Page<RunnerAppRole> findAllById(Long id, Pageable pageable);

    RunnerAppRole findFirstByRunnerUserRolesId(Long id);

    boolean existsByRoleNameIgnoreCase(String roleName);

}
