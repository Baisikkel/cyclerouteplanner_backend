package com.cyclerouteplanner.backend.features.ingest.domain;

import java.time.Instant;
import java.util.Map;

public record DataSnapshotRecord(
        String source,
        String sourceVersion,
        Instant sourceTimestamp,
        String checksum,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
