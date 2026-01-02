package me.sathish.runsapp.runs_app.shedlock;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import me.sathish.runsapp.runs_app.security.UserRoles;
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
@RequestMapping(value = "/api/shedlocks", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('" + UserRoles.ROLE_ADMIN + "', '" + UserRoles.ROLE_USER + "')")
@SecurityRequirement(name = "basic-auth")
public class ShedlockResource {

    private final ShedlockService shedlockService;

    public ShedlockResource(final ShedlockService shedlockService) {
        this.shedlockService = shedlockService;
    }

    @GetMapping
    public ResponseEntity<List<ShedlockDTO>> getAllShedlocks() {
        return ResponseEntity.ok(shedlockService.findAll());
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
