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
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/garmin-import", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('" + UserRoles.ROLE_ADMIN + "')")
@SecurityRequirement(name = "basic-auth")
@Slf4j
public class GarminFitImportResource {

    private final GarminFitImportScheduledJob garminFitImportScheduledJob;

    public GarminFitImportResource(GarminFitImportScheduledJob garminFitImportScheduledJob) {
        this.garminFitImportScheduledJob = garminFitImportScheduledJob;
    }

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger Garmin FIT file import")
    @ApiResponse(responseCode = "200", description = "Import completed")
    public ResponseEntity<ImportResult> triggerImport() {
        log.info("Manual import triggered via REST API");
        ImportResult result = garminFitImportScheduledJob.triggerManualImport();
        return ResponseEntity.ok(result);
    }
}
