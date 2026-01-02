package me.sathish.runsapp.runs_app.strava_run;

import org.springframework.data.jpa.repository.JpaRepository;


public interface StravaRunRepository extends JpaRepository<StravaRun, Long> {

    StravaRun findFirstByCreatedById(Long id);

}
