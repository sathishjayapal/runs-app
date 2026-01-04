package me.sathish.runsapp.runs_app.strava_run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface StravaRunService {

    Page<StravaRunDTO> findAll(String filter, Pageable pageable);

    StravaRunDTO get(Long runNumber);

    Long create(StravaRunDTO stravaRunDTO);

    void update(Long runNumber, StravaRunDTO stravaRunDTO);

    void delete(Long runNumber);

}
