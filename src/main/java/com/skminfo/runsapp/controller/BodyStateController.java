package com.skminfo.runsapp.controller;

import com.skminfo.runsapp.dto.BodyStateRequest;
import com.skminfo.runsapp.model.BodyState;
import com.skminfo.runsapp.service.BodyStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/body-state")
public class BodyStateController {
    
    @Autowired
    private BodyStateService bodyStateService;
    
    @PostMapping
    public ResponseEntity<BodyState> logBodyState(@RequestBody BodyStateRequest request) {
        BodyState bodyState = bodyStateService.logBodyState(
            request.getPainLevel(),
            request.getSleepQuality(),
            request.getStressLevel(),
            request.getNotes()
        );
        return ResponseEntity.ok(bodyState);
    }
    
    @GetMapping("/latest")
    public ResponseEntity<BodyState> getLatestBodyState() {
        Optional<BodyState> bodyState = bodyStateService.getLatestBodyState();
        return bodyState.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<BodyState>> getAllBodyStates() {
        List<BodyState> bodyStates = bodyStateService.getAllBodyStates();
        return ResponseEntity.ok(bodyStates);
    }
}
