package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeoRoutingEdgeBuildServiceTest {

    @Test
    void rebuildFromGeoCachesReturnsCountsAndWritesSnapshot() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Map.of(
                        "osm_upserted", 540,
                        "osm_plus_tallinn_upserted", 220,
                        "tallinn_only_upserted", 70
                ));

        List<String> snapshotSources = new ArrayList<>();
        DataSnapshotPort snapshotPort = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(String source, String sourceVersion, Instant sourceTimestamp, String checksum, Map<String, Object> metadata) {
                snapshotSources.add(source);
                return new DataSnapshotRecord(source, sourceVersion, sourceTimestamp, checksum, metadata, Instant.now());
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                return List.of();
            }
        };

        GeoRoutingEdgeBuildService service = new GeoRoutingEdgeBuildService(jdbcTemplate, snapshotPort);
        GeoRoutingEdgeBuildStatus status = service.rebuildFromGeoCaches();

        assertTrue(status.successful());
        assertEquals(540, status.osmUpsertedCount());
        assertEquals(220, status.osmPlusTallinnUpsertedCount());
        assertEquals(70, status.tallinnOnlyUpsertedCount());
        assertEquals(610, status.totalUpsertedCount());
        assertEquals(List.of("routing_edges"), snapshotSources);
    }

    @Test
    void statusReturnsCurrentActiveCounts() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Map.of(
                        "active_total_count", 1200,
                        "active_osm_count", 700,
                        "active_osm_plus_tallinn_count", 380,
                        "active_tallinn_only_count", 120
                ));

        DataSnapshotPort snapshotPort = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(String source, String sourceVersion, Instant sourceTimestamp, String checksum, Map<String, Object> metadata) {
                return new DataSnapshotRecord(source, sourceVersion, sourceTimestamp, checksum, metadata, Instant.now());
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                return List.of();
            }
        };

        GeoRoutingEdgeBuildService service = new GeoRoutingEdgeBuildService(jdbcTemplate, snapshotPort);
        GeoRoutingEdgeStatus status = service.status();

        assertEquals(1200, status.activeTotalCount());
        assertEquals(700, status.activeOsmCount());
        assertEquals(380, status.activeOsmPlusTallinnCount());
        assertEquals(120, status.activeTallinnOnlyCount());
    }
}
