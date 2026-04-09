package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import com.cyclerouteplanner.backend.features.address.domain.AdsSearchStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdsService {

    private static final String PROVIDER = "maa-amet-ads";
    private static final int MAX_SNIPPET_LENGTH = 1_000;
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

    /**
     * Executes a thin-slice ADS search call to validate request/response wiring before full domain mapping is added.
     */
    public AdsSearchStatus search(String query, int limit) {
        try {
            String payload = adsGatewayPort.search(query, limit);
            return new AdsSearchStatus(
                    true,
                    PROVIDER,
                    query,
                    limit,
                    "Search request completed",
                    truncate(payload),
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            return new AdsSearchStatus(
                    false,
                    PROVIDER,
                    query,
                    limit,
                    ex.getMessage(),
                    null,
                    Instant.now()
            );
        }
    }

    private String summarize(String payload) {
        if (payload == null || payload.isBlank()) {
            return "No status payload returned";
        }
        return payload.lines().findFirst().orElse(payload).trim();
    }

    private String truncate(String payload) {
        if (payload == null) {
            return null;
        }
        if (payload.length() <= MAX_SNIPPET_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}
