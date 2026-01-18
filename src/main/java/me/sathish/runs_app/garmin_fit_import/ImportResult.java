package me.sathish.runs_app.garmin_fit_import;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
public class ImportResult {
    
    private List<String> successFiles = new ArrayList<>();
    private List<String> skippedFiles = new ArrayList<>();
    private Map<String, String> failedFiles = new HashMap<>();
    
    public void addSuccess(String fileName) {
        successFiles.add(fileName);
    }
    
    public void addSkipped(String fileName) {
        skippedFiles.add(fileName);
    }
    
    public void addFailed(String fileName, String error) {
        failedFiles.put(fileName, error);
    }
    
    public int getSuccessCount() {
        return successFiles.size();
    }
    
    public int getSkippedCount() {
        return skippedFiles.size();
    }
    
    public int getFailedCount() {
        return failedFiles.size();
    }
    
    public int getTotalProcessed() {
        return getSuccessCount() + getSkippedCount() + getFailedCount();
    }
}
