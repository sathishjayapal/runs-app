package com.skminfo.runsapp.controller;

import com.skminfo.runsapp.dto.TrainingSessionRequest;
import com.skminfo.runsapp.model.TrainingSession;
import com.skminfo.runsapp.service.TrainingAdvisorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/training")
public class TrainingController {
    
    @Autowired
    private TrainingAdvisorService trainingAdvisorService;
    
    @PostMapping("/recommend")
    public ResponseEntity<TrainingSession> getRecommendation(@RequestBody TrainingSessionRequest request) {
        TrainingSession session = trainingAdvisorService.recommendSession(
            request.getPlannedType(),
            request.getPlannedDistance(),
            request.getPlannedDate()
        );
        return ResponseEntity.ok(session);
    }
    
    @GetMapping("/sessions")
    public ResponseEntity<List<TrainingSession>> getAllSessions() {
        List<TrainingSession> sessions = trainingAdvisorService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/sessions/{id}")
    public ResponseEntity<TrainingSession> getSession(@PathVariable Long id) {
        Optional<TrainingSession> session = trainingAdvisorService.getSessionById(id);
        return session.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/sessions/{id}/complete")
    public ResponseEntity<TrainingSession> markSessionCompleted(@PathVariable Long id) {
        try {
            TrainingSession session = trainingAdvisorService.markSessionCompleted(id);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
