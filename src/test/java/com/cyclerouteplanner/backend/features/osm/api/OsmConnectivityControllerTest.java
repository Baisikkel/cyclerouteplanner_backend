package com.cyclerouteplanner.backend.features.osm.api;

import com.cyclerouteplanner.backend.features.osm.application.OsmConnectivityService;
import com.cyclerouteplanner.backend.features.osm.domain.OsmConnectivityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OsmConnectivityControllerTest {

    @Test
    void connectivityReturnsOkWhenOsmIsReachable() throws Exception {
        OsmConnectivityService osmConnectivityService = mock(OsmConnectivityService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OsmConnectivityController(osmConnectivityService)).build();
        when(osmConnectivityService.checkConnectivity())
                .thenReturn(new OsmConnectivityStatus(true, "overpass", "Connected as: 1", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/osm/connectivity").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("overpass"))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.details").value("Connected as: 1"));
    }

    @Test
    void connectivityReturnsServiceUnavailableWhenOsmIsDown() throws Exception {
        OsmConnectivityService osmConnectivityService = mock(OsmConnectivityService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OsmConnectivityController(osmConnectivityService)).build();
        when(osmConnectivityService.checkConnectivity())
                .thenReturn(new OsmConnectivityStatus(false, "overpass", "Overpass unavailable", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/osm/connectivity").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.provider").value("overpass"))
                .andExpect(jsonPath("$.reachable").value(false))
                .andExpect(jsonPath("$.details").value("Overpass unavailable"));
    }
}
