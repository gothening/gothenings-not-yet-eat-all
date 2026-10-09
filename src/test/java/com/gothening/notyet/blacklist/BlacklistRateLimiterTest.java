package com.gothening.notyet.blacklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BlacklistRateLimiterTest {
    @Test
    void rejectsRequestsInsideTheWindowAndAllowsAfterwards() {
        AtomicLong clock = new AtomicLong(1_000L);
        BlacklistRateLimiter limiter = new BlacklistRateLimiter(100L, clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.allow(player));
        assertFalse(limiter.allow(player));

        clock.set(1_099L);
        assertFalse(limiter.allow(player));

        clock.set(1_199L);
        assertTrue(limiter.allow(player));
    }

    @Test
    void tracksPlayersIndependently() {
        AtomicLong clock = new AtomicLong(0L);
        BlacklistRateLimiter limiter = new BlacklistRateLimiter(100L, clock::get);
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        assertTrue(limiter.allow(playerA));
        assertTrue(limiter.allow(playerB));
        assertFalse(limiter.allow(playerA));
    }
}
