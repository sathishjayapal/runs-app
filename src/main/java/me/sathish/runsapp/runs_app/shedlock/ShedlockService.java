package me.sathish.runsapp.runs_app.shedlock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ShedlockService {

    Page<ShedlockDTO> findAll(String filter, Pageable pageable);

    ShedlockDTO get(Long name);

    Long create(ShedlockDTO shedlockDTO);

    void update(Long name, ShedlockDTO shedlockDTO);

    void delete(Long name);

}
