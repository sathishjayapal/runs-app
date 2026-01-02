package com.skminfo.runsapp.service;

import com.skminfo.runsapp.model.BodyState;
import com.skminfo.runsapp.model.TrainingSession;
import com.skminfo.runsapp.model.TrainingSession.SessionType;
import com.skminfo.runsapp.repository.BodyStateRepository;
import com.skminfo.runsapp.repository.TrainingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingAdvisorService {
    
    private static final double MODERATE_LONG_RUN_REDUCTION_FACTOR = 0.625;
    private static final double MODERATE_INTENSITY_REDUCTION_FACTOR = 0.8;
    private static final double POOR_DISTANCE_REDUCTION_FACTOR = 0.5;
    private static final double MAX_RECOVERY_DISTANCE_MILES = 3.0;
    private static final int EXCELLENT_SCORE_THRESHOLD = 7;
    private static final int MODERATE_SCORE_THRESHOLD = 5;
    private static final int HIGH_PAIN_THRESHOLD = 7;
    private static final int HIGH_STRESS_THRESHOLD = 8;
    
    @Autowired
    private BodyStateRepository bodyStateRepository;
    
    @Autowired
    private TrainingSessionRepository trainingSessionRepository;
    
    public TrainingSession recommendSession(SessionType plannedType, Double plannedDistance, LocalDateTime plannedDate) {
        Optional<BodyState> latestBodyStateOpt = bodyStateRepository.findFirstByOrderByTimestampDesc();
        
        if (latestBodyStateOpt.isEmpty()) {
            return createSession(plannedType, plannedDistance, plannedDate, null, 
                "No body state logged. Please log your current pain, sleep, and stress levels.");
        }
        
        BodyState bodyState = latestBodyStateOpt.get();
        
        int overallScore = calculateOverallScore(bodyState);
        
        TrainingSession session = new TrainingSession();
        session.setPlannedDate(plannedDate);
        session.setPlannedType(plannedType);
        session.setPlannedDistance(plannedDistance);
        session.setBodyState(bodyState);
        session.setCompleted(false);
        
        if (overallScore >= EXCELLENT_SCORE_THRESHOLD) {
            session.setRecommendedType(plannedType);
            session.setRecommendedDistance(plannedDistance);
            session.setRecommendation(String.format(
                "Body state is excellent (Score: %d/10). Proceed with planned %s of %.1f miles.", 
                overallScore, plannedType, plannedDistance));
        } else if (overallScore >= MODERATE_SCORE_THRESHOLD) {
            adjustSessionForModerateState(session, bodyState, overallScore);
        } else {
            adjustSessionForPoorState(session, bodyState, overallScore);
        }
        
        return trainingSessionRepository.save(session);
    }
    
    private void adjustSessionForModerateState(TrainingSession session, BodyState bodyState, int score) {
        SessionType plannedType = session.getPlannedType();
        Double plannedDistance = session.getPlannedDistance();
        
        if (plannedType == SessionType.LONG_RUN) {
            double reducedDistance = plannedDistance * MODERATE_LONG_RUN_REDUCTION_FACTOR;
            session.setRecommendedType(SessionType.EASY_RUN);
            session.setRecommendedDistance(reducedDistance);
            session.setRecommendation(String.format(
                "Moderate body state (Score: %d/10). Pain: %d, Sleep: %d, Stress: %d. " +
                "Downgrading from %s (%.1f mi) to %s (%.1f mi) to protect recovery.",
                score, bodyState.getPainLevel(), bodyState.getSleepQuality(), bodyState.getStressLevel(),
                plannedType, plannedDistance, SessionType.EASY_RUN, reducedDistance));
        } else if (plannedType == SessionType.INTERVAL_RUN || plannedType == SessionType.TEMPO_RUN) {
            session.setRecommendedType(SessionType.EASY_RUN);
            session.setRecommendedDistance(plannedDistance * MODERATE_INTENSITY_REDUCTION_FACTOR);
            session.setRecommendation(String.format(
                "Moderate body state (Score: %d/10). Converting intensity session to easy run.",
                score));
        } else {
            session.setRecommendedType(plannedType);
            session.setRecommendedDistance(plannedDistance);
            session.setRecommendation(String.format(
                "Moderate body state (Score: %d/10). Proceed with caution and monitor how you feel.",
                score));
        }
    }
    
    private void adjustSessionForPoorState(TrainingSession session, BodyState bodyState, int score) {
        if (bodyState.getPainLevel() >= HIGH_PAIN_THRESHOLD || bodyState.getStressLevel() >= HIGH_STRESS_THRESHOLD) {
            session.setRecommendedType(SessionType.CROSS_TRAINING);
            session.setRecommendedDistance(0.0);
            session.setRecommendation(String.format(
                "Poor body state (Score: %d/10). High pain (%d) or stress (%d) detected. " +
                "Recommending cross-training (swimming, cycling, yoga) instead of running to prevent injury.",
                score, bodyState.getPainLevel(), bodyState.getStressLevel()));
        } else {
            session.setRecommendedType(SessionType.RECOVERY_RUN);
            session.setRecommendedDistance(Math.min(session.getPlannedDistance() * POOR_DISTANCE_REDUCTION_FACTOR, MAX_RECOVERY_DISTANCE_MILES));
            session.setRecommendation(String.format(
                "Poor body state (Score: %d/10). Sleep quality is low (%d). " +
                "Recommending short recovery run (max %.1f miles) or rest day.",
                score, bodyState.getSleepQuality(), MAX_RECOVERY_DISTANCE_MILES));
        }
    }
    
    private int calculateOverallScore(BodyState bodyState) {
        int painScore = 10 - bodyState.getPainLevel();
        int sleepScore = bodyState.getSleepQuality();
        int stressScore = 10 - bodyState.getStressLevel();
        
        return (painScore + sleepScore + stressScore) / 3;
    }
    
    private TrainingSession createSession(SessionType plannedType, Double plannedDistance, 
                                         LocalDateTime plannedDate, BodyState bodyState, String recommendation) {
        TrainingSession session = new TrainingSession();
        session.setPlannedDate(plannedDate);
        session.setPlannedType(plannedType);
        session.setPlannedDistance(plannedDistance);
        session.setBodyState(bodyState);
        session.setRecommendation(recommendation);
        session.setCompleted(false);
        return session;
    }
    
    public List<TrainingSession> getAllSessions() {
        return trainingSessionRepository.findAll();
    }
    
    public Optional<TrainingSession> getSessionById(Long id) {
        return trainingSessionRepository.findById(id);
    }
    
    public TrainingSession markSessionCompleted(Long id) {
        Optional<TrainingSession> sessionOpt = trainingSessionRepository.findById(id);
        if (sessionOpt.isPresent()) {
            TrainingSession session = sessionOpt.get();
            session.setCompleted(true);
            return trainingSessionRepository.save(session);
        }
        throw new RuntimeException("Session not found with id: " + id);
    }
}
