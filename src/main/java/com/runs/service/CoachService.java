package com.runs.service;

import com.runs.model.CoachingRequest;
import com.runs.model.CoachingResponse;
import com.runs.model.HealthMetrics;
import com.runs.model.TrainingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Coaching service that monitors health metrics and provides training advice
 * Refuses to push the athlete if red flags are detected
 */
@Service
@Slf4j
public class CoachService {

    /**
     * Provides coaching advice based on the request and health metrics
     * Will refuse to push the athlete if red flags are detected
     */
    public CoachingResponse getCoachingAdvice(CoachingRequest request) {
        log.info("Processing coaching request: {}", request.getQuestion());

        HealthMetrics metrics = request.getHealthMetrics();
        boolean hasRedFlags = metrics != null && metrics.hasRedFlags();

        // Build the response
        CoachingResponse.CoachingResponseBuilder responseBuilder = CoachingResponse.builder();

        if (hasRedFlags) {
            String redFlagsDesc = metrics.getRedFlagsDescription();
            log.warn("Red flags detected: {}", redFlagsDesc);
            
            responseBuilder
                .allowWorkout(false)
                .redFlags(redFlagsDesc)
                .reason("Your body is showing warning signs. Rest and recovery are essential.");
            
            // Get recovery advice
            String advice = getRecoveryAdvice(request, redFlagsDesc);
            responseBuilder.advice(advice);
            
        } else {
            log.info("No red flags detected, proceeding with normal coaching");
            responseBuilder
                .allowWorkout(true)
                .redFlags("No red flags detected")
                .reason("Your health metrics look good for training.");
            
            // Get training advice
            String advice = getTrainingAdvice(request);
            responseBuilder.advice(advice);
        }

        return responseBuilder.build();
    }

    /**
     * Gets recovery advice when red flags are present
     */
    private String getRecoveryAdvice(CoachingRequest request, String redFlags) {
        StringBuilder advice = new StringBuilder();
        
        advice.append("I understand you're eager to train for Boston, but I need to put the brakes on here. ");
        advice.append("Your body is showing some concerning signs that we can't ignore:\n\n");
        advice.append(redFlags).append("\n\n");
        
        HealthMetrics metrics = request.getHealthMetrics();
        
        advice.append("Here's what I recommend:\n\n");
        
        if (metrics.getHasInjury() != null && metrics.getHasInjury()) {
            advice.append("• INJURY: This is non-negotiable. Take at least 2-3 days off and consider seeing a sports medicine professional. ");
            advice.append("Running through an injury now could sideline you for weeks or even end your Boston dream.\n\n");
        }
        
        if (metrics.getHeartRate() != null && metrics.getHeartRate() > 100) {
            advice.append("• ELEVATED HEART RATE: Your resting heart rate of ").append(metrics.getHeartRate());
            advice.append(" bpm suggests your body is under stress. This is often a sign of overtraining or illness. ");
            advice.append("Take a complete rest day or do very easy cross-training only.\n\n");
        }
        
        if (metrics.getSleepHours() != null && metrics.getSleepHours() < 6.0) {
            advice.append("• SLEEP DEFICIT: You're only getting ").append(metrics.getSleepHours());
            advice.append(" hours of sleep. Recovery happens during sleep. Prioritize getting 7-9 hours tonight. ");
            advice.append("Consider taking a nap if possible.\n\n");
        }
        
        if (metrics.getStressLevel() != null && metrics.getStressLevel() > 7) {
            advice.append("• HIGH STRESS: A stress level of ").append(metrics.getStressLevel());
            advice.append("/10 means your cortisol is likely elevated. Adding training stress on top of life stress is a recipe for burnout or injury. ");
            advice.append("Do some gentle yoga, meditation, or just rest.\n\n");
        }
        
        if (metrics.getFeelingFatigued() != null && metrics.getFeelingFatigued()) {
            advice.append("• FATIGUE: Listen to your body when it's telling you it's tired. ");
            advice.append("Fatigue is a warning sign. Rest now so you can train hard later.\n\n");
        }
        
        if (metrics.getRecoveryDays() != null && metrics.getRecoveryDays() < 1) {
            advice.append("• INSUFFICIENT RECOVERY: You need at least one day between hard efforts. ");
            advice.append("Muscles grow and adapt during rest, not during the workout itself.\n\n");
        }
        
        advice.append("Remember: The Boston Marathon is a marathon, not a sprint (pun intended). ");
        advice.append("Missing one workout to prevent an injury that could cost you weeks is always the right choice. ");
        advice.append("Trust the process, respect your body's signals, and you'll get to that starting line in Hopkinton healthy and strong.");
        
        return advice.toString();
    }

    /**
     * Gets training advice when health metrics are good
     */
    private String getTrainingAdvice(CoachingRequest request) {
        StringBuilder advice = new StringBuilder();
        TrainingContext context = request.getTrainingContext();
        
        advice.append("Great news! Your health metrics look solid, which means you're ready to train. ");
        
        if (context != null) {
            if (context.getWeeksUntilRace() != null) {
                int weeks = context.getWeeksUntilRace();
                advice.append("\n\nWith ").append(weeks).append(" weeks until race day, ");
                
                if (weeks > 12) {
                    advice.append("you're in the base-building phase. Focus on gradually increasing your weekly mileage. ");
                    advice.append("Keep most runs at an easy, conversational pace. The goal is building aerobic endurance without injury.");
                } else if (weeks > 6) {
                    advice.append("you're in the build phase. This is where you add quality workouts - tempo runs, intervals, and hill work. ");
                    advice.append("But remember: hard days hard, easy days easy. Don't make every run a race.");
                } else if (weeks > 2) {
                    advice.append("you're entering the peak phase. Your longest runs are behind you now. ");
                    advice.append("Focus on race-pace work and maintaining fitness while staying fresh.");
                } else {
                    advice.append("you're in taper mode! Reduce your volume but keep some intensity. ");
                    advice.append("Trust your training. Your work is done - now it's about arriving at the start line healthy and rested.");
                }
            }
            
            if (context.getCurrentWeeklyMileage() != null) {
                advice.append("\n\nAt ").append(context.getCurrentWeeklyMileage()).append(" miles per week, ");
                if (context.getCurrentWeeklyMileage() < 40) {
                    advice.append("you have room to grow. Increase by no more than 10% per week.");
                } else if (context.getCurrentWeeklyMileage() < 60) {
                    advice.append("you're in a good range for Boston training. Maintain consistency.");
                } else {
                    advice.append("you're putting in serious mileage. Make sure you're getting enough recovery.");
                }
            }
            
            if (context.getCurrentPhase() != null) {
                String phase = context.getCurrentPhase().toLowerCase();
                advice.append("\n\nFor the ").append(phase).append(" phase, ");
                switch (phase) {
                    case "base":
                        advice.append("prioritize easy miles, build your aerobic base, and work on form.");
                        break;
                    case "build":
                        advice.append("add structured workouts: tempo runs on Tuesdays, long runs on weekends, recovery runs in between.");
                        break;
                    case "peak":
                        advice.append("maintain your fitness while avoiding overtraining. Quality over quantity now.");
                        break;
                    case "taper":
                        advice.append("reduce volume by 40-50% but keep some speed work to stay sharp.");
                        break;
                    default:
                        advice.append("stay consistent with your training plan.");
                }
            }
        }
        
        advice.append("\n\nRemember the keys to Boston success:\n");
        advice.append("• Respect the hills - train on them if you can\n");
        advice.append("• Practice your race nutrition during long runs\n");
        advice.append("• Don't ignore the small aches - they can become big injuries\n");
        advice.append("• Stay consistent - it's better to be 90% trained and 100% healthy than vice versa\n\n");
        
        advice.append("You've got this! Keep listening to your body, and let me know if anything changes.");
        
        return advice.toString();
    }
}
