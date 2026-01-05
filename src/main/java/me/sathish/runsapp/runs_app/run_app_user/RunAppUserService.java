package me.sathish.runsapp.runs_app.run_app_user;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface RunAppUserService {

    Page<RunAppUserDTO> findAll(String filter, Pageable pageable);

    RunAppUserDTO get(Long id);

    Long create(RunAppUserDTO runAppUserDTO);

    void update(Long id, RunAppUserDTO runAppUserDTO);

    void delete(Long id);

    Map<Long, String> getRunAppUserValues();

}
