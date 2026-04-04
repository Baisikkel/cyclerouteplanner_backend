package com.cyclerouteplanner.backend.features.ingest.application;

import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataIngestServiceTest {

    @Test
    void runSkeletonIngestTracksAllConfiguredSources() {
        List<String> upsertedSources = new ArrayList<>();
        DataSnapshotPort port = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(
                    String source,
                    String sourceVersion,
                    Instant sourceTimestamp,
                    String checksum,
                    Map<String, Object> metadata
            ) {
                upsertedSources.add(source);
                return new DataSnapshotRecord(
                        source,
                        sourceVersion,
                        sourceTimestamp,
                        checksum,
                        metadata,
                        Instant.now()
                );
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                return List.of();
            }
        };

        DataIngestService service = new DataIngestService(port);

        List<DataSnapshotRecord> snapshots = service.runSkeletonIngest();

        assertEquals(3, snapshots.size());
        assertTrue(upsertedSources.contains("osm_geofabrik_estonia"));
        assertTrue(upsertedSources.contains("tallinn_open_data"));
        assertTrue(upsertedSources.contains("maaamet_inads"));
    }

    @Test
    void latestSnapshotsBoundsLimitToAllowedRange() {
        List<Integer> capturedLimits = new ArrayList<>();
        DataSnapshotPort port = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(
                    String source,
                    String sourceVersion,
                    Instant sourceTimestamp,
                    String checksum,
                    Map<String, Object> metadata
            ) {
                throw new UnsupportedOperationException("not needed");
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                capturedLimits.add(limit);
                return List.of();
            }
        };

        DataIngestService service = new DataIngestService(port);
        service.latestSnapshots(0);
        service.latestSnapshots(200);

        assertEquals(List.of(1, 100), capturedLimits);
    }
}
