package me.sathish.runs_app.garmin_fit_import;

import com.garmin.fit.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Component
@Slf4j
public class GarminFitFileParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public FitActivityData parse(String filePath) throws Exception {
        FitActivityData activityData = new FitActivityData();
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            Decode decode = new Decode();
            MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
            
            // Listen for Session messages (summary data)
            mesgBroadcaster.addListener(new SessionMesgListener() {
                @Override
                public void onMesg(SessionMesg sessionMesg) {
                    if (sessionMesg != null) {
                        // Activity type
                        if (sessionMesg.getSport() != null) {
                            activityData.setActivityType(mapSportType(sessionMesg.getSport()));
                        }
                        
                        // Distance (convert from meters to miles)
                        if (sessionMesg.getTotalDistance() != null) {
                            double meters = sessionMesg.getTotalDistance();
                            double miles = meters * 0.000621371; // meters to miles
                            activityData.setDistanceMiles(miles);
                        }
                        
                        // Elapsed time (in seconds)
                        if (sessionMesg.getTotalElapsedTime() != null) {
                            activityData.setElapsedTimeSeconds(sessionMesg.getTotalElapsedTime().intValue());
                        }
                        
                        // Calories
                        if (sessionMesg.getTotalCalories() != null) {
                            activityData.setCalories(sessionMesg.getTotalCalories());
                        }
                        
                        // Max heart rate
                        if (sessionMesg.getMaxHeartRate() != null) {
                            activityData.setMaxHeartRate(sessionMesg.getMaxHeartRate().intValue());
                        }
                        
                        // Average heart rate
                        if (sessionMesg.getAvgHeartRate() != null) {
                            activityData.setAvgHeartRate(sessionMesg.getAvgHeartRate().intValue());
                        }
                        
                        // Start time
                        if (sessionMesg.getStartTime() != null) {
                            DateTime startTime = sessionMesg.getStartTime();
                            Instant instant = Instant.ofEpochSecond(startTime.getTimestamp());
                            activityData.setActivityDate(instant.atZone(ZoneId.systemDefault()).format(ISO_FORMATTER));
                        }
                        
                        log.debug("Session data - Sport: {}, Distance: {} m, Calories: {}, HR: {}",
                                sessionMesg.getSport(), sessionMesg.getTotalDistance(),
                                sessionMesg.getTotalCalories(), sessionMesg.getMaxHeartRate());
                    }
                }
            });
            
            // Listen for Activity messages
            mesgBroadcaster.addListener(new ActivityMesgListener() {
                @Override
                public void onMesg(ActivityMesg activityMesg) {
                    if (activityMesg != null) {
                        // Timestamp for activity ID
                        if (activityMesg.getTimestamp() != null) {
                            DateTime timestamp = activityMesg.getTimestamp();
                            activityData.setActivityId(String.valueOf(timestamp.getTimestamp()));
                        }
                        
                        // Total timer time
                        if (activityMesg.getTotalTimerTime() != null) {
                            activityData.setElapsedTimeSeconds(activityMesg.getTotalTimerTime().intValue());
                        }
                        
                        log.debug("Activity data - Timestamp: {}, Type: {}",
                                activityMesg.getTimestamp(), activityMesg.getType());
                    }
                }
            });
            
            // Listen for FileId messages (file metadata)
            mesgBroadcaster.addListener(new FileIdMesgListener() {
                @Override
                public void onMesg(FileIdMesg fileIdMesg) {
                    if (fileIdMesg != null) {
                        if (fileIdMesg.getTimeCreated() != null) {
                            DateTime created = fileIdMesg.getTimeCreated();
                            Instant instant = Instant.ofEpochSecond(created.getTimestamp());
                            if (activityData.getActivityDate() == null) {
                                activityData.setActivityDate(instant.atZone(ZoneId.systemDefault()).format(ISO_FORMATTER));
                            }
                        }
                        log.debug("File ID - Type: {}, Manufacturer: {}",
                                fileIdMesg.getType(), fileIdMesg.getManufacturer());
                    }
                }
            });
            
            // Decode the FIT file
            if (!decode.checkFileIntegrity(inputStream)) {
                throw new IllegalArgumentException("FIT file integrity check failed: " + filePath);
            }
            
            // Reset stream and decode
            try (InputStream inputStream2 = new FileInputStream(filePath)) {
                decode.read(inputStream2, mesgBroadcaster);
            }
            
            log.info("Successfully parsed FIT file: {} - Distance: {} miles, Calories: {}, Duration: {} seconds, Max HR: {}",
                    filePath, 
                    activityData.getDistanceMiles() != null ? String.format("%.2f", activityData.getDistanceMiles()) : "N/A",
                    activityData.getCalories() != null ? activityData.getCalories() : "N/A",
                    activityData.getElapsedTimeSeconds() != null ? activityData.getElapsedTimeSeconds() : "N/A",
                    activityData.getMaxHeartRate() != null ? activityData.getMaxHeartRate() : "N/A");
        }
        
        // Set defaults if not parsed
        if (activityData.getActivityType() == null) {
            activityData.setActivityType("running");
        }
        if (activityData.getActivityName() == null) {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            activityData.setActivityName(fileName.replace(".fit", ""));
        }
        
        return activityData;
    }
    
    private String mapSportType(Sport sport) {
        if (sport == null) {
            return "running";
        }
        
        // Map Sport enum to activity type strings
        String sportName = sport.name().toLowerCase();
        
        if (sportName.contains("run")) {
            return "running";
        } else if (sportName.contains("training") || sportName.contains("strength")) {
            return "strength_training";
        } else if (sportName.contains("elliptical")) {
            return "elliptical";
        } else if (sportName.contains("cycling") || sportName.contains("biking")) {
            return "cycling";
        } else if (sportName.contains("walk") || sportName.contains("hik")) {
            return "walking";
        } else if (sportName.contains("swim")) {
            return "swimming";
        } else {
            return "running"; // default
        }
    }
}
