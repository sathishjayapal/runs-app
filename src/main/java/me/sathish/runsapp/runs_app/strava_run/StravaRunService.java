package me.sathish.runsapp.runs_app.strava_run;

import java.util.List;


public interface StravaRunService {

    List<StravaRunDTO> findAll();

    StravaRunDTO get(Long runNumber);

    Long create(StravaRunDTO stravaRunDTO);

    void update(Long runNumber, StravaRunDTO stravaRunDTO);

    void delete(Long runNumber);

}
