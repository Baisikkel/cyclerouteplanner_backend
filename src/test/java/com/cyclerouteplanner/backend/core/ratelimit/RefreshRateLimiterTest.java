package com.cyclerouteplanner.backend.core.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshRateLimiterTest {

    @Test
    void blocksAfterConfiguredLimitAndResetsAfterWindow() throws Exception {
        RefreshRateLimitProperties properties = new RefreshRateLimitProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1);
        RefreshRateLimiter limiter = new RefreshRateLimiter(properties);

        assertTrue(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));
        assertTrue(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));
        assertFalse(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));

        Thread.sleep(1_050);

        assertTrue(limiter.allow("/api/geo/cache/osm/refresh", "127.0.0.1"));
    }
}
