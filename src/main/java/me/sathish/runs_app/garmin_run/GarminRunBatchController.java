package me.sathish.runs_app.garmin_run;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/garminRuns")
@RequiredArgsConstructor
@Slf4j
public class GarminRunBatchController {

    private final GarminRunRepository garminRunRepository;
    private final GarminRunService garminRunService;

    @GetMapping("/batch")
    public ResponseEntity<List<GarminRunDTO>> getRunsByIds(@RequestParam String ids) {
        log.info("Batch fetch request for ids: {}", ids);
        
        try {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            
            List<GarminRun> runs = garminRunRepository.findAllById(idList);
            
            List<GarminRunDTO> dtos = runs.stream()
                    .map(run -> mapToDTO(run, new GarminRunDTO()))
                    .collect(Collectors.toList());
            
            log.info("Returning {} runs for batch request", dtos.size());
            return ResponseEntity.ok(dtos);
            
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in batch request: {}", ids);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<GarminRunDTO>> getRecentRuns(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        log.info("Fetching recent runs since: {}", since);
        
        OffsetDateTime sinceOffset = since.atOffset(ZoneOffset.UTC);
        List<GarminRun> runs = garminRunRepository.findByCreatedAtAfterOrderByCreatedAtDesc(sinceOffset);
        
        List<GarminRunDTO> dtos = runs.stream()
                .map(run -> mapToDTO(run, new GarminRunDTO()))
                .collect(Collectors.toList());
        
        log.info("Returning {} recent runs", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    private GarminRunDTO mapToDTO(GarminRun garminRun, GarminRunDTO garminRunDTO) {
        garminRunDTO.setId(garminRun.getId());
        garminRunDTO.setActivityId(garminRun.getActivityId());
        garminRunDTO.setActivityDate(garminRun.getActivityDate());
        garminRunDTO.setActivityType(garminRun.getActivityType());
        garminRunDTO.setActivityName(garminRun.getActivityName());
        garminRunDTO.setActivityDescription(garminRun.getActivityDescription());
        garminRunDTO.setElapsedTime(garminRun.getElapsedTime());
        garminRunDTO.setDistance(garminRun.getDistance());
        garminRunDTO.setMaxHeartRate(garminRun.getMaxHeartRate());
        garminRunDTO.setCalories(garminRun.getCalories());
        garminRunDTO.setCreatedBy(garminRun.getCreatedBy() == null ? null : garminRun.getCreatedBy().getId());
        garminRunDTO.setCreatedByName(garminRun.getCreatedBy() == null ? null : garminRun.getCreatedBy().getName());
        garminRunDTO.setUpdateBy(garminRun.getUpdateBy() == null ? null : garminRun.getUpdateBy().getId());
        garminRunDTO.setUpdateByName(garminRun.getUpdateBy() == null ? null : garminRun.getUpdateBy().getName());
        return garminRunDTO;
    }
}
