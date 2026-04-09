package com.cyclerouteplanner.backend.core.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Profile({"local", "docker"})
public class RefreshRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_ENDPOINTS = Set.of(
            "/api/address/cache/refresh",
            "/api/geo/cache/osm/refresh",
            "/api/geo/cache/tallinn/refresh"
    );

    private final RefreshRateLimiter refreshRateLimiter;
    private final RefreshRateLimitProperties properties;

    public RefreshRateLimitFilter(RefreshRateLimiter refreshRateLimiter, RefreshRateLimitProperties properties) {
        this.refreshRateLimiter = refreshRateLimiter;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        if (isLimitedRequest(request, requestPath)) {
            String clientId = resolveClientId(request);
            boolean allowed = refreshRateLimiter.allow(requestPath, clientId);
            if (!allowed) {
                writeTooManyRequests(response, requestPath);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLimitedRequest(HttpServletRequest request, String requestPath) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LIMITED_ENDPOINTS.contains(requestPath);
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int separator = forwardedFor.indexOf(',');
            return separator >= 0 ? forwardedFor.substring(0, separator).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String endpoint) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = """
                {
                  "error": "too_many_requests",
                  "message": "Refresh rate limit exceeded",
                  "endpoint": "%s",
                  "maxRequests": %d,
                  "windowSeconds": %d
                }
                """.formatted(endpoint, properties.getMaxRequests(), properties.getWindowSeconds());
        response.getWriter().write(body);
    }
}
