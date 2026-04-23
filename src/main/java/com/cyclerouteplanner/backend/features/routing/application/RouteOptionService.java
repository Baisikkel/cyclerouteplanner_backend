package com.cyclerouteplanner.backend.features.routing.application;

import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionPort;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class RouteOptionService {

    private static final String SNAPSHOT_SOURCE = "routing_options";
    private static final String OPTIONS_SOURCE = "osm_with_optional_tallinn_enrichment";

    private final RouteOptionPort routeOptionPort;
    private final DataSnapshotPort dataSnapshotPort;

    public RouteOptionService(RouteOptionPort routeOptionPort, DataSnapshotPort dataSnapshotPort) {
        this.routeOptionPort = routeOptionPort;
        this.dataSnapshotPort = dataSnapshotPort;
    }

    public RouteOptionRefreshStatus refreshFromGeoCaches() {
        try {
            int upsertedCount = routeOptionPort.rebuildFromGeoCaches();
            dataSnapshotPort.upsert(
                    SNAPSHOT_SOURCE,
                    LocalDate.now(ZoneOffset.UTC).toString(),
                    Instant.now(),
                    null,
                    Map.of("upsertedCount", upsertedCount, "strategy", "osm_base_tallinn_optional")
            );
            return new RouteOptionRefreshStatus(
                    true,
                    OPTIONS_SOURCE,
                    upsertedCount,
                    "Route options refresh completed",
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            return new RouteOptionRefreshStatus(
                    false,
                    OPTIONS_SOURCE,
                    0,
                    ex.getMessage(),
                    Instant.now()
            );
        }
    }

    public List<RouteOptionRecord> activeOptions(int limit) {
        int sanitizedLimit = Math.clamp(limit, 1, 500);
        return routeOptionPort.findActive(sanitizedLimit);
    }
}
