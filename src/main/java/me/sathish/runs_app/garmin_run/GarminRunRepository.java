package me.sathish.runs_app.garmin_run;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface GarminRunRepository extends JpaRepository<GarminRun, Long> {

    Page<GarminRun> findAllById(Long id, Pageable pageable);

    GarminRun findFirstByCreatedById(Long id);

    GarminRun findFirstByUpdateById(Long id);

    boolean existsByActivityId(String activityId);

    GarminRun findByActivityId(String activityId);

    long countByActivityIdIn(Collection<String> activityIds);

    List<GarminRun> findByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime since);

}
