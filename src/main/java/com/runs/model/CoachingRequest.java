package com.runs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for coaching advice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingRequest {
    private String question;
    private HealthMetrics healthMetrics;
    private TrainingContext trainingContext;
}
