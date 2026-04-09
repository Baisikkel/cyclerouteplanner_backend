package com.cyclerouteplanner.backend.features.address.domain;

import java.time.Instant;

public record AdsConnectivityStatus(
        boolean reachable,
        String provider,
        String details,
        Instant checkedAt
) {
}
