package me.sathish.runsapp.runs_app.strava_run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StravaRunRepository extends JpaRepository<StravaRun, Long> {

    Page<StravaRun> findAllByRunNumber(Long runNumber, Pageable pageable);

    StravaRun findFirstByCreatedById(Long id);

    StravaRun findFirstByUpdatedById(Long id);

}
