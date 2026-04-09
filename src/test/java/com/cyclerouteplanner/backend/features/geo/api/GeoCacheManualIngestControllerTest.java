package com.cyclerouteplanner.backend.features.geo.api;

import com.cyclerouteplanner.backend.features.geo.application.GeoCacheIngestService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeoCacheManualIngestControllerTest {

    @Test
    void ingestOsmReturnsOk() throws Exception {
        GeoCacheIngestService service = mock(GeoCacheIngestService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheManualIngestController(service)
        ).build();

        when(service.ingestOsmFeatures(anyList())).thenReturn(new GeoCacheIngestStatus(
                true,
                "osm_geofabrik_estonia",
                1,
                1,
                "Geo cache ingest completed",
                Instant.parse("2026-04-04T00:00:00Z")
        ));

        String body = """
                [
                  {
                    "sourceId": "way/1",
                    "name": "Track A",
                    "featureType": "cycleway",
                    "tags": {"surface":"asphalt"},
                    "wktGeometry": "LINESTRING(24.7 59.4,24.8 59.41)",
                    "rawPayload": {"id":"way/1"}
                  }
                ]
                """;

        mockMvc.perform(post("/api/geo/cache/osm/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("osm_geofabrik_estonia"))
                .andExpect(jsonPath("$.upsertedCount").value(1));
    }

    @Test
    void ingestTallinnReturnsServiceUnavailableOnFailure() throws Exception {
        GeoCacheIngestService service = mock(GeoCacheIngestService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new GeoCacheManualIngestController(service)
        ).build();

        when(service.ingestTallinnLayer(eq("bike_network"), anyList())).thenReturn(new GeoCacheIngestStatus(
                false,
                "tallinn_open_data",
                1,
                0,
                "Invalid geometry",
                Instant.parse("2026-04-04T00:00:00Z")
        ));

        String body = """
                [
                  {
                    "sourceId": "segment-1",
                    "name": "Segment 1",
                    "properties": {"type":"cycleway"},
                    "wktGeometry": "LINESTRING(24.71 59.42,24.72 59.43)",
                    "rawPayload": {"id":"segment-1"}
                  }
                ]
                """;

        mockMvc.perform(post("/api/geo/cache/tallinn/ingest")
                        .param("sourceLayer", "bike_network")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.source").value("tallinn_open_data"))
                .andExpect(jsonPath("$.details").value("Invalid geometry"));
    }
}
