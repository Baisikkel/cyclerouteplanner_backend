package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.infra.GeoIngestProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Profile({ "local", "docker" })
public class OsmGeoRefreshService {

    private final OsmGeoSourcePort osmGeoSourcePort;
    private final GeoCacheIngestService geoCacheIngestService;
    private final GeoIngestProperties geoIngestProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OsmGeoRefreshService(
            OsmGeoSourcePort osmGeoSourcePort,
            GeoCacheIngestService geoCacheIngestService,
            GeoIngestProperties geoIngestProperties) {
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
                geoIngestProperties.getOverpassTimeoutSeconds());
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

            Map<Long, Set<String>> wayRouteNetworks = collectWayRouteNetworks(elements);
            List<OsmFeatureCacheEntry> entries = new ArrayList<>();
            for (JsonNode element : elements) {
                OsmFeatureCacheEntry entry = toEntry(element, wayRouteNetworks);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Overpass payload", ex);
        }
    }

    private OsmFeatureCacheEntry toEntry(JsonNode element, Map<Long, Set<String>> wayRouteNetworks) {
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
        Map<String, Object> sourceTags = tagsNode.isObject()
                ? objectMapper.convertValue(
                        tagsNode,
                        new TypeReference<Map<String, Object>>() {
                            // Use a TypeReference so Jackson preserves the generic type information
                            // when converting the JsonNode to a Map<String, Object>.
                            // This avoids the unchecked raw Map.class conversion warning.
                        })
                : Map.of();
        Map<String, Object> tags = new HashMap<>(sourceTags);
        if ("way".equals(type)) {
            addRouteNetworkTags(idNode.asLong(), tags, wayRouteNetworks);
        }

        String name = tags.get("name") instanceof String value ? value : null;
        String featureType = tags.get("highway") instanceof String value
                ? value
                : tags.get("barrier") instanceof String ? "barrier" : type;

        return new OsmFeatureCacheEntry(
                type + "/" + idNode.asText(),
                name,
                featureType,
                tags,
                wktGeometry,
                element.toString());
    }

    private Map<Long, Set<String>> collectWayRouteNetworks(JsonNode elements) {
        Map<Long, Set<String>> wayRouteNetworks = new HashMap<>();
        for (JsonNode element : elements) {
            if (!"relation".equals(element.path("type").asText())) {
                continue;
            }

            JsonNode tagsNode = element.path("tags");
            if (!"bicycle".equals(tagsNode.path("route").asText())) {
                continue;
            }

            Set<String> networks = extractRouteNetworks(tagsNode);
            if (networks.isEmpty()) {
                continue;
            }

            JsonNode members = element.path("members");
            if (!members.isArray()) {
                continue;
            }

            for (JsonNode member : members) {
                if (!"way".equals(member.path("type").asText())) {
                    continue;
                }
                JsonNode refNode = member.get("ref");
                if (refNode == null || !refNode.canConvertToLong()) {
                    continue;
                }
                wayRouteNetworks.computeIfAbsent(refNode.asLong(), ignored -> new HashSet<>()).addAll(networks);
            }
        }
        return wayRouteNetworks;
    }

    private Set<String> extractRouteNetworks(JsonNode relationTags) {
        Set<String> networks = new HashSet<>();
        addNetworkFromValue(networks, relationTags.path("network").asText(null));
        addNetworkFromFlag(networks, relationTags, "icn");
        addNetworkFromFlag(networks, relationTags, "ncn");
        addNetworkFromFlag(networks, relationTags, "rcn");
        addNetworkFromFlag(networks, relationTags, "lcn");
        return networks;
    }

    private void addNetworkFromValue(Set<String> networks, String networkValue) {
        if (networkValue == null || networkValue.isBlank()) {
            return;
        }
        switch (networkValue) {
            case "icn", "international" -> networks.add("icn");
            case "ncn", "national" -> networks.add("ncn");
            case "rcn", "regional" -> networks.add("rcn");
            case "lcn", "local" -> networks.add("lcn");
            default -> {
                // Ignore unsupported network labels.
            }
        }
    }

    private void addNetworkFromFlag(Set<String> networks, JsonNode relationTags, String network) {
        if ("yes".equalsIgnoreCase(relationTags.path(network).asText())) {
            networks.add(network);
        }
    }

    private void addRouteNetworkTags(long wayId, Map<String, Object> tags, Map<Long, Set<String>> wayRouteNetworks) {
        Set<String> networks = wayRouteNetworks.get(wayId);
        if (networks == null || networks.isEmpty()) {
            return;
        }
        for (String network : networks) {
            tags.put("route_bicycle_" + network, "yes");
        }
    }

    private String extractWkt(JsonNode element) {
        if (element.has("lat") && element.has("lon")) {
            return "POINT(%s %s)".formatted(
                    element.get("lon").asText(),
                    element.get("lat").asText());
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
