package me.sathish.runs_app.rifl;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Park §2.2 completion-record store; §3.1 simplified design — in-memory only,
 * relying on PostgreSQL WAL to make the underlying mutation durable.
 *
 * <p>Two maps:
 * <ul>
 *   <li>{@code results} — keyed by {@link RpcId}, holds the cached response.</li>
 *   <li>{@code firstIncomplete} — per-client watermark; everything below is GC-safe
 *       because the client has implicitly ACK'd it.</li>
 * </ul>
 */
@Component
public class ResultTracker {

    private final ConcurrentHashMap<RpcId, CompletionRecord> results = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> firstIncomplete = new ConcurrentHashMap<>();

    public Optional<CompletionRecord> lookup(RpcId id) {
        return Optional.ofNullable(results.get(id));
    }

    public void record(RpcId id, int httpStatus, String bodyJson) {
        results.putIfAbsent(id, new CompletionRecord(httpStatus, bodyJson, Instant.now()));
    }

    /**
     * Update the per-client watermark. Only advance — never let a stale ACK pull it back.
     */
    public void advanceFirstIncomplete(long clientId, long value) {
        firstIncomplete.merge(clientId, value, Math::max);
    }

    public long firstIncomplete(long clientId) {
        return firstIncomplete.getOrDefault(clientId, 0L);
    }

    /**
     * Drop every record for {@code clientId} whose sequence number is strictly less
     * than the client's watermark. Returns the count evicted (useful for tests + metrics).
     */
    public int trim(long clientId) {
        long cutoff = firstIncomplete(clientId);
        int[] count = {0};
        results.keySet().removeIf(id -> {
            boolean drop = id.clientId() == clientId && id.sequenceNumber() < cutoff;
            if (drop) {
                count[0]++;
            }
            return drop;
        });
        return count[0];
    }

    /**
     * Reap everything for {@code clientId}. Used when the lease expires.
     */
    public int reapAll(long clientId) {
        int[] count = {0};
        results.keySet().removeIf(id -> {
            boolean drop = id.clientId() == clientId;
            if (drop) {
                count[0]++;
            }
            return drop;
        });
        firstIncomplete.remove(clientId);
        return count[0];
    }

    /**
     * Test-only: total size of the records map.
     */
    public int size() {
        return results.size();
    }
}
