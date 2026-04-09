package com.cyclerouteplanner.backend.core.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshRateLimiterTest {

    @Test
    void blocksAfterConfiguredLimitAndResetsAfterWindow() {
        RefreshRateLimitProperties properties = new RefreshRateLimitProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1);
        RefreshRateLimiter limiter = new RefreshRateLimiter(properties);

        assertTrue(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));
        assertTrue(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));
        assertFalse(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));

        boolean allowedAfterReset = false;
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1")) {
                allowedAfterReset = true;
                break;
            }
        }

        assertTrue(allowedAfterReset);
    }
}
