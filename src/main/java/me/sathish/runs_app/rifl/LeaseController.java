package me.sathish.runs_app.rifl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rifl/lease")
public class LeaseController {

    private final LeaseManager leaseManager;

    public LeaseController(LeaseManager leaseManager) {
        this.leaseManager = leaseManager;
    }

    @PostMapping("/open")
    public ResponseEntity<Long> open() {
        return ResponseEntity.ok(leaseManager.openLease());
    }

    @PostMapping
    public ResponseEntity<Void> renew(@RequestHeader("X-Client-Id") long clientId) {
        leaseManager.renew(clientId);
        return ResponseEntity.noContent().build();
    }
}
