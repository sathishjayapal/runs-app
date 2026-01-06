package me.sathish.runs_app.shedlock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import me.sathish.runs_app.security.UserRoles;
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
@RequestMapping(value = "/api/shedlocks", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@SecurityRequirement(name = "basic-auth")
public class ShedlockResource {

    private final ShedlockService shedlockService;

    public ShedlockResource(final ShedlockService shedlockService) {
        this.shedlockService = shedlockService;
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
    public ResponseEntity<Page<ShedlockDTO>> getAllShedlocks(
            @RequestParam(name = "filter", required = false) final String filter,
            @Parameter(hidden = true) @SortDefault(sort = "name") @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(shedlockService.findAll(filter, pageable));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ShedlockDTO> getShedlock(@PathVariable(name = "name") final Long name) {
        return ResponseEntity.ok(shedlockService.get(name));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createShedlock(@RequestBody @Valid final ShedlockDTO shedlockDTO) {
        final Long createdName = shedlockService.create(shedlockDTO);
        return new ResponseEntity<>(createdName, HttpStatus.CREATED);
    }

    @PutMapping("/{name}")
    public ResponseEntity<Long> updateShedlock(@PathVariable(name = "name") final Long name,
            @RequestBody @Valid final ShedlockDTO shedlockDTO) {
        shedlockService.update(name, shedlockDTO);
        return ResponseEntity.ok(name);
    }

    @DeleteMapping("/{name}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteShedlock(@PathVariable(name = "name") final Long name) {
        shedlockService.delete(name);
        return ResponseEntity.noContent().build();
    }

}
