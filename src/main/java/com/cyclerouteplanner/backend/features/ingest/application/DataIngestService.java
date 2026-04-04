package com.cyclerouteplanner.backend.features.ingest.application;

import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class DataIngestService {

    private final DataSnapshotPort dataSnapshotPort;

    public DataIngestService(DataSnapshotPort dataSnapshotPort) {
        this.dataSnapshotPort = dataSnapshotPort;
    }

    /**
     * Skeleton ingest flow that records freshness markers for planned data providers.
     */
    public List<DataSnapshotRecord> runSkeletonIngest() {
        String sourceVersion = LocalDate.now(ZoneOffset.UTC).toString();
        Instant sourceTimestamp = Instant.now();

        DataSnapshotRecord osmSnapshot = dataSnapshotPort.upsert(
                "osm_geofabrik_estonia",
                sourceVersion,
                sourceTimestamp,
                null,
                Map.of("stage", "skeleton", "status", "not-downloaded")
        );
        DataSnapshotRecord tallinnSnapshot = dataSnapshotPort.upsert(
                "tallinn_open_data",
                sourceVersion,
                sourceTimestamp,
                null,
                Map.of("stage", "skeleton", "status", "not-downloaded")
        );
        DataSnapshotRecord adsSnapshot = dataSnapshotPort.upsert(
                "maaamet_inads",
                sourceVersion,
                sourceTimestamp,
                null,
                Map.of("stage", "skeleton", "status", "not-downloaded")
        );

        return List.of(osmSnapshot, tallinnSnapshot, adsSnapshot);
    }

    public List<DataSnapshotRecord> latestSnapshots(int limit) {
        int sanitizedLimit = Math.max(1, Math.min(limit, 100));
        return dataSnapshotPort.findLatest(sanitizedLimit);
    }
}
