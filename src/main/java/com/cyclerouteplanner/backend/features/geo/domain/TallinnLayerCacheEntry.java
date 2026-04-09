package com.cyclerouteplanner.backend.features.geo.domain;

import java.util.Map;

public record TallinnLayerCacheEntry(
        String sourceLayer,
        String sourceId,
        String name,
        Map<String, Object> properties,
        String wktGeometry,
        String rawPayload
) {
}
