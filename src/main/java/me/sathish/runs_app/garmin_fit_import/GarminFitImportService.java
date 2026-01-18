package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.file_name_tracker.FileNameTracker;
import me.sathish.runs_app.file_name_tracker.FileNameTrackerRepository;
import me.sathish.runs_app.garmin_run.GarminRunDTO;
import me.sathish.runs_app.garmin_run.GarminRunService;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service
@Slf4j
public class GarminFitImportService {

    private final GarminFitFileParser fitFileParser;
    private final GarminRunService garminRunService;
    private final FileNameTrackerRepository fileNameTrackerRepository;
    private final RunAppUserRepository runAppUserRepository;

    @Value("${app.garmin.import.folder}")
    private String importFolder;

    @Value("${app.garmin.import.systemUserId}")
    private Long systemUserId;

    public GarminFitImportService(
            GarminFitFileParser fitFileParser,
            GarminRunService garminRunService,
            FileNameTrackerRepository fileNameTrackerRepository,
            RunAppUserRepository runAppUserRepository) {
        this.fitFileParser = fitFileParser;
        this.garminRunService = garminRunService;
        this.fileNameTrackerRepository = fileNameTrackerRepository;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Transactional
    public ImportResult processImportFolder() {
        ImportResult result = new ImportResult();
        
        log.info("Starting Garmin FIT file import from folder: {}", importFolder);
        
        File folder = new File(importFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Import folder does not exist or is not a directory: {}", importFolder);
            return result;
        }

        File[] files = folder.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".fit"));
        
        if (files == null || files.length == 0) {
            log.info("No ZIP or FIT files found in import folder");
            return result;
        }

        log.info("Found {} file(s) to process", files.length);

        for (File file : files) {
            try {
                if (file.getName().toLowerCase().endsWith(".zip")) {
                    processZipFile(file, result);
                } else if (file.getName().toLowerCase().endsWith(".fit")) {
                    processFitFile(file, result);
                }
            } catch (Exception e) {
                log.error("Error processing file: {}", file.getName(), e);
                result.addFailed(file.getName(), e.getMessage());
            }
        }

        log.info("Import completed - Success: {}, Skipped: {}, Failed: {}", 
                result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());
        
        return result;
    }

    private void processZipFile(File zipFile, ImportResult result) throws IOException {
        log.info("Processing ZIP file: {}", zipFile.getName());
        
        Path tempDir = Files.createTempDirectory("garmin-fit-extract-");
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".fit")) {
                    // Extract to temp directory
                    File tempFitFile = new File(tempDir.toFile(), new File(entry.getName()).getName());
                    
                    try (FileOutputStream fos = new FileOutputStream(tempFitFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    
                    // Process the extracted FIT file
                    processFitFile(tempFitFile, result);
                    
                    // Delete temp file
                    tempFitFile.delete();
                }
                zis.closeEntry();
            }
        } finally {
            // Clean up temp directory
            Files.deleteIfExists(tempDir);
        }
        
        // Move processed ZIP to archive or delete
        archiveProcessedFile(zipFile);
    }

    private void processFitFile(File fitFile, ImportResult result) {
        String fileName = fitFile.getName();
        
        // Check if already processed
        if (fileNameTrackerRepository.existsByFileName(fileName)) {
            log.debug("File already processed, skipping: {}", fileName);
            result.addSkipped(fileName);
            return;
        }

        try {
            // Parse FIT file
            FitActivityData activityData = fitFileParser.parse(fitFile.getAbsolutePath());
            
            // Convert to GarminRunDTO
            GarminRunDTO dto = mapToGarminRunDTO(activityData, fileName);
            
            // Save to database
            Long createdId = garminRunService.create(dto);
            
            // Track processed file
            trackProcessedFile(fileName);
            
            result.addSuccess(fileName);
            log.info("Successfully imported activity from file: {} (ID: {})", fileName, createdId);
            
        } catch (Exception e) {
            log.error("Failed to process FIT file: {}", fileName, e);
            result.addFailed(fileName, e.getMessage());
        }
    }

    private GarminRunDTO mapToGarminRunDTO(FitActivityData activityData, String fileName) {
        GarminRunDTO dto = new GarminRunDTO();
        
        // Use timestamp as activity ID, or generate from filename if not available
        dto.setActivityId(activityData.getActivityId() != null ? 
                activityData.getActivityId() : generateActivityIdFromFileName(fileName));
        
        dto.setActivityDate(activityData.getActivityDate());
        dto.setActivityType(activityData.getActivityType() != null ? 
                activityData.getActivityType() : "running");
        
        // Use filename as activity name if not available
        dto.setActivityName(activityData.getActivityName() != null ? 
                activityData.getActivityName() : fileName.replace(".fit", ""));
        
        dto.setActivityDescription("Imported from FIT file: " + fileName);
        
        // Format elapsed time as HH:MM:SS
        dto.setElapsedTime(activityData.getFormattedElapsedTime());
        
        // Format distance
        if (activityData.getDistanceMiles() != null) {
            dto.setDistance(String.format("%.2f", activityData.getDistanceMiles()));
        }
        
        // Max heart rate
        if (activityData.getMaxHeartRate() != null) {
            dto.setMaxHeartRate(String.valueOf(activityData.getMaxHeartRate()));
        }
        
        // Calories
        if (activityData.getCalories() != null) {
            dto.setCalories(String.valueOf(activityData.getCalories()));
        }
        
        // Set system user as creator
        dto.setCreatedBy(systemUserId);
        
        return dto;
    }

    private String generateActivityIdFromFileName(String fileName) {
        // Extract timestamp or use hash of filename
        return String.valueOf(fileName.hashCode() & 0x7FFFFFFF);
    }

    private void trackProcessedFile(String fileName) {
        FileNameTracker tracker = new FileNameTracker();
        tracker.setFileName(fileName);
        tracker.setUpdatedBy("SYSTEM");
        
        // Get system user
        RunAppUser systemUser = runAppUserRepository.findById(systemUserId)
                .orElseThrow(() -> new IllegalStateException("System user not found with ID: " + systemUserId));
        tracker.setCreatedBy(systemUser);
        
        fileNameTrackerRepository.save(tracker);
        log.debug("Tracked processed file: {}", fileName);
    }

    private void archiveProcessedFile(File file) {
        try {
            Path archivePath = Paths.get(importFolder, "processed", file.getName());
            Files.createDirectories(archivePath.getParent());
            Files.move(file.toPath(), archivePath);
            log.info("Archived processed file to: {}", archivePath);
        } catch (IOException e) {
            log.warn("Failed to archive file: {}, deleting instead", file.getName(), e);
            file.delete();
        }
    }
}
