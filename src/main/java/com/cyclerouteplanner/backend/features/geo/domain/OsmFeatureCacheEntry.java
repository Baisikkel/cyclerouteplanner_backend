package com.cyclerouteplanner.backend.features.geo.domain;

import java.util.Map;

public record OsmFeatureCacheEntry(
        String sourceId,
        String name,
        String featureType,
        Map<String, Object> tags,
        String wktGeometry,
        String rawPayload
) {
}
