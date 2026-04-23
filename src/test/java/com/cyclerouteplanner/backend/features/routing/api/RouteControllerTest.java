package com.cyclerouteplanner.backend.features.routing.api;

import com.cyclerouteplanner.backend.features.routing.application.BRouterService;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
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

class RouteControllerTest {

    @Test
    void calculateRouteReturnsBrouterPayload() throws Exception {
        BRouterService bRouterService = mock(BRouterService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(bRouterService, routeOptionService)
        ).build();
        when(bRouterService.getRoute(59.43, 24.72, 59.44, 24.73))
                .thenReturn("{\"type\":\"FeatureCollection\",\"features\":[]}");

        mockMvc.perform(get("/api/routes/calculate")
                        .param("startLat", "59.43")
                        .param("startLon", "24.72")
                        .param("endLat", "59.44")
                        .param("endLon", "24.73"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void refreshOptionsReturnsStatusPayload() throws Exception {
        BRouterService bRouterService = mock(BRouterService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(bRouterService, routeOptionService)
        ).build();
        when(routeOptionService.refreshFromGeoCaches()).thenReturn(new RouteOptionRefreshStatus(
                true,
                "osm_with_optional_tallinn_enrichment",
                15,
                "Route options refresh completed",
                Instant.parse("2026-04-22T12:00:00Z")
        ));

        mockMvc.perform(post("/api/routes/options/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("osm_with_optional_tallinn_enrichment"))
                .andExpect(jsonPath("$.upsertedCount").value(15));
    }

    @Test
    void listOptionsReturnsCurrentRouteOptions() throws Exception {
        BRouterService bRouterService = mock(BRouterService.class);
        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteController(bRouterService, routeOptionService)
        ).build();
        when(routeOptionService.activeOptions(2)).thenReturn(List.of(
                new RouteOptionRecord(
                        7L,
                        "way/1001",
                        "Bike Street",
                        "fastbike",
                        0.85,
                        "osm_plus_tallinn",
                        "LINESTRING(24.72 59.43,24.73 59.44)"
                )
        ));

        mockMvc.perform(get("/api/routes/options").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value("way/1001"))
                .andExpect(jsonPath("$[0].profileHint").value("fastbike"))
                .andExpect(jsonPath("$[0].enrichmentType").value("osm_plus_tallinn"));
    }
}
