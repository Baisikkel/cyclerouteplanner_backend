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

    private static final String STAGE = "stage";
    private static final String STATUS = "status";
    private static final String SKELETON = "skeleton";
    private static final String NOT_DOWNLOADED = "not-downloaded";

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
                Map.of(STAGE, SKELETON, STATUS, NOT_DOWNLOADED)
        );
        DataSnapshotRecord tallinnSnapshot = dataSnapshotPort.upsert(
                "tallinn_open_data",
                sourceVersion,
                sourceTimestamp,
                null,
                Map.of(STAGE, SKELETON, STATUS, NOT_DOWNLOADED)
        );
        DataSnapshotRecord adsSnapshot = dataSnapshotPort.upsert(
                "maaamet_inads",
                sourceVersion,
                sourceTimestamp,
                null,
                Map.of(STAGE, SKELETON, STATUS, NOT_DOWNLOADED)
        );

        return List.of(osmSnapshot, tallinnSnapshot, adsSnapshot);
    }

    public List<DataSnapshotRecord> latestSnapshots(int limit) {
        int sanitizedLimit = Math.clamp(limit, 1, 100);
        return dataSnapshotPort.findLatest(sanitizedLimit);
    }
}
