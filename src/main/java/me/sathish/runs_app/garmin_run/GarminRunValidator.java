package me.sathish.runs_app.garmin_run;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


@Component
public class GarminRunValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return GarminRunDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        GarminRunDTO dto = (GarminRunDTO) target;

        // Validate distance (0.01 to 200 miles)
        if (dto.getDistance() != null && !dto.getDistance().isEmpty()) {
            try {
                double distance = Double.parseDouble(dto.getDistance());
                if (distance < 0.01 || distance > 200) {
                    errors.rejectValue("distance", "invalid.range", 
                        "Distance must be between 0.01 and 200 miles");
                }
            } catch (NumberFormatException e) {
                errors.rejectValue("distance", "invalid.format", 
                    "Distance must be a valid number");
            }
        }

        // Validate max heart rate (40 to 220 bpm)
        if (dto.getMaxHeartRate() != null && !dto.getMaxHeartRate().isEmpty()) {
            try {
                int heartRate = Integer.parseInt(dto.getMaxHeartRate());
                if (heartRate < 40 || heartRate > 220) {
                    errors.rejectValue("maxHeartRate", "invalid.range", 
                        "Max heart rate must be between 40 and 220 bpm");
                }
            } catch (NumberFormatException e) {
                errors.rejectValue("maxHeartRate", "invalid.format", 
                    "Max heart rate must be a valid integer");
            }
        }

        // Validate calories (1 to 10000)
        if (dto.getCalories() != null && !dto.getCalories().isEmpty()) {
            try {
                int calories = Integer.parseInt(dto.getCalories());
                if (calories < 1 || calories > 10000) {
                    errors.rejectValue("calories", "invalid.range", 
                        "Calories must be between 1 and 10000");
                }
            } catch (NumberFormatException e) {
                errors.rejectValue("calories", "invalid.format", 
                    "Calories must be a valid integer");
            }
        }
    }
}
