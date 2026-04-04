package com.cyclerouteplanner.backend.core.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local", "docker"})
public class RefreshRateLimiter {

    private final RefreshRateLimitProperties properties;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RefreshRateLimiter(RefreshRateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean allow(String endpoint, String clientId) {
        if (properties.getMaxRequests() <= 0 || properties.getWindowSeconds() <= 0) {
            return true;
        }

        String key = endpoint + "::" + clientId;
        long now = System.currentTimeMillis();
        long windowMs = properties.getWindowSeconds() * 1_000L;
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));

        synchronized (counter) {
            if (now - counter.windowStartMs >= windowMs) {
                counter.windowStartMs = now;
                counter.requestCount = 0;
            }
            if (counter.requestCount >= properties.getMaxRequests()) {
                return false;
            }
            counter.requestCount++;
            return true;
        }
    }

    private static final class WindowCounter {
        private long windowStartMs;
        private int requestCount;

        private WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.requestCount = 0;
        }
    }
}
