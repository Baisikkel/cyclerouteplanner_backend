package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.core.util.TextUtils;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdsService {

    private static final String PROVIDER = "maa-amet-ads";
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_SEARCH_LIMIT = 10;
    private final AdsGatewayPort adsGatewayPort;

    public AdsService(AdsGatewayPort adsGatewayPort) {
        this.adsGatewayPort = adsGatewayPort;
    }

    /**
     * Performs a minimal external call to confirm the configured ADS provider is reachable.
     */
    public AdsConnectivityStatus checkConnectivity() {
        try {
            String statusPayload = adsGatewayPort.fetchStatus();
            return new AdsConnectivityStatus(true, PROVIDER, summarize(statusPayload), Instant.now());
        } catch (RuntimeException ex) {
            return new AdsConnectivityStatus(false, PROVIDER, ex.getMessage(), Instant.now());
        }
    }

    public List<AdsAddressSuggestion> search(String query, int limit) {
        String normalizedQuery = validateQuery(query);
        validateLimit(limit);

        return adsGatewayPort.search(normalizedQuery, limit);
    }

    private String summarize(String payload) {
        if (TextUtils.isBlank(payload)) {
            return "No status payload returned";
        }
        return payload.lines().findFirst().orElse(payload).trim();
    }

    private String validateQuery(String query) {
        if (TextUtils.isBlank(query)) {
            throw new IllegalArgumentException("query must not be blank");
        }
        String normalized = query.trim();
        if (normalized.length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException("query must be at least " + MIN_QUERY_LENGTH + " characters");
        }
        return normalized;
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_SEARCH_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_SEARCH_LIMIT);
        }
    }
}
