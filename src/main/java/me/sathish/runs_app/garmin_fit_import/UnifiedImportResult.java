package me.sathish.runs_app.garmin_fit_import;

import lombok.Data;

@Data
public class UnifiedImportResult {
    
    private ImportResult fitResult;
    private ImportResult csvResult;
    private String fitError;
    private String csvError;
    private long totalDurationMs;
    
    public int getTotalSuccessCount() {
        int total = 0;
        if (fitResult != null) {
            total += fitResult.getSuccessCount();
        }
        if (csvResult != null) {
            total += csvResult.getSuccessCount();
        }
        return total;
    }
    
    public int getTotalSkippedCount() {
        int total = 0;
        if (fitResult != null) {
            total += fitResult.getSkippedCount();
        }
        if (csvResult != null) {
            total += csvResult.getSkippedCount();
        }
        return total;
    }
    
    public int getTotalFailedCount() {
        int total = 0;
        if (fitResult != null) {
            total += fitResult.getFailedCount();
        }
        if (csvResult != null) {
            total += csvResult.getFailedCount();
        }
        return total;
    }
    
    public int getTotalProcessed() {
        return getTotalSuccessCount() + getTotalSkippedCount() + getTotalFailedCount();
    }
    
    public boolean hasErrors() {
        return fitError != null || csvError != null;
    }
    
    public boolean hasFailed() {
        return getTotalFailedCount() > 0;
    }
    
    public boolean isFullySuccessful() {
        return !hasErrors() && !hasFailed() && getTotalProcessed() > 0;
    }
}
