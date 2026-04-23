package com.cyclerouteplanner.backend.features.geo.api;

import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheIngestResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheStatusResponse;
import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAuditService;
import com.cyclerouteplanner.backend.features.geo.application.GeoCacheStatusService;
import com.cyclerouteplanner.backend.features.geo.application.OsmGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.application.TallinnGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "docker"})
@RequestMapping("/api/geo/cache")
public class GeoCacheIngestController {

    private final GeoCacheStatusService geoCacheStatusService;
    private final GeoRoutingAuditService geoRoutingAuditService;
    private final OsmGeoRefreshService osmGeoRefreshService;
    private final TallinnGeoRefreshService tallinnGeoRefreshService;

    public GeoCacheIngestController(
            GeoCacheStatusService geoCacheStatusService,
            GeoRoutingAuditService geoRoutingAuditService,
            OsmGeoRefreshService osmGeoRefreshService,
            TallinnGeoRefreshService tallinnGeoRefreshService
    ) {
        this.geoCacheStatusService = geoCacheStatusService;
        this.geoRoutingAuditService = geoRoutingAuditService;
        this.osmGeoRefreshService = osmGeoRefreshService;
        this.tallinnGeoRefreshService = tallinnGeoRefreshService;
    }

    @GetMapping("/status")
    public ResponseEntity<GeoCacheStatusResponse> status() {
        return ResponseEntity.ok(geoCacheStatusService.status());
    }

    @GetMapping("/routing-audit")
    public ResponseEntity<GeoRoutingAuditResponse> routingAudit() {
        return ResponseEntity.ok(geoRoutingAuditService.audit());
    }

    @PostMapping("/osm/refresh")
    public ResponseEntity<GeoCacheIngestResponse> refreshOsmFromOverpass() {
        GeoCacheIngestStatus status = osmGeoRefreshService.refreshTallinnCycleNetwork();
        return toResponse(status);
    }

    @PostMapping("/tallinn/refresh")
    public ResponseEntity<GeoCacheIngestResponse> refreshTallinnFromConfiguredSource() {
        GeoCacheIngestStatus status = tallinnGeoRefreshService.refreshFromConfiguredSource();
        return toResponse(status);
    }

    private ResponseEntity<GeoCacheIngestResponse> toResponse(GeoCacheIngestStatus status) {
        GeoCacheIngestResponse response = new GeoCacheIngestResponse(
                status.source(),
                status.successful(),
                status.requestedCount(),
                status.upsertedCount(),
                status.details(),
                status.checkedAt()
        );
        if (status.successful()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
