package com.cyclerouteplanner.backend.features.ingest.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface DataSnapshotPort {

    DataSnapshotRecord upsert(
            String source,
            String sourceVersion,
            Instant sourceTimestamp,
            String checksum,
            Map<String, Object> metadata
    );

    List<DataSnapshotRecord> findLatest(int limit);
}
