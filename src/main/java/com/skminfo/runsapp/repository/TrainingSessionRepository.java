package com.skminfo.runsapp.repository;

import com.skminfo.runsapp.model.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    
    List<TrainingSession> findByPlannedDateBetweenOrderByPlannedDate(LocalDateTime start, LocalDateTime end);
    
    List<TrainingSession> findByCompletedOrderByPlannedDateDesc(Boolean completed);
}
