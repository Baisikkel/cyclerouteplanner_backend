package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.infra.GeoIngestProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class OsmGeoRefreshService {

    private final OsmGeoSourcePort osmGeoSourcePort;
    private final GeoCacheIngestService geoCacheIngestService;
    private final GeoIngestProperties geoIngestProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OsmGeoRefreshService(
            OsmGeoSourcePort osmGeoSourcePort,
            GeoCacheIngestService geoCacheIngestService,
            GeoIngestProperties geoIngestProperties
    ) {
        this.osmGeoSourcePort = osmGeoSourcePort;
        this.geoCacheIngestService = geoCacheIngestService;
        this.geoIngestProperties = geoIngestProperties;
    }

    public GeoCacheIngestStatus refreshTallinnCycleNetwork() {
        String payload = osmGeoSourcePort.fetchCycleNetwork(
                geoIngestProperties.getDefaultBboxSouth(),
                geoIngestProperties.getDefaultBboxWest(),
                geoIngestProperties.getDefaultBboxNorth(),
                geoIngestProperties.getDefaultBboxEast(),
                geoIngestProperties.getOverpassTimeoutSeconds()
        );
        List<OsmFeatureCacheEntry> entries = parseEntries(payload);
        return geoCacheIngestService.ingestOsmFeatures(entries);
    }

    private List<OsmFeatureCacheEntry> parseEntries(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                return List.of();
            }

            List<OsmFeatureCacheEntry> entries = new ArrayList<>();
            for (JsonNode element : elements) {
                OsmFeatureCacheEntry entry = toEntry(element);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Overpass payload", ex);
        }
    }

    private OsmFeatureCacheEntry toEntry(JsonNode element) {
        String type = element.path("type").asText(null);
        JsonNode idNode = element.get("id");
        if (type == null || idNode == null || !idNode.canConvertToLong()) {
            return null;
        }

        String wktGeometry = extractWkt(element);
        if (wktGeometry == null) {
            return null;
        }

        JsonNode tagsNode = element.path("tags");
        Map<String, Object> tags = tagsNode.isObject()
                ? objectMapper.convertValue(tagsNode, Map.class)
                : Map.of();
        String name = tags.get("name") instanceof String value ? value : null;
        String featureType = tags.get("highway") instanceof String value ? value : type;

        return new OsmFeatureCacheEntry(
                type + "/" + idNode.asText(),
                name,
                featureType,
                tags,
                wktGeometry,
                element.toString()
        );
    }

    private String extractWkt(JsonNode element) {
        if (element.has("lat") && element.has("lon")) {
            return "POINT(%s %s)".formatted(
                    element.get("lon").asText(),
                    element.get("lat").asText()
            );
        }

        JsonNode geometry = element.path("geometry");
        if (geometry.isArray() && geometry.size() >= 2) {
            List<String> points = new ArrayList<>();
            for (JsonNode point : geometry) {
                if (!point.has("lat") || !point.has("lon")) {
                    continue;
                }
                points.add(point.get("lon").asText() + " " + point.get("lat").asText());
            }
            if (points.size() >= 2) {
                return "LINESTRING(" + String.join(",", points) + ")";
            }
        }

        return null;
    }
}
