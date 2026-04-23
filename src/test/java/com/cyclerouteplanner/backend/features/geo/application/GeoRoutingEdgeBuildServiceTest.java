package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeExportStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingEdgeProperties;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

        GeoRoutingEdgeBuildService service = new GeoRoutingEdgeBuildService(jdbcTemplate, snapshotPort, defaultProperties("brouter/build-input/routing_edges.geojson"));
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

        GeoRoutingEdgeBuildService service = new GeoRoutingEdgeBuildService(jdbcTemplate, snapshotPort, defaultProperties("brouter/build-input/routing_edges.geojson"));
        GeoRoutingEdgeStatus status = service.status();

        assertEquals(1200, status.activeTotalCount());
        assertEquals(700, status.activeOsmCount());
        assertEquals(380, status.activeOsmPlusTallinnCount());
        assertEquals(120, status.activeTallinnOnlyCount());
    }

    @Test
    void exportActiveEdgesWritesGeoJsonBuildInput() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            RowCallbackHandler callback = invocation.getArgument(1);

            ResultSet row1 = mock(ResultSet.class);
            when(row1.getString("source_key")).thenReturn("osm:way/1");
            when(row1.getString("origin_source")).thenReturn("osm");
            when(row1.getString("osm_source_id")).thenReturn("way/1");
            when(row1.getString("tallinn_source_id")).thenReturn("tallinn_open_data/100");
            when(row1.getString("profile_hint")).thenReturn("fastbike");
            when(row1.getString("merge_type")).thenReturn("osm_plus_tallinn");
            when(row1.getDouble("quality_score")).thenReturn(0.92d);
            when(row1.getString("geom_geojson")).thenReturn("{\"type\":\"LineString\",\"coordinates\":[[24.7,59.4],[24.8,59.41]]}");
            when(row1.getString("metadata_json")).thenReturn("{\"featureType\":\"cycleway\"}");

            ResultSet row2 = mock(ResultSet.class);
            when(row2.getString("source_key")).thenReturn("tallinn:lane:200");
            when(row2.getString("origin_source")).thenReturn("tallinn");
            when(row2.getString("osm_source_id")).thenReturn(null);
            when(row2.getString("tallinn_source_id")).thenReturn("lane/200");
            when(row2.getString("profile_hint")).thenReturn("safety");
            when(row2.getString("merge_type")).thenReturn("tallinn_only");
            when(row2.getDouble("quality_score")).thenReturn(0.75d);
            when(row2.getString("geom_geojson")).thenReturn("{\"type\":\"LineString\",\"coordinates\":[[24.71,59.42],[24.72,59.43]]}");
            when(row2.getString("metadata_json")).thenReturn("{\"sourceLayer\":\"lane\"}");

            callback.processRow(row1);
            callback.processRow(row2);
            return null;
        }).when(jdbcTemplate).query(any(String.class), any(RowCallbackHandler.class));

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

        Path tempDirectory = Files.createTempDirectory("routing-edge-export-test");
        Path exportPath = tempDirectory.resolve("routing_edges.geojson");
        GeoRoutingEdgeBuildService service = new GeoRoutingEdgeBuildService(jdbcTemplate, snapshotPort, defaultProperties(exportPath.toString()));

        GeoRoutingEdgeExportStatus status = service.exportActiveEdgesToBrouterInput();
        String exported = Files.readString(exportPath, StandardCharsets.UTF_8);

        assertTrue(status.successful());
        assertEquals(2, status.exportedCount());
        assertTrue(exported.contains("\"type\":\"FeatureCollection\""));
        assertTrue(exported.contains("\"sourceKey\":\"osm:way/1\""));
        assertTrue(exported.contains("\"sourceKey\":\"tallinn:lane:200\""));
    }

    private GeoRoutingEdgeProperties defaultProperties(String outputPath) {
        GeoRoutingEdgeProperties properties = new GeoRoutingEdgeProperties();
        properties.setExportOutputPath(outputPath);
        return properties;
    }
}
