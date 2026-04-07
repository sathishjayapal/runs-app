package me.sathish.runs_app.garmin_fit_import;

import lombok.Data;

@Data
public class UnifiedImportEvent {
    private String eventType; // "GARMIN_UNIFIED_IMPORT"
    private int totalImported;
    private int totalSkipped;
    private int totalFailed;
    private long durationMs;
    private int fitSuccess;
    private int fitSkipped;
    private int fitFailed;
    private int csvSuccess;
    private int csvSkipped;
    private int csvFailed;
    private String status; // "SUCCESS", "PARTIAL_SUCCESS", "NO_NEW_DATA"
}
