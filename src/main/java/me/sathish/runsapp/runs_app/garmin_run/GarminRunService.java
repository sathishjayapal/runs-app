package me.sathish.runsapp.runs_app.garmin_run;

import java.util.List;


public interface GarminRunService {

    List<GarminRunDTO> findAll();

    GarminRunDTO get(Long id);

    Long create(GarminRunDTO garminRunDTO);

    void update(Long id, GarminRunDTO garminRunDTO);

    void delete(Long id);

}
