package com.skminfo.runsapp.service;

import com.skminfo.runsapp.model.BodyState;
import com.skminfo.runsapp.repository.BodyStateRepository;
import com.skminfo.runsapp.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BodyStateServiceTest {
    
    @Autowired
    private BodyStateService bodyStateService;
    
    @Autowired
    private BodyStateRepository bodyStateRepository;
    
    @Autowired
    private TrainingSessionRepository trainingSessionRepository;
    
    @BeforeEach
    public void setup() {
        trainingSessionRepository.deleteAll();
        bodyStateRepository.deleteAll();
    }
    
    @Test
    public void testLogBodyState() {
        BodyState bodyState = bodyStateService.logBodyState(3, 7, 4, "Feeling good");
        
        assertNotNull(bodyState);
        assertNotNull(bodyState.getId());
        assertEquals(3, bodyState.getPainLevel());
        assertEquals(7, bodyState.getSleepQuality());
        assertEquals(4, bodyState.getStressLevel());
        assertEquals("Feeling good", bodyState.getNotes());
    }
    
    @Test
    public void testGetLatestBodyState() {
        bodyStateService.logBodyState(2, 8, 3, "First log");
        bodyStateService.logBodyState(4, 6, 5, "Second log");
        
        Optional<BodyState> latest = bodyStateService.getLatestBodyState();
        
        assertTrue(latest.isPresent());
        assertEquals("Second log", latest.get().getNotes());
    }
}
