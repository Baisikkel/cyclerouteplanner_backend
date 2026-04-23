package com.cyclerouteplanner.backend.features.routing.application;

import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionPort;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteOptionServiceTest {

    @Test
    void refreshFromGeoCachesBuildsOptionsAndWritesSnapshot() {
        List<String> snapshotSources = new ArrayList<>();
        RouteOptionPort routeOptionPort = new RouteOptionPort() {
            @Override
            public int rebuildFromGeoCaches() {
                return 12;
            }

            @Override
            public List<RouteOptionRecord> findActive(int limit) {
                return List.of();
            }
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

        RouteOptionService service = new RouteOptionService(routeOptionPort, snapshotPort);

        RouteOptionRefreshStatus status = service.refreshFromGeoCaches();

        assertTrue(status.successful());
        assertEquals(12, status.upsertedCount());
        assertEquals(List.of("routing_options"), snapshotSources);
    }

    @Test
    void activeOptionsClampsLimit() {
        List<Integer> capturedLimits = new ArrayList<>();
        RouteOptionPort routeOptionPort = new RouteOptionPort() {
            @Override
            public int rebuildFromGeoCaches() {
                return 0;
            }

            @Override
            public List<RouteOptionRecord> findActive(int limit) {
                capturedLimits.add(limit);
                return List.of(new RouteOptionRecord(
                        1L,
                        "way/1",
                        "Sample",
                        "fastbike",
                        0.8,
                        "osm_only",
                        "LINESTRING(24.7 59.4,24.8 59.41)"
                ));
            }
        };
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

        RouteOptionService service = new RouteOptionService(routeOptionPort, snapshotPort);
        List<RouteOptionRecord> options = service.activeOptions(0);

        assertEquals(1, options.size());
        assertEquals(List.of(1), capturedLimits);
    }
}
