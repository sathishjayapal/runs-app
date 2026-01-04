package me.sathish.runsapp.runs_app.shedlock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ShedlockRepository extends JpaRepository<Shedlock, Long> {

    Page<Shedlock> findAllByName(Long name, Pageable pageable);

}
