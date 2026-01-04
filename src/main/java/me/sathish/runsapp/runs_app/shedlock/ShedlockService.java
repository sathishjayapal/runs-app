package me.sathish.runsapp.runs_app.shedlock;

import java.util.List;


public interface ShedlockService {

    List<ShedlockDTO> findAll();

    ShedlockDTO get(Long name);

    Long create(ShedlockDTO shedlockDTO);

    void update(Long name, ShedlockDTO shedlockDTO);

    void delete(Long name);

}
