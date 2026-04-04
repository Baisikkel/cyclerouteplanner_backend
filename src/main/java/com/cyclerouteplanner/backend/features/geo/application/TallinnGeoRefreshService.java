package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
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
public class TallinnGeoRefreshService {

    private final TallinnGeoSourcePort tallinnGeoSourcePort;
    private final GeoCacheIngestService geoCacheIngestService;
    private final GeoIngestProperties geoIngestProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TallinnGeoRefreshService(
            TallinnGeoSourcePort tallinnGeoSourcePort,
            GeoCacheIngestService geoCacheIngestService,
            GeoIngestProperties geoIngestProperties
    ) {
        this.tallinnGeoSourcePort = tallinnGeoSourcePort;
        this.geoCacheIngestService = geoCacheIngestService;
        this.geoIngestProperties = geoIngestProperties;
    }

    public GeoCacheIngestStatus refreshFromConfiguredSource() {
        String sourceUrl = geoIngestProperties.getTallinnSourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalStateException("geo.ingest.tallinn-source-url is not configured");
        }

        String payload = tallinnGeoSourcePort.fetchGeoJson(sourceUrl.trim());
        List<TallinnLayerCacheEntry> entries = parseEntries(payload);
        return geoCacheIngestService.ingestTallinnLayer(geoIngestProperties.getTallinnSourceLayer(), entries);
    }

    private List<TallinnLayerCacheEntry> parseEntries(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode features = root.path("features");
            if (!features.isArray()) {
                return List.of();
            }

            List<TallinnLayerCacheEntry> entries = new ArrayList<>();
            int index = 0;
            for (JsonNode feature : features) {
                TallinnLayerCacheEntry entry = toEntry(feature, index++);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Tallinn GeoJSON payload", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private TallinnLayerCacheEntry toEntry(JsonNode feature, int fallbackIndex) {
        JsonNode properties = feature.path("properties");
        JsonNode geometry = feature.path("geometry");
        if (!geometry.isObject()) {
            return null;
        }

        Map<String, Object> propertiesMap = properties.isObject()
                ? objectMapper.convertValue(properties, Map.class)
                : Map.of();
        String sourceId = firstNonBlank(
                valueAsString(properties.get(geoIngestProperties.getTallinnFeatureIdProperty())),
                valueAsString(feature.get("id")),
                valueAsString(properties.get("OBJECTID")),
                valueAsString(properties.get("gid")),
                "feature-" + fallbackIndex
        );
        String name = firstNonBlank(
                valueAsString(properties.get(geoIngestProperties.getTallinnFeatureNameProperty())),
                valueAsString(properties.get("name")),
                valueAsString(properties.get("nimetus"))
        );
        String wkt = geometryToWkt(geometry);
        if (wkt == null) {
            return null;
        }

        return new TallinnLayerCacheEntry(
                geoIngestProperties.getTallinnSourceLayer(),
                sourceId,
                name,
                propertiesMap,
                wkt,
                feature.toString()
        );
    }

    private String geometryToWkt(JsonNode geometry) {
        String type = valueAsString(geometry.get("type"));
        JsonNode coordinates = geometry.get("coordinates");
        if (type == null || coordinates == null) {
            return null;
        }

        return switch (type) {
            case "Point" -> pointToWkt(coordinates);
            case "LineString" -> lineStringToWkt(coordinates);
            case "MultiLineString" -> multiLineStringToWkt(coordinates);
            case "Polygon" -> polygonToWkt(coordinates);
            case "MultiPolygon" -> multiPolygonToWkt(coordinates);
            default -> null;
        };
    }

    private String pointToWkt(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.size() < 2) {
            return null;
        }
        return "POINT(" + coordinates.get(0).asText() + " " + coordinates.get(1).asText() + ")";
    }

    private String lineStringToWkt(JsonNode coordinates) {
        List<String> points = coordinateList(coordinates);
        if (points.size() < 2) {
            return null;
        }
        return "LINESTRING(" + String.join(",", points) + ")";
    }

    private String multiLineStringToWkt(JsonNode coordinates) {
        if (!coordinates.isArray()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode line : coordinates) {
            List<String> points = coordinateList(line);
            if (points.size() >= 2) {
                lines.add("(" + String.join(",", points) + ")");
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return "MULTILINESTRING(" + String.join(",", lines) + ")";
    }

    private String polygonToWkt(JsonNode coordinates) {
        if (!coordinates.isArray()) {
            return null;
        }
        List<String> rings = new ArrayList<>();
        for (JsonNode ring : coordinates) {
            List<String> points = coordinateList(ring);
            if (points.size() >= 3) {
                closeRing(points);
                rings.add("(" + String.join(",", points) + ")");
            }
        }
        if (rings.isEmpty()) {
            return null;
        }
        return "POLYGON(" + String.join(",", rings) + ")";
    }

    private String multiPolygonToWkt(JsonNode coordinates) {
        if (!coordinates.isArray()) {
            return null;
        }
        List<String> polygons = new ArrayList<>();
        for (JsonNode polygon : coordinates) {
            String polygonWkt = polygonToWkt(polygon);
            if (polygonWkt != null) {
                polygons.add("(" + polygonWkt.substring("POLYGON(".length(), polygonWkt.length() - 1) + ")");
            }
        }
        if (polygons.isEmpty()) {
            return null;
        }
        return "MULTIPOLYGON(" + String.join(",", polygons) + ")";
    }

    private List<String> coordinateList(JsonNode coordinates) {
        List<String> points = new ArrayList<>();
        if (!coordinates.isArray()) {
            return points;
        }
        for (JsonNode point : coordinates) {
            if (!point.isArray() || point.size() < 2) {
                continue;
            }
            points.add(point.get(0).asText() + " " + point.get(1).asText());
        }
        return points;
    }

    private void closeRing(List<String> points) {
        if (!points.isEmpty() && !points.getFirst().equals(points.getLast())) {
            points.add(points.getFirst());
        }
    }

    private String valueAsString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
