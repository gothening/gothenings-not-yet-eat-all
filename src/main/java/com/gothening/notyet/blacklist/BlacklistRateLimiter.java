package com.gothening.notyet.blacklist;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Minimal per-player rate limit for blacklist mutation requests.
 * Only used from the server thread.
 */
public final class BlacklistRateLimiter {
    private final long minIntervalMillis;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastActionAt = new HashMap<>();

    public BlacklistRateLimiter(long minIntervalMillis, LongSupplier clock) {
        this.minIntervalMillis = minIntervalMillis;
        this.clock = clock;
    }

    public boolean allow(UUID playerId) {
        long now = clock.getAsLong();
        Long last = lastActionAt.put(playerId, now);
        return last == null || now - last >= minIntervalMillis;
    }
}
