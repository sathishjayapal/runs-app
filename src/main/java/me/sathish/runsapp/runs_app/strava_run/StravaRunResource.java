package me.sathish.runsapp.runs_app.strava_run;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import me.sathish.runsapp.runs_app.security.UserRoles;
import me.sathish.runsapp.runs_app.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/stravaRuns", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@SecurityRequirement(name = "basic-auth")
public class StravaRunResource {

    private final StravaRunService stravaRunService;
    private final UserService userService;

    public StravaRunResource(final StravaRunService stravaRunService,
            final UserService userService) {
        this.stravaRunService = stravaRunService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<StravaRunDTO>> getAllStravaRuns() {
        return ResponseEntity.ok(stravaRunService.findAll());
    }

    @GetMapping("/{runNumber}")
    public ResponseEntity<StravaRunDTO> getStravaRun(
            @PathVariable(name = "runNumber") final Long runNumber) {
        return ResponseEntity.ok(stravaRunService.get(runNumber));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createStravaRun(
            @RequestBody @Valid final StravaRunDTO stravaRunDTO) {
        final Long createdRunNumber = stravaRunService.create(stravaRunDTO);
        return new ResponseEntity<>(createdRunNumber, HttpStatus.CREATED);
    }

    @PutMapping("/{runNumber}")
    public ResponseEntity<Long> updateStravaRun(
            @PathVariable(name = "runNumber") final Long runNumber,
            @RequestBody @Valid final StravaRunDTO stravaRunDTO) {
        stravaRunService.update(runNumber, stravaRunDTO);
        return ResponseEntity.ok(runNumber);
    }

    @DeleteMapping("/{runNumber}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteStravaRun(
            @PathVariable(name = "runNumber") final Long runNumber) {
        stravaRunService.delete(runNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/createdByValues")
    public ResponseEntity<Map<Long, String>> getCreatedByValues() {
        return ResponseEntity.ok(userService.getUserValues());
    }

}
