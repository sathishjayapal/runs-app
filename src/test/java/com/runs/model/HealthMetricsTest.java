package com.runs.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HealthMetricsTest {

    @Test
    void testNoRedFlags() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(8.0)
            .stressLevel(3)
            .hasInjury(false)
            .feelingFatigued(false)
            .recoveryDays(2)
            .build();

        assertFalse(metrics.hasRedFlags());
        assertEquals("No red flags detected", metrics.getRedFlagsDescription());
    }

    @Test
    void testElevatedHeartRate() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(105)
            .sleepHours(8.0)
            .stressLevel(3)
            .hasInjury(false)
            .feelingFatigued(false)
            .recoveryDays(2)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("Elevated resting heart rate"));
    }

    @Test
    void testInsufficientSleep() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(5.0)
            .stressLevel(3)
            .hasInjury(false)
            .feelingFatigued(false)
            .recoveryDays(2)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("Insufficient sleep"));
    }

    @Test
    void testHighStress() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(8.0)
            .stressLevel(9)
            .hasInjury(false)
            .feelingFatigued(false)
            .recoveryDays(2)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("High stress level"));
    }

    @Test
    void testInjury() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(8.0)
            .stressLevel(3)
            .hasInjury(true)
            .feelingFatigued(false)
            .recoveryDays(2)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("Active injury"));
    }

    @Test
    void testFatigue() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(8.0)
            .stressLevel(3)
            .hasInjury(false)
            .feelingFatigued(true)
            .recoveryDays(2)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("Feeling fatigued"));
    }

    @Test
    void testInsufficientRecovery() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(65)
            .sleepHours(8.0)
            .stressLevel(3)
            .hasInjury(false)
            .feelingFatigued(false)
            .recoveryDays(0)
            .build();

        assertTrue(metrics.hasRedFlags());
        assertTrue(metrics.getRedFlagsDescription().contains("Insufficient recovery"));
    }

    @Test
    void testMultipleRedFlags() {
        HealthMetrics metrics = HealthMetrics.builder()
            .heartRate(105)
            .sleepHours(5.0)
            .stressLevel(9)
            .hasInjury(true)
            .feelingFatigued(true)
            .recoveryDays(0)
            .build();

        assertTrue(metrics.hasRedFlags());
        String description = metrics.getRedFlagsDescription();
        assertTrue(description.contains("Active injury"));
        assertTrue(description.contains("Elevated resting heart rate"));
        assertTrue(description.contains("Insufficient sleep"));
        assertTrue(description.contains("High stress level"));
        assertTrue(description.contains("Feeling fatigued"));
        assertTrue(description.contains("Insufficient recovery"));
    }
}
