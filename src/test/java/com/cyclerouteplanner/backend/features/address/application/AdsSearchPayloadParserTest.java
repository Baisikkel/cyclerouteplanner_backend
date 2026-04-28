package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdsSearchPayloadParserTest {

    @Test
    void parseSuggestionsMapsMaaAmetPayloadToFrontendReadyDomainObjects() {
        AdsSearchPayloadParser parser = new AdsSearchPayloadParser();

        List<AdsAddressSuggestion> suggestions = parser.parseSuggestions("""
                {
                  "addresses": [
                    {
                      "pikkaadress": "Harju maakond, Tallinn, Kristiine linnaosa, Mustamäe tee 51",
                      "ipikkaadress": "Mustamäe tee 51, Kristiine linnaosa, Tallinn, Harju maakond",
                      "aadresstekst": "Mustamäe tee 51",
                      "ads_oid": "ME01087725",
                      "maakond": "Harju maakond",
                      "omavalitsus": "Tallinn",
                      "asustusyksus": "Kristiine linnaosa",
                      "viitepunkt_l": "24.697966",
                      "viitepunkt_b": "59.421047"
                    },
                    { "ads_oid": "missing-coordinates", "aadresstekst": "Skipped" }
                  ]
                }
                """);

        assertEquals(1, suggestions.size());
        AdsAddressSuggestion suggestion = suggestions.getFirst();
        assertEquals("ME01087725", suggestion.id());
        assertEquals("Mustamäe tee 51, Kristiine linnaosa, Tallinn, Harju maakond", suggestion.label());
        assertEquals("Mustamäe tee 51", suggestion.address());
        assertEquals("Kristiine linnaosa", suggestion.settlement());
        assertEquals("Tallinn", suggestion.municipality());
        assertEquals("Harju maakond", suggestion.county());
        assertEquals(59.421047, suggestion.latitude());
        assertEquals(24.697966, suggestion.longitude());
    }
}
