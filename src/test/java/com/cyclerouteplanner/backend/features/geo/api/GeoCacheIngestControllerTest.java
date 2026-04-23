package com.cyclerouteplanner.backend.features.geo.api;

import com.cyclerouteplanner.backend.features.geo.application.GeoCacheStatusService;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheSourceStatusResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditOsmResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditReadinessResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditTallinnResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheStatusResponse;
import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAuditService;
import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingEdgeBuildService;
import com.cyclerouteplanner.backend.features.geo.application.OsmGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.application.TallinnGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

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
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
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
    void routingAuditReturnsMergedGraphReadinessSnapshot() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
        ).build();

        when(routingAuditService.audit()).thenReturn(new GeoRoutingAuditResponse(
                Instant.parse("2026-04-23T00:00:00Z"),
                new GeoRoutingAuditOsmResponse(1000, 600, 420, 310, 280, 190, 260, 110),
                new GeoRoutingAuditTallinnResponse(400, 390, 390, 120, 1, 340),
                new GeoRoutingAuditReadinessResponse(
                        true,
                        true,
                        0.7,
                        0.8717948718,
                        List.of()
                )
        ));

        mockMvc.perform(get("/api/geo/cache/routing-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.osm.bikeCandidateLineRows").value(420))
                .andExpect(jsonPath("$.tallinn.lineRowsOverlappingOsm").value(340))
                .andExpect(jsonPath("$.readiness.readyForMergedOsmTallinnGraphBuild").value(true));
    }

    @Test
    void rebuildRoutingEdgesReturnsOk() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
        ).build();

        when(routingEdgeBuildService.rebuildFromGeoCaches()).thenReturn(new GeoRoutingEdgeBuildStatus(
                true,
                "osm_with_optional_tallinn_merge",
                500,
                210,
                80,
                580,
                "Routing edge rebuild completed",
                Instant.parse("2026-04-23T00:00:00Z")
        ));

        mockMvc.perform(post("/api/geo/cache/routing-edges/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.osmUpsertedCount").value(500))
                .andExpect(jsonPath("$.osmPlusTallinnUpsertedCount").value(210))
                .andExpect(jsonPath("$.tallinnOnlyUpsertedCount").value(80))
                .andExpect(jsonPath("$.totalUpsertedCount").value(580));
    }

    @Test
    void routingEdgeStatusReturnsCounts() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
        ).build();

        when(routingEdgeBuildService.status()).thenReturn(new GeoRoutingEdgeStatus(
                Instant.parse("2026-04-23T00:00:00Z"),
                1000,
                640,
                280,
                80
        ));

        mockMvc.perform(get("/api/geo/cache/routing-edges/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTotalCount").value(1000))
                .andExpect(jsonPath("$.activeOsmCount").value(640))
                .andExpect(jsonPath("$.activeOsmPlusTallinnCount").value(280))
                .andExpect(jsonPath("$.activeTallinnOnlyCount").value(80));
    }

    @Test
    void refreshOsmReturnsOk() throws Exception {
        GeoCacheStatusService statusService = mock(GeoCacheStatusService.class);
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
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
        GeoRoutingAuditService routingAuditService = mock(GeoRoutingAuditService.class);
        GeoRoutingEdgeBuildService routingEdgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        OsmGeoRefreshService refreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnRefreshService = mock(TallinnGeoRefreshService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheIngestController(statusService, routingAuditService, routingEdgeBuildService, refreshService, tallinnRefreshService)
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
