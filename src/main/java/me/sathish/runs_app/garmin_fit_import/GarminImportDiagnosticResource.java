package me.sathish.runs_app.garmin_fit_import;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import me.sathish.runs_app.security.UserRoles;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/garminRuns/import-diagnostics", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('" + UserRoles.ROLE_ADMIN + "')")
@SecurityRequirement(name = "basic-auth")
public class GarminImportDiagnosticResource {

    private final GarminCsvImportProperties properties;
    private final GoogleDriveCsvFileProvider driveProvider;

    public GarminImportDiagnosticResource(GarminCsvImportProperties properties,
                                          GoogleDriveCsvFileProvider driveProvider) {
        this.properties = properties;
        this.driveProvider = driveProvider;
    }

    @GetMapping
    @Operation(summary = "Diagnose Garmin CSV import pipeline",
            description = "Checks each step of the Google Drive import setup and reports what is working or failing")
    public ResponseEntity<Map<String, Object>> diagnose() {
        Map<String, Object> report = new LinkedHashMap<>();

        // Step 1: Check configured source
        report.put("1_configuredSource", String.valueOf(properties.getSource()));

        // Step 2: Check credentials configuration
        GarminCsvImportProperties.Drive driveProps = properties.getDrive();
        boolean hasKeyPath = driveProps != null
                && driveProps.getServiceAccountKeyPath() != null
                && !driveProps.getServiceAccountKeyPath().isBlank();
        boolean hasKeyBase64 = driveProps != null
                && driveProps.getServiceAccountKeyBase64() != null
                && !driveProps.getServiceAccountKeyBase64().isBlank();
        report.put("2_serviceAccountKeyPathConfigured", hasKeyPath);
        report.put("2_serviceAccountKeyBase64Configured", hasKeyBase64);

        if (hasKeyPath) {
            java.nio.file.Path keyPath = java.nio.file.Path.of(driveProps.getServiceAccountKeyPath());
            report.put("2_serviceAccountKeyPathExists", java.nio.file.Files.exists(keyPath));
            report.put("2_serviceAccountKeyPathValue", driveProps.getServiceAccountKeyPath());
        }

        // Step 3: Check Drive client initialization
        boolean driveInitialized = driveProvider.isDriveClientInitialized();
        report.put("3_driveClientInitialized", driveInitialized);

        // Step 4: Check folder ID
        String folderId = driveProps != null ? driveProps.getFolderId() : null;
        boolean hasFolderId = folderId != null && !folderId.isBlank();
        report.put("4_folderIdConfigured", hasFolderId);
        if (hasFolderId) {
            report.put("4_folderId", folderId);
        }

        // Step 5: Check processed folder ID
        String processedFolderId = driveProps != null ? driveProps.getProcessedFolderId() : null;
        report.put("5_processedFolderIdConfigured",
                processedFolderId != null && !processedFolderId.isBlank());

        // Step 6: Check systemUserId
        report.put("6_systemUserId", properties.getSystemUserId());

        // Step 7: Try listing ALL files in the folder (not just CSV)
        if (driveInitialized && hasFolderId) {
            try {
                List<Map<String, String>> allFiles = driveProvider.listAllFilesInFolder(folderId);
                report.put("7_folderAccessible", true);
                report.put("7_totalFilesInFolder", allFiles.size());
                report.put("7_files", allFiles);

                long csvCount = allFiles.stream()
                        .filter(f -> "text/csv".equals(f.get("mimeType")))
                        .count();
                report.put("7_csvMimeTypeCount", csvCount);

                if (csvCount == 0 && !allFiles.isEmpty()) {
                    report.put("7_HINT", "Files exist but none have mimeType 'text/csv'. "
                            + "The import query filters on mimeType='text/csv'. "
                            + "Check if the file was uploaded as a Google Sheet or other type.");
                }
            } catch (Exception e) {
                report.put("7_folderAccessible", false);
                report.put("7_error", e.getClass().getSimpleName() + ": " + e.getMessage());
                report.put("7_HINT", "Ensure the Google Drive folder is shared with the service account email.");
            }
        } else {
            report.put("7_skipped", "Drive client not initialized or folder ID not configured");
        }

        return ResponseEntity.ok(report);
    }
}
