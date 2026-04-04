package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCachePort;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCachePort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoCacheIngestServiceTest {

    @Test
    void ingestOsmFeaturesUpsertsAndTracksSnapshot() {
        List<OsmFeatureCacheEntry> upsertedEntries = new ArrayList<>();
        List<String> snapshotSources = new ArrayList<>();

        OsmFeatureCachePort osmPort = upsertedEntries::add;
        TallinnLayerCachePort tallinnPort = entry -> {
        };
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
        GeoCacheIngestService service = new GeoCacheIngestService(osmPort, tallinnPort, snapshotPort);

        GeoCacheIngestStatus status = service.ingestOsmFeatures(List.of(
                new OsmFeatureCacheEntry("way/1", "Track A", "cycleway", Map.of("surface", "asphalt"), "LINESTRING(24.7 59.4,24.8 59.41)", "{}")
        ));

        assertTrue(status.successful());
        assertEquals(1, status.upsertedCount());
        assertEquals(1, upsertedEntries.size());
        assertEquals(List.of("osm_geofabrik_estonia"), snapshotSources);
    }

    @Test
    void ingestTallinnLayerNormalizesSourceLayer() {
        List<TallinnLayerCacheEntry> upsertedEntries = new ArrayList<>();

        OsmFeatureCachePort osmPort = entry -> {
        };
        TallinnLayerCachePort tallinnPort = upsertedEntries::add;
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
        GeoCacheIngestService service = new GeoCacheIngestService(osmPort, tallinnPort, snapshotPort);

        GeoCacheIngestStatus status = service.ingestTallinnLayer("bike_network", List.of(
                new TallinnLayerCacheEntry("ignored", "id-1", "Segment 1", Map.of(), "LINESTRING(24.71 59.42,24.72 59.43)", "{}")
        ));

        assertTrue(status.successful());
        assertEquals(1, status.upsertedCount());
        assertEquals("bike_network", upsertedEntries.getFirst().sourceLayer());
    }
}
