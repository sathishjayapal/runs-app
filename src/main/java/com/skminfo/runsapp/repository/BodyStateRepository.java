package com.skminfo.runsapp.repository;

import com.skminfo.runsapp.model.BodyState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyStateRepository extends JpaRepository<BodyState, Long> {
    
    List<BodyState> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    Optional<BodyState> findFirstByOrderByTimestampDesc();
}
