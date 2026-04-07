package me.sathish.runs_app.garmin_fit_import;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.security.UserRoles;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/garmin-import", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('" + UserRoles.ROLE_ADMIN + "')")
@SecurityRequirement(name = "basic-auth")
@Slf4j
public class GarminFitImportResource {

    private final GarminFitImportScheduledJob garminFitImportScheduledJob;
    private final GarminCsvImportService garminCsvImportService;
    private final UnifiedGarminImportService unifiedGarminImportService;

    public GarminFitImportResource(GarminFitImportScheduledJob garminFitImportScheduledJob,
                                    GarminCsvImportService garminCsvImportService,
                                    UnifiedGarminImportService unifiedGarminImportService) {
        this.garminFitImportScheduledJob = garminFitImportScheduledJob;
        this.garminCsvImportService = garminCsvImportService;
        this.unifiedGarminImportService = unifiedGarminImportService;
    }

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger Garmin FIT file import")
    @ApiResponse(responseCode = "200", description = "Import completed")
    public ResponseEntity<ImportResult> triggerImport() {
        log.info("Manual FIT import triggered via REST API");
        ImportResult result = garminFitImportScheduledJob.triggerManualImport();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/trigger-csv")
    @Operation(summary = "Import Garmin activities from CSV file",
               description = "Parses activities.csv, skips records already in DB, reconciles results. " +
                             "Optional 'folder' param overrides the configured csv-folder.")
    @ApiResponse(responseCode = "200", description = "CSV import completed")
    public ResponseEntity<ImportResult> triggerCsvImport(
            @RequestParam(required = false) String folder) {
        log.info("Manual CSV import triggered via REST API, folder={}", folder);
        ImportResult result = garminCsvImportService.processImportFolder(folder);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/trigger-unified")
    @Operation(summary = "Import both Garmin FIT and CSV files in parallel",
               description = "Processes FIT files and CSV files simultaneously in separate threads. " +
                             "Provides comprehensive reconciliation report showing what was processed, " +
                             "what was skipped, what failed, and next steps. " +
                             "Optional 'csvFolder' param overrides the configured csv-folder.")
    @ApiResponse(responseCode = "200", description = "Unified import completed")
    public ResponseEntity<UnifiedImportResult> triggerUnifiedImport(
            @RequestParam(required = false) String csvFolder) {
        log.info("Manual unified import triggered via REST API, csvFolder={}", csvFolder);
        UnifiedImportResult result = unifiedGarminImportService.processAllFiles(csvFolder);
        return ResponseEntity.ok(result);
    }
}
