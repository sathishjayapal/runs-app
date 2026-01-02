package com.runs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from the AI coach
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingResponse {
    private String advice;
    private boolean allowWorkout;
    private String reason;
    private String redFlags;
}
