package com.skminfo.runsapp.service;

import com.skminfo.runsapp.model.BodyState;
import com.skminfo.runsapp.model.TrainingSession;
import com.skminfo.runsapp.model.TrainingSession.SessionType;
import com.skminfo.runsapp.repository.BodyStateRepository;
import com.skminfo.runsapp.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TrainingAdvisorServiceTest {
    
    @Autowired
    private TrainingAdvisorService trainingAdvisorService;
    
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
    public void testRecommendSession_ExcellentBodyState() {
        BodyState bodyState = new BodyState(1, 9, 2);
        bodyStateRepository.save(bodyState);
        
        TrainingSession session = trainingAdvisorService.recommendSession(
            SessionType.LONG_RUN, 
            16.0, 
            LocalDateTime.now().plusDays(1)
        );
        
        assertNotNull(session);
        assertEquals(SessionType.LONG_RUN, session.getRecommendedType());
        assertEquals(16.0, session.getRecommendedDistance());
        assertTrue(session.getRecommendation().contains("excellent"));
    }
    
    @Test
    public void testRecommendSession_ModerateBodyState_DowngradesLongRun() {
        BodyState bodyState = new BodyState(4, 6, 5);
        bodyStateRepository.save(bodyState);
        
        TrainingSession session = trainingAdvisorService.recommendSession(
            SessionType.LONG_RUN, 
            16.0, 
            LocalDateTime.now().plusDays(1)
        );
        
        assertNotNull(session);
        assertEquals(SessionType.EASY_RUN, session.getRecommendedType());
        assertEquals(10.0, session.getRecommendedDistance());
        assertTrue(session.getRecommendation().contains("Downgrading"));
    }
    
    @Test
    public void testRecommendSession_PoorBodyState_RecommendsCrossTraining() {
        BodyState bodyState = new BodyState(8, 4, 9);
        bodyStateRepository.save(bodyState);
        
        TrainingSession session = trainingAdvisorService.recommendSession(
            SessionType.LONG_RUN, 
            16.0, 
            LocalDateTime.now().plusDays(1)
        );
        
        assertNotNull(session);
        assertEquals(SessionType.CROSS_TRAINING, session.getRecommendedType());
        assertEquals(0.0, session.getRecommendedDistance());
        assertTrue(session.getRecommendation().contains("cross-training"));
    }
    
    @Test
    public void testRecommendSession_PoorSleep_RecommendedRecovery() {
        BodyState bodyState = new BodyState(4, 2, 5);
        bodyStateRepository.save(bodyState);
        
        TrainingSession session = trainingAdvisorService.recommendSession(
            SessionType.LONG_RUN, 
            16.0, 
            LocalDateTime.now().plusDays(1)
        );
        
        assertNotNull(session);
        assertEquals(SessionType.RECOVERY_RUN, session.getRecommendedType());
        assertTrue(session.getRecommendedDistance() <= 3.0);
        assertTrue(session.getRecommendation().contains("recovery"));
    }
    
    @Test
    public void testMarkSessionCompleted() {
        BodyState bodyState = new BodyState(2, 8, 3);
        bodyStateRepository.save(bodyState);
        
        TrainingSession session = trainingAdvisorService.recommendSession(
            SessionType.EASY_RUN, 
            5.0, 
            LocalDateTime.now().plusDays(1)
        );
        
        Long sessionId = session.getId();
        TrainingSession completed = trainingAdvisorService.markSessionCompleted(sessionId);
        
        assertTrue(completed.getCompleted());
    }
}
