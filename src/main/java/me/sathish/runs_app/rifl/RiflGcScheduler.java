package me.sathish.runs_app.rifl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RiflGcScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiflGcScheduler.class);

    private final LeaseManager leaseManager;
    private final ResultTracker resultTracker;

    public RiflGcScheduler(LeaseManager leaseManager, ResultTracker resultTracker) {
        this.leaseManager = leaseManager;
        this.resultTracker = resultTracker;
    }

    @Scheduled(fixedDelayString = "${rifl.gc-interval:30000}")
    void reapExpiredClients() {
        Set<Long> expired = leaseManager.expiredClients();
        for (Long clientId : expired) {
            int reaped = resultTracker.reapAll(clientId);
            leaseManager.expire(clientId);
            log.debug("RIFL GC: reaped {} records for expired client {}", reaped, clientId);
        }
        if (!expired.isEmpty()) {
            log.info("RIFL GC: expired {} clients", expired.size());
        }
    }
}
