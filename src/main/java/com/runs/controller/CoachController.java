package com.runs.controller;

import com.runs.model.CoachingRequest;
import com.runs.model.CoachingResponse;
import com.runs.service.CoachService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for the AI running coach
 */
@RestController
@RequestMapping("/api/coach")
@Slf4j
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    /**
     * Get coaching advice
     */
    @PostMapping("/advice")
    public ResponseEntity<CoachingResponse> getAdvice(@RequestBody CoachingRequest request) {
        log.info("Received coaching request");
        CoachingResponse response = coachService.getCoachingAdvice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Coach is ready to help!");
    }
}
