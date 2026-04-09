package com.cyclerouteplanner.backend.features.address.api.dto.response;

import java.time.Instant;

public record AdsConnectivityResponse(
        String provider,
        boolean reachable,
        String details,
        Instant checkedAt
) {
}
