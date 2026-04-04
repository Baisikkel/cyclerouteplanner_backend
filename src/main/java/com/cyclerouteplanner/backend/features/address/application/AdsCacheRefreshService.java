package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCacheEntry;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCachePort;
import com.cyclerouteplanner.backend.features.address.domain.AdsCacheRefreshStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class AdsCacheRefreshService {

    private static final String PROVIDER = "maa-amet-ads";
    private final AdsGatewayPort adsGatewayPort;
    private final AdsAddressCachePort adsAddressCachePort;
    private final DataSnapshotPort dataSnapshotPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdsCacheRefreshService(
            AdsGatewayPort adsGatewayPort,
            AdsAddressCachePort adsAddressCachePort,
            DataSnapshotPort dataSnapshotPort
    ) {
        this.adsGatewayPort = adsGatewayPort;
        this.adsAddressCachePort = adsAddressCachePort;
        this.dataSnapshotPort = dataSnapshotPort;
    }

    public AdsCacheRefreshStatus refresh(String query, int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, 500));
        try {
            String payload = adsGatewayPort.search(query, sanitizedLimit);
            int upsertedCount = upsertAddresses(payload);

            dataSnapshotPort.upsert(
                    "maaamet_inads",
                    LocalDate.now(ZoneOffset.UTC).toString(),
                    Instant.now(),
                    null,
                    Map.of(
                            "query", query,
                            "limit", sanitizedLimit,
                            "upsertedCount", upsertedCount
                    )
            );

            return new AdsCacheRefreshStatus(
                    true,
                    PROVIDER,
                    query,
                    sanitizedLimit,
                    upsertedCount,
                    "Address cache refresh completed",
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            return new AdsCacheRefreshStatus(
                    false,
                    PROVIDER,
                    query,
                    sanitizedLimit,
                    0,
                    ex.getMessage(),
                    Instant.now()
            );
        }
    }

    private int upsertAddresses(String payload) {
        List<AdsAddressCacheEntry> entries = parseEntries(payload);
        for (AdsAddressCacheEntry entry : entries) {
            adsAddressCachePort.upsert(entry);
        }
        return entries.size();
    }

    private List<AdsAddressCacheEntry> parseEntries(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode candidates = findCandidateArray(root);

            List<AdsAddressCacheEntry> entries = new ArrayList<>();
            for (JsonNode node : candidates) {
                AdsAddressCacheEntry entry = toEntry(node);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse ADS payload", ex);
        }
    }

    private JsonNode findCandidateArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (root.has("results") && root.get("results").isArray()) {
            return root.get("results");
        }
        if (root.has("addresses") && root.get("addresses").isArray()) {
            return root.get("addresses");
        }
        if (root.has("items") && root.get("items").isArray()) {
            return root.get("items");
        }
        return objectMapper.createArrayNode();
    }

    private AdsAddressCacheEntry toEntry(JsonNode node) {
        String adsOid = firstText(node, "ads_oid", "oid", "id", "tunnus");
        String fullAddress = firstText(node, "full_address", "address", "koodaadress", "nimi");
        if (isBlank(adsOid) || isBlank(fullAddress)) {
            return null;
        }

        String etakCode = firstText(node, "etak_code", "etakood", "etak");
        Double longitude = firstNumber(node, "lon", "lng", "x");
        Double latitude = firstNumber(node, "lat", "y");

        if ((longitude == null || latitude == null) && node.has("point")) {
            JsonNode point = node.get("point");
            if (point.has("coordinates") && point.get("coordinates").isArray() && point.get("coordinates").size() >= 2) {
                longitude = point.get("coordinates").get(0).asDouble();
                latitude = point.get("coordinates").get(1).asDouble();
            }
        }

        return new AdsAddressCacheEntry(
                adsOid,
                fullAddress,
                normalizeAddress(fullAddress),
                etakCode,
                longitude,
                latitude,
                node.toString()
        );
    }

    private String normalizeAddress(String input) {
        return input == null ? null : input.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (!isBlank(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private Double firstNumber(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
            if (value != null && value.isTextual()) {
                try {
                    return Double.parseDouble(value.asText());
                } catch (NumberFormatException ignored) {
                    // Continue scanning other candidate fields.
                }
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
