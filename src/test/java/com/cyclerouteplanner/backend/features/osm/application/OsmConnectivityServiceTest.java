package com.cyclerouteplanner.backend.features.osm.application;

import com.cyclerouteplanner.backend.features.osm.domain.OsmConnectivityStatus;
import com.cyclerouteplanner.backend.features.osm.domain.OsmStatusPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmConnectivityServiceTest {

    @Test
    void checkConnectivityReturnsReachableWhenPortResponds() {
        OsmStatusPort port = () -> "Connected as: 12345";
        OsmConnectivityService service = new OsmConnectivityService(port);

        OsmConnectivityStatus status = service.checkConnectivity();

        assertTrue(status.reachable());
        assertEquals("overpass", status.provider());
        assertEquals("Connected as: 12345", status.details());
        assertNotNull(status.checkedAt());
    }

    @Test
    void checkConnectivityReturnsUnavailableWhenPortThrows() {
        OsmStatusPort port = () -> {
            throw new IllegalStateException("Overpass unavailable");
        };
        OsmConnectivityService service = new OsmConnectivityService(port);

        OsmConnectivityStatus status = service.checkConnectivity();

        assertFalse(status.reachable());
        assertEquals("overpass", status.provider());
        assertEquals("Overpass unavailable", status.details());
        assertNotNull(status.checkedAt());
    }
}
