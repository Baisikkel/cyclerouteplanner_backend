package com.cyclerouteplanner.backend.features.osm.api.dto.response;

import java.time.Instant;

public record OsmConnectivityResponse(
        String provider,
        boolean reachable,
        String details,
        Instant checkedAt
) {
}
