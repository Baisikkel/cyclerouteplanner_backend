package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import com.cyclerouteplanner.backend.features.address.domain.AdsSearchStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            public String search(String query, int limit) {
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
    void searchReturnsUnavailableWhenGatewayThrows() {
        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "";
            }

            @Override
            public String search(String query, int limit) {
                throw new IllegalStateException("ADS unavailable");
            }
        };
        AdsService service = new AdsService(gateway);

        AdsSearchStatus status = service.search("tartu", 5);

        assertFalse(status.reachable());
        assertEquals("maa-amet-ads", status.provider());
        assertEquals("tartu", status.query());
        assertEquals(5, status.limit());
        assertEquals("ADS unavailable", status.details());
        assertNull(status.payloadSnippet());
        assertNotNull(status.checkedAt());
    }
}
