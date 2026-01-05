package me.sathish.runsapp.runs_app.garmin_run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface GarminRunRepository extends JpaRepository<GarminRun, Long> {

    Page<GarminRun> findAllById(Long id, Pageable pageable);

    GarminRun findFirstByCreatedById(Long id);

    GarminRun findFirstByUpdateById(Long id);

}
