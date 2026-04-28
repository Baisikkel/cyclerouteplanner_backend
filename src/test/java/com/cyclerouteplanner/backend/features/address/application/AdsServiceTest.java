package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdsServiceTest {

    @Test
    void checkConnectivityReturnsReachableWhenGatewayResponds() {
        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "ADS OK";
            }

            @Override
            public List<AdsAddressSuggestion> search(String query, int limit) {
                return List.of();
            }

            @Override
            public String searchRaw(String query, int limit) {
                return "";
            }
        };
        AdsService service = new AdsService(gateway);

        AdsConnectivityStatus status = service.checkConnectivity();

        assertTrue(status.reachable());
        assertEquals("maa-amet-ads", status.provider());
        assertEquals("ADS OK", status.details());
        assertNotNull(status.checkedAt());
    }

    @Test
    void searchReturnsGatewaySuggestionsForValidatedRequest() {
        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "OK";
            }

            @Override
            public List<AdsAddressSuggestion> search(String query, int limit) {
                return List.of(new AdsAddressSuggestion(
                        "ME01087725",
                        "Mustamäe tee 51, Kristiine linnaosa, Tallinn, Harju maakond",
                        "Mustamäe tee 51",
                        "Kristiine linnaosa",
                        "Tallinn",
                        "Harju maakond",
                        59.421047,
                        24.697966
                ));
            }

            @Override
            public String searchRaw(String query, int limit) {
                return "";
            }
        };
        AdsService service = new AdsService(gateway);

        List<AdsAddressSuggestion> suggestions = service.search(" mustamäe ", 5);

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

    @Test
    void searchRejectsBlankQuery() {
        AdsService service = new AdsService(new NoopGateway());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.search(" ", 5));

        assertEquals("query must not be blank", ex.getMessage());
    }

    @Test
    void searchRejectsTooShortQuery() {
        AdsService service = new AdsService(new NoopGateway());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.search("t", 5));

        assertEquals("query must be at least 2 characters", ex.getMessage());
    }

    @Test
    void searchRejectsOutOfRangeLimit() {
        AdsService service = new AdsService(new NoopGateway());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.search("tartu", 11));

        assertEquals("limit must be between 1 and 10", ex.getMessage());
    }

    @Test
    void searchPropagatesGatewayFailures() {
        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "OK";
            }

            @Override
            public List<AdsAddressSuggestion> search(String query, int limit) {
                throw new IllegalStateException("ADS unavailable");
            }

            @Override
            public String searchRaw(String query, int limit) {
                return "";
            }
        };
        AdsService service = new AdsService(gateway);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.search("tartu", 5));

        assertEquals("ADS unavailable", ex.getMessage());
    }

    private static final class NoopGateway implements AdsGatewayPort {
        @Override
        public String fetchStatus() {
            return "OK";
        }

        @Override
        public List<AdsAddressSuggestion> search(String query, int limit) {
            return List.of();
        }

        @Override
        public String searchRaw(String query, int limit) {
            return "{\"addresses\":[]}";
        }
    }
}
