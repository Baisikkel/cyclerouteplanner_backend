package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.cyclerouteplanner.backend.features.geo.infra.GeoIngestProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

        List<TallinnLayerCacheEntry> entries = fetchEntries(sourceUrl.trim());
        return geoCacheIngestService.ingestTallinnLayer(geoIngestProperties.getTallinnSourceLayer(), entries);
    }

    private List<TallinnLayerCacheEntry> fetchEntries(String sourceUrl) {
        if (!isArcGisQueryUrl(sourceUrl)) {
            String payload = tallinnGeoSourcePort.fetchGeoJson(sourceUrl);
            return parseEntries(payload, 0).entries();
        }

        int pageSize = positiveOrDefault(geoIngestProperties.getTallinnPageSize(), 2000);
        int maxPages = positiveOrDefault(geoIngestProperties.getTallinnMaxPages(), 100);
        List<TallinnLayerCacheEntry> allEntries = new ArrayList<>();
        Set<String> seenSourceIds = new HashSet<>();
        Set<Integer> seenPayloadHashes = new HashSet<>();
        int offset = 0;

        for (int page = 0; page < maxPages; page++) {
            String pageUrl = withArcGisPagination(sourceUrl, offset, pageSize);
            String payload = tallinnGeoSourcePort.fetchGeoJson(pageUrl);
            int payloadHash = payload.hashCode();
            if (!seenPayloadHashes.add(payloadHash)) {
                break;
            }

            ParsedTallinnPayload parsed = parseEntries(payload, offset);
            if (parsed.featureCount() == 0) {
                break;
            }

            for (TallinnLayerCacheEntry entry : parsed.entries()) {
                if (seenSourceIds.add(entry.sourceId())) {
                    allEntries.add(entry);
                }
            }

            boolean likelyMoreData = parsed.exceededTransferLimit() || parsed.featureCount() >= pageSize;
            if (!likelyMoreData) {
                break;
            }
            offset += parsed.featureCount();

            if (page == maxPages - 1) {
                throw new IllegalStateException(
                        "Tallinn source pagination limit reached before completion. Increase geo.ingest.tallinn-max-pages.");
            }
        }

        return allEntries;
    }

    private ParsedTallinnPayload parseEntries(String payload, int fallbackStartIndex) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode error = root.path("error");
            if (error.isObject()) {
                String message = firstNonBlank(
                        valueAsString(error.get("message")),
                        valueAsString(error.get("details")),
                        "Unknown source error"
                );
                throw new IllegalStateException("Tallinn source returned error: " + message);
            }
            JsonNode features = root.path("features");
            if (!features.isArray()) {
                return new ParsedTallinnPayload(List.of(), 0, root.path("exceededTransferLimit").asBoolean(false));
            }

            int featureCount = features.size();
            boolean exceededTransferLimit = root.path("exceededTransferLimit").asBoolean(false);
            List<TallinnLayerCacheEntry> entries = new ArrayList<>();
            int index = 0;
            for (JsonNode feature : features) {
                TallinnLayerCacheEntry entry = toEntry(feature, fallbackStartIndex + index++);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return new ParsedTallinnPayload(entries, featureCount, exceededTransferLimit);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Tallinn GeoJSON payload", ex);
        }
    }

    private boolean isArcGisQueryUrl(String sourceUrl) {
        String normalized = sourceUrl.toLowerCase(Locale.ROOT);
        return normalized.contains("/arcgis/rest/") && normalized.contains("/query");
    }

    private String withArcGisPagination(String sourceUrl, int offset, int pageSize) {
        return UriComponentsBuilder.fromUriString(sourceUrl)
                .replaceQueryParam("f", "geojson")
                .replaceQueryParam("resultOffset", offset)
                .replaceQueryParam("resultRecordCount", pageSize)
                .build(true)
                .toUriString();
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
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

    private record ParsedTallinnPayload(
            List<TallinnLayerCacheEntry> entries,
            int featureCount,
            boolean exceededTransferLimit
    ) {
    }
}
