package com.cyclerouteplanner.backend.features.osm.domain;

import java.time.Instant;

public record OsmConnectivityStatus(
        boolean reachable,
        String provider,
        String details,
        Instant checkedAt
) {
}
