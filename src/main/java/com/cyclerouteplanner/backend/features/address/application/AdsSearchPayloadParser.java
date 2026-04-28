package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.core.util.TextUtils;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdsSearchPayloadParser {

    private static final String ADDRESSES = "addresses";
    private static final String RESULTS = "results";
    private static final String ITEMS = "items";
    private static final String COORDINATES = "coordinates";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AdsAddressSuggestion> parseSuggestions(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode candidates = findCandidateArray(root);

            List<AdsAddressSuggestion> suggestions = new ArrayList<>();
            for (JsonNode node : candidates) {
                AdsAddressSuggestion suggestion = toSuggestion(node);
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
            return suggestions;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse ADS search response", ex);
        }
    }

    private JsonNode findCandidateArray(JsonNode root) {
        if (root != null && root.isArray()) {
            return root;
        }
        if (root != null && root.has(ADDRESSES) && root.get(ADDRESSES).isArray()) {
            return root.get(ADDRESSES);
        }
        if (root != null && root.has(RESULTS) && root.get(RESULTS).isArray()) {
            return root.get(RESULTS);
        }
        if (root != null && root.has(ITEMS) && root.get(ITEMS).isArray()) {
            return root.get(ITEMS);
        }
        return objectMapper.createArrayNode();
    }

    private AdsAddressSuggestion toSuggestion(JsonNode node) {
        String id = firstText(node, "ads_oid", "adr_id", "adob_id", "tunnus", "id", "oid");
        String address = firstText(node, "aadresstekst", "lahiaadress", "address", "full_address", "taisaadress", "pikkaadress", "nimi");
        String label = firstText(node, "ipikkaadress", "taisaadress", "pikkaadress", "full_address", "address", "aadresstekst", "nimi");
        Double latitude = firstNumber(node, "lat", "latitude", "viitepunkt_b");
        Double longitude = firstNumber(node, "lon", "lng", "longitude", "viitepunkt_l");

        if ((latitude == null || longitude == null) && node.has("point")) {
            JsonNode point = node.get("point");
            if (point.has(COORDINATES) && point.get(COORDINATES).isArray() && point.get(COORDINATES).size() >= 2) {
                longitude = point.get(COORDINATES).get(0).asDouble();
                latitude = point.get(COORDINATES).get(1).asDouble();
            }
        }

        if (TextUtils.isBlank(id) || TextUtils.isBlank(label) || latitude == null || longitude == null) {
            return null;
        }

        return new AdsAddressSuggestion(
                id,
                label,
                address,
                firstText(node, "asustusyksus", "asum", "vaikekoht"),
                firstText(node, "omavalitsus", "municipality"),
                firstText(node, "maakond", "county"),
                latitude,
                longitude
        );
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (!TextUtils.isBlank(text)) {
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
                } catch (NumberFormatException _) {
                    // Continue scanning other candidate fields.
                }
            }
        }
        return null;
    }

}
