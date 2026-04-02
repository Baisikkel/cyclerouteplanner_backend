package com.cyclerouteplanner.backend.features.osm.application;

import com.cyclerouteplanner.backend.features.osm.domain.OsmConnectivityStatus;
import com.cyclerouteplanner.backend.features.osm.domain.OsmStatusPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OsmConnectivityService {

    private static final String PROVIDER = "overpass";
    private final OsmStatusPort osmStatusPort;

    public OsmConnectivityService(OsmStatusPort osmStatusPort) {
        this.osmStatusPort = osmStatusPort;
    }

    public OsmConnectivityStatus checkConnectivity() {
        try {
            String statusPayload = osmStatusPort.fetchStatus();
            return new OsmConnectivityStatus(true, PROVIDER, summarizeStatus(statusPayload), Instant.now());
        } catch (RuntimeException ex) {
            return new OsmConnectivityStatus(false, PROVIDER, ex.getMessage(), Instant.now());
        }
    }

    private String summarizeStatus(String statusPayload) {
        if (statusPayload == null || statusPayload.isBlank()) {
            return "No status payload returned";
        }
        return statusPayload.lines().findFirst().orElse(statusPayload).trim();
    }
}
