package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCachePort;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCachePort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class GeoCacheIngestService {

    private final OsmFeatureCachePort osmFeatureCachePort;
    private final TallinnLayerCachePort tallinnLayerCachePort;
    private final DataSnapshotPort dataSnapshotPort;

    public GeoCacheIngestService(
            OsmFeatureCachePort osmFeatureCachePort,
            TallinnLayerCachePort tallinnLayerCachePort,
            DataSnapshotPort dataSnapshotPort
    ) {
        this.osmFeatureCachePort = osmFeatureCachePort;
        this.tallinnLayerCachePort = tallinnLayerCachePort;
        this.dataSnapshotPort = dataSnapshotPort;
    }

    public GeoCacheIngestStatus ingestOsmFeatures(List<OsmFeatureCacheEntry> entries) {
        return ingest(
                "osm_geofabrik_estonia",
                entries == null ? 0 : entries.size(),
                () -> {
                    int count = 0;
                    if (entries != null) {
                        for (OsmFeatureCacheEntry entry : entries) {
                            osmFeatureCachePort.upsert(entry);
                            count++;
                        }
                    }
                    return count;
                }
        );
    }

    public GeoCacheIngestStatus ingestTallinnLayer(String sourceLayer, List<TallinnLayerCacheEntry> entries) {
        String safeLayer = sourceLayer == null || sourceLayer.isBlank() ? "unknown" : sourceLayer.trim();
        return ingest(
                "tallinn_open_data",
                entries == null ? 0 : entries.size(),
                () -> {
                    int count = 0;
                    if (entries != null) {
                        for (TallinnLayerCacheEntry entry : entries) {
                            TallinnLayerCacheEntry effectiveEntry = new TallinnLayerCacheEntry(
                                    safeLayer,
                                    entry.sourceId(),
                                    entry.name(),
                                    entry.properties(),
                                    entry.wktGeometry(),
                                    entry.rawPayload()
                            );
                            tallinnLayerCachePort.upsert(effectiveEntry);
                            count++;
                        }
                    }
                    return count;
                }
        );
    }

    private GeoCacheIngestStatus ingest(String source, int requestedCount, IngestAction action) {
        try {
            int upsertedCount = action.execute();
            dataSnapshotPort.upsert(
                    source,
                    LocalDate.now(ZoneOffset.UTC).toString(),
                    Instant.now(),
                    null,
                    Map.of("requestedCount", requestedCount, "upsertedCount", upsertedCount)
            );
            return new GeoCacheIngestStatus(
                    true,
                    source,
                    requestedCount,
                    upsertedCount,
                    "Geo cache ingest completed",
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            return new GeoCacheIngestStatus(
                    false,
                    source,
                    requestedCount,
                    0,
                    ex.getMessage(),
                    Instant.now()
            );
        }
    }

    @FunctionalInterface
    private interface IngestAction {
        int execute();
    }
}
