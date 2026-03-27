package me.sathish.runs_app.shedlock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.sathish.runs_app.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/shedlocks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ShedLock Monitoring", description = "Read-only API for monitoring distributed locks")
public class ShedlockViewResource {

    private final ShedlockViewService shedlockViewService;

    public ShedlockViewResource(final ShedlockViewService shedlockViewService) {
        this.shedlockViewService = shedlockViewService;
    }

    @GetMapping
    @Operation(summary = "Get all ShedLock entries", description = "Returns a paginated list of all lock entries. This is read-only for monitoring purposes.")
    public ResponseEntity<PagedResponse<ShedlockViewDTO>> getAllShedlocks(
            @PageableDefault(size = 20)
            @SortDefault(sort = "lockedAt", direction = Sort.Direction.DESC) final Pageable pageable) {
        Page<ShedlockViewDTO> page = shedlockViewService.findAll(pageable);
        return ResponseEntity.ok(new PagedResponse<>(page));
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a specific ShedLock entry by name")
    @ApiResponse(responseCode = "200", description = "Lock entry found")
    @ApiResponse(responseCode = "404", description = "Lock entry not found")
    public ResponseEntity<ShedlockViewDTO> getShedlock(
            @PathVariable(name = "name") final String name) {
        return ResponseEntity.ok(shedlockViewService.get(name));
    }

}
