package com.runs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents health metrics for monitoring runner's condition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetrics {
    private Integer heartRate;
    private Double sleepHours;
    private Integer stressLevel; // 1-10 scale
    private Boolean hasInjury;
    private Boolean feelingFatigued;
    private Integer recoveryDays;
    private String additionalNotes;

    /**
     * Checks if there are any red flags in the health metrics
     * @return true if there are concerning health indicators
     */
    public boolean hasRedFlags() {
        // Check for concerning health indicators
        if (hasInjury != null && hasInjury) {
            return true;
        }
        if (heartRate != null && heartRate > 100) { // Elevated resting heart rate
            return true;
        }
        if (sleepHours != null && sleepHours < 6.0) { // Insufficient sleep
            return true;
        }
        if (stressLevel != null && stressLevel > 7) { // High stress
            return true;
        }
        if (feelingFatigued != null && feelingFatigued) {
            return true;
        }
        if (recoveryDays != null && recoveryDays < 1) { // Insufficient recovery
            return true;
        }
        return false;
    }

    /**
     * Gets a description of the red flags
     * @return description of health concerns
     */
    public String getRedFlagsDescription() {
        StringBuilder desc = new StringBuilder();
        if (hasInjury != null && hasInjury) {
            desc.append("Active injury; ");
        }
        if (heartRate != null && heartRate > 100) {
            desc.append("Elevated resting heart rate (").append(heartRate).append(" bpm); ");
        }
        if (sleepHours != null && sleepHours < 6.0) {
            desc.append("Insufficient sleep (").append(sleepHours).append(" hours); ");
        }
        if (stressLevel != null && stressLevel > 7) {
            desc.append("High stress level (").append(stressLevel).append("/10); ");
        }
        if (feelingFatigued != null && feelingFatigued) {
            desc.append("Feeling fatigued; ");
        }
        if (recoveryDays != null && recoveryDays < 1) {
            desc.append("Insufficient recovery time; ");
        }
        return desc.length() > 0 ? desc.toString() : "No red flags detected";
    }
}
