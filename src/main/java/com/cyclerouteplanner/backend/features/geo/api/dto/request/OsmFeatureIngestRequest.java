package com.cyclerouteplanner.backend.features.geo.api.dto.request;

import java.util.Map;

public record OsmFeatureIngestRequest(
        String sourceId,
        String name,
        String featureType,
        Map<String, Object> tags,
        String wktGeometry,
        Map<String, Object> rawPayload
) {
}
