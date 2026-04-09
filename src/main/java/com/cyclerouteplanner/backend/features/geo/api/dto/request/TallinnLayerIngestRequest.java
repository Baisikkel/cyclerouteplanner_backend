package com.cyclerouteplanner.backend.features.geo.api.dto.request;

import java.util.Map;

public record TallinnLayerIngestRequest(
        String sourceId,
        String name,
        Map<String, Object> properties,
        String wktGeometry,
        Map<String, Object> rawPayload
) {
}
