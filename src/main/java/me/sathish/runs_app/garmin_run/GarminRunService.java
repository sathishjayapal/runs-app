package me.sathish.runs_app.garmin_run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface GarminRunService {

    Page<GarminRunDTO> findAll(String filter, Pageable pageable);

    GarminRunDTO get(Long id);

    Long create(GarminRunDTO garminRunDTO);

    void update(Long id, GarminRunDTO garminRunDTO);

    void delete(Long id);

}
