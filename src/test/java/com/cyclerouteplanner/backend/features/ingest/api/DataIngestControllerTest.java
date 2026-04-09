package com.cyclerouteplanner.backend.features.ingest.api;

import com.cyclerouteplanner.backend.features.ingest.application.DataIngestService;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataIngestControllerTest {

    @Test
    void runSkeletonIngestReturnsSnapshots() throws Exception {
        DataIngestService service = mock(DataIngestService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DataIngestController(service)).build();
        when(service.runSkeletonIngest()).thenReturn(List.of(
                new DataSnapshotRecord(
                        "osm_geofabrik_estonia",
                        "2026-04-02",
                        Instant.parse("2026-04-02T00:00:00Z"),
                        null,
                        Map.of("stage", "skeleton"),
                        Instant.parse("2026-04-02T00:00:10Z")
                )
        ));

        mockMvc.perform(post("/api/ingest/run").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("osm_geofabrik_estonia"))
                .andExpect(jsonPath("$[0].sourceVersion").value("2026-04-02"));
    }

    @Test
    void latestSnapshotsReturnsConfiguredLimit() throws Exception {
        DataIngestService service = mock(DataIngestService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DataIngestController(service)).build();
        when(service.latestSnapshots(10)).thenReturn(List.of(
                new DataSnapshotRecord(
                        "tallinn_open_data",
                        "2026-04-02",
                        Instant.parse("2026-04-02T00:00:00Z"),
                        null,
                        Map.of("stage", "skeleton"),
                        Instant.parse("2026-04-02T00:00:10Z")
                )
        ));

        mockMvc.perform(get("/api/ingest/snapshots")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("tallinn_open_data"))
                .andExpect(jsonPath("$[0].sourceVersion").value("2026-04-02"));
    }
}
