/**
 * RIFL (Reusable Infrastructure for Linearizability) — scaled-down implementation of
 * Seo Jin Park's Stanford PhD work (2019), applied to the {@code /api/garminRuns}
 * REST surface.
 *
 * <p>The goal is exactly-once execution of mutating RPCs in the face of client retries,
 * so the observed history is linearizable. See Park, ch. 2 and ch. 3.
 *
 * <p>This is the simplified Ch. 3 design: completion records live in memory only,
 * relying on PostgreSQL's WAL for mutation durability. On Spring restart, RIFL state
 * is lost; retries land on Postgres again, and correctness is preserved by making the
 * underlying mutation idempotent.
 *
 * <h2>Lease policy</h2>
 * Heartbeat every 10s, expiry at 30s. Park used 30s in RAMCloud; we keep the same
 * expiry budget but tighten the heartbeat to survive one missed beat over the open
 * internet.
 *
 * <h2>Headers on mutating requests under {@code /api/garminRuns}</h2>
 * <ul>
 *   <li>{@code X-Client-Id} — obtained from {@code POST /api/rifl/lease/open}</li>
 *   <li>{@code X-Sequence-Number} — client-assigned, monotonic per client</li>
 *   <li>{@code X-First-Incomplete} — smallest sequence the client has not yet ack'd; drives GC</li>
 * </ul>
 *
 * <h2>Response signals</h2>
 * <ul>
 *   <li>First execution — normal 200/201.</li>
 *   <li>Cached retry — same status and body, plus {@code X-Rifl-Replay: true}.</li>
 *   <li>Expired lease — HTTP 410, {@code {"error":"STALE_RPC"}}.</li>
 * </ul>
 */
package me.sathish.runs_app.rifl;
