package com.cyclerouteplanner.backend.features.ingest.api.dto.response;

import java.time.Instant;
import java.util.Map;

public record DataSnapshotResponse(
        String source,
        String sourceVersion,
        Instant sourceTimestamp,
        String checksum,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
