package com.cyclerouteplanner.backend.features.geo.api;

import com.cyclerouteplanner.backend.features.geo.api.dto.request.OsmFeatureIngestRequest;
import com.cyclerouteplanner.backend.features.geo.api.dto.request.TallinnLayerIngestRequest;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheIngestResponse;
import com.cyclerouteplanner.backend.features.geo.application.GeoCacheIngestService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "api", name = "dev-endpoints-enabled", havingValue = "true")
@RequestMapping("/api/geo/cache")
public class GeoCacheManualIngestController {

    private final GeoCacheIngestService geoCacheIngestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeoCacheManualIngestController(GeoCacheIngestService geoCacheIngestService) {
        this.geoCacheIngestService = geoCacheIngestService;
    }

    @PostMapping("/osm/ingest")
    public ResponseEntity<GeoCacheIngestResponse> ingestOsm(@RequestBody List<OsmFeatureIngestRequest> payload) {
        List<OsmFeatureCacheEntry> entries = payload.stream()
                .map(item -> new OsmFeatureCacheEntry(
                        item.sourceId(),
                        item.name(),
                        item.featureType(),
                        item.tags(),
                        item.wktGeometry(),
                        asJson(item.rawPayload())
                ))
                .toList();
        GeoCacheIngestStatus status = geoCacheIngestService.ingestOsmFeatures(entries);
        return toResponse(status);
    }

    @PostMapping("/tallinn/ingest")
    public ResponseEntity<GeoCacheIngestResponse> ingestTallinn(
            @RequestParam String sourceLayer,
            @RequestBody List<TallinnLayerIngestRequest> payload
    ) {
        List<TallinnLayerCacheEntry> entries = payload.stream()
                .map(item -> new TallinnLayerCacheEntry(
                        sourceLayer,
                        item.sourceId(),
                        item.name(),
                        item.properties(),
                        item.wktGeometry(),
                        asJson(item.rawPayload())
                ))
                .toList();
        GeoCacheIngestStatus status = geoCacheIngestService.ingestTallinnLayer(sourceLayer, entries);
        return toResponse(status);
    }

    private ResponseEntity<GeoCacheIngestResponse> toResponse(GeoCacheIngestStatus status) {
        GeoCacheIngestResponse response = new GeoCacheIngestResponse(
                status.source(),
                status.successful(),
                status.requestedCount(),
                status.upsertedCount(),
                status.details(),
                status.checkedAt()
        );
        if (status.successful()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    private String asJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize raw payload", ex);
        }
    }
}
