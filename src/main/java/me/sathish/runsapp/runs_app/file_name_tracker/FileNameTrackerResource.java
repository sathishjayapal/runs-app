package me.sathish.runsapp.runs_app.file_name_tracker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Map;
import me.sathish.runsapp.runs_app.security.UserRoles;
import me.sathish.runsapp.runs_app.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/fileNameTrackers", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@SecurityRequirement(name = "basic-auth")
public class FileNameTrackerResource {

    private final FileNameTrackerService fileNameTrackerService;
    private final UserService userService;

    public FileNameTrackerResource(final FileNameTrackerService fileNameTrackerService,
            final UserService userService) {
        this.fileNameTrackerService = fileNameTrackerService;
        this.userService = userService;
    }

    @Operation(
            parameters = {
                    @Parameter(
                            name = "page",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = Integer.class)
                    ),
                    @Parameter(
                            name = "size",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = Integer.class)
                    ),
                    @Parameter(
                            name = "sort",
                            in = ParameterIn.QUERY,
                            schema = @Schema(implementation = String.class)
                    )
            }
    )
    @GetMapping
    public ResponseEntity<Page<FileNameTrackerDTO>> getAllFileNameTrackers(
            @RequestParam(name = "filter", required = false) final String filter,
            @Parameter(hidden = true) @SortDefault(sort = "id") @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(fileNameTrackerService.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileNameTrackerDTO> getFileNameTracker(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(fileNameTrackerService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createFileNameTracker(
            @RequestBody @Valid final FileNameTrackerDTO fileNameTrackerDTO) {
        final Long createdId = fileNameTrackerService.create(fileNameTrackerDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateFileNameTracker(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final FileNameTrackerDTO fileNameTrackerDTO) {
        fileNameTrackerService.update(id, fileNameTrackerDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteFileNameTracker(@PathVariable(name = "id") final Long id) {
        fileNameTrackerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/createdByValues")
    public ResponseEntity<Map<Long, String>> getCreatedByValues() {
        return ResponseEntity.ok(userService.getUserValues());
    }

}
