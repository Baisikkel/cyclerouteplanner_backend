package com.cyclerouteplanner.backend.features.geo.api;

import com.cyclerouteplanner.backend.features.geo.application.GeoCacheStatusService;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheSourceStatusResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheStatusResponse;
import com.cyclerouteplanner.backend.features.geo.application.OsmGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.application.TallinnGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeoCacheIngestControllerTest {

    @Test
    void statusReturnsCacheReadinessSnapshot() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, refreshService, tallinnRefreshService)
        ).build();

        when(statusService.status()).thenReturn(new GeoCacheStatusResponse(
                Instant.parse("2026-04-04T00:00:00Z"),
                new GeoCacheSourceStatusResponse("maaamet_inads", 100, Instant.parse("2026-04-04T00:00:00Z"), Instant.parse("2026-04-04T00:00:00Z")),
                new GeoCacheSourceStatusResponse("osm_geofabrik_estonia", 200, Instant.parse("2026-04-04T00:00:00Z"), Instant.parse("2026-04-04T00:00:00Z")),
                new GeoCacheSourceStatusResponse("tallinn_open_data", 300, Instant.parse("2026-04-04T00:00:00Z"), Instant.parse("2026-04-04T00:00:00Z"))
        ));

        mockMvc.perform(get("/api/geo/cache/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ads.cachedRowCount").value(100))
                .andExpect(jsonPath("$.osm.cachedRowCount").value(200))
                .andExpect(jsonPath("$.tallinn.cachedRowCount").value(300));
    }

    @Test
    void refreshOsmReturnsOk() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, refreshService, tallinnRefreshService)
        ).build();
        when(refreshService.refreshTallinnCycleNetwork()).thenReturn(new GeoCacheIngestStatus(
                true,
                "osm_geofabrik_estonia",
                42,
                39,
                "Geo cache ingest completed",
                Instant.parse("2026-04-04T00:00:00Z")
        ));

        mockMvc.perform(post("/api/geo/cache/osm/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("osm_geofabrik_estonia"))
                .andExpect(jsonPath("$.requestedCount").value(42))
                .andExpect(jsonPath("$.upsertedCount").value(39));
    }

    @Test
    void refreshTallinnReturnsOk() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, refreshService, tallinnRefreshService)
        ).build();
        when(tallinnRefreshService.refreshFromConfiguredSource()).thenReturn(new GeoCacheIngestStatus(
                true,
                "tallinn_open_data",
                20,
                20,
                "Geo cache ingest completed",
                Instant.parse("2026-04-04T00:00:00Z")
        ));

        mockMvc.perform(post("/api/geo/cache/tallinn/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("tallinn_open_data"))
                .andExpect(jsonPath("$.requestedCount").value(20))
                .andExpect(jsonPath("$.upsertedCount").value(20));
    }
}
