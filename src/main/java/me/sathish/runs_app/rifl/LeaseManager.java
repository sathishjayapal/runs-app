package me.sathish.runs_app.rifl;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LeaseManager {

    public static final Duration EXPIRY = Duration.ofSeconds(60);

    private final ConcurrentHashMap<Long, Instant> lastSeen = new ConcurrentHashMap<>();
    private final AtomicLong nextClientId = new AtomicLong(1);

    public long openLease() {
        long id = nextClientId.getAndIncrement();
        lastSeen.put(id, Instant.now());
        return id;
    }

    public void renew(long clientId) {
        if (!lastSeen.containsKey(clientId)) {
            throw new StaleRpcException(clientId);
        }
        lastSeen.put(clientId, Instant.now());
    }

    public boolean isAlive(long clientId) {
        Instant seen = lastSeen.get(clientId);
        if (seen == null) {
            return false;
        }
        return Duration.between(seen, Instant.now()).compareTo(EXPIRY) < 0;
    }

    public void expire(long clientId) {
        lastSeen.remove(clientId);
    }

    public Set<Long> expiredClients() {
        Instant cutoff = Instant.now().minus(EXPIRY);
        return lastSeen.entrySet().stream()
                .filter(e -> e.getValue().isBefore(cutoff))
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
