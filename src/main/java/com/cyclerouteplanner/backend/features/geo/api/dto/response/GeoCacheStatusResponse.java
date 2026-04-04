package com.cyclerouteplanner.backend.features.geo.api.dto.response;

import java.time.Instant;

public record GeoCacheStatusResponse(
        Instant checkedAt,
        GeoCacheSourceStatusResponse ads,
        GeoCacheSourceStatusResponse osm,
        GeoCacheSourceStatusResponse tallinn
) {
}
