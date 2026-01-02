package com.runs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Training context information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingContext {
    private String currentPhase; // base, build, peak, taper
    private Integer weeksUntilRace;
    private Double currentWeeklyMileage;
    private String lastWorkoutType;
    private Boolean trainingForBoston;
}
