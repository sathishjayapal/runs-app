package me.sathish.runs_app.garmin_fit_import;

import lombok.Data;


@Data
public class FitActivityData {
    
    private String activityId;
    private String activityDate;
    private String activityType;
    private String activityName;
    private Double distanceMiles;
    private Integer elapsedTimeSeconds;
    private Integer maxHeartRate;
    private Integer avgHeartRate;
    private Integer calories;
    
    public String getFormattedElapsedTime() {
        if (elapsedTimeSeconds == null) {
            return "00:00:00";
        }
        
        int hours = elapsedTimeSeconds / 3600;
        int minutes = (elapsedTimeSeconds % 3600) / 60;
        int seconds = elapsedTimeSeconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
