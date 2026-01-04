package me.sathish.runsapp.runs_app.garmin_run;

import org.springframework.data.jpa.repository.JpaRepository;


public interface GarminRunRepository extends JpaRepository<GarminRun, Long> {

    GarminRun findFirstByCreatedById(Long id);

}
