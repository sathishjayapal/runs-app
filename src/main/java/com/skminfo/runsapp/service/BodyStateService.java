package com.skminfo.runsapp.service;

import com.skminfo.runsapp.model.BodyState;
import com.skminfo.runsapp.repository.BodyStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BodyStateService {
    
    @Autowired
    private BodyStateRepository bodyStateRepository;
    
    public BodyState logBodyState(Integer painLevel, Integer sleepQuality, Integer stressLevel, String notes) {
        BodyState bodyState = new BodyState();
        bodyState.setTimestamp(LocalDateTime.now());
        bodyState.setPainLevel(painLevel);
        bodyState.setSleepQuality(sleepQuality);
        bodyState.setStressLevel(stressLevel);
        bodyState.setNotes(notes);
        
        return bodyStateRepository.save(bodyState);
    }
    
    public Optional<BodyState> getLatestBodyState() {
        return bodyStateRepository.findFirstByOrderByTimestampDesc();
    }
    
    public List<BodyState> getBodyStatesInRange(LocalDateTime start, LocalDateTime end) {
        return bodyStateRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
    
    public List<BodyState> getAllBodyStates() {
        return bodyStateRepository.findAll();
    }
}
