package com.cyclerouteplanner.backend.features.osm.api;

import com.cyclerouteplanner.backend.features.osm.api.dto.response.OsmConnectivityResponse;
import com.cyclerouteplanner.backend.features.osm.application.OsmConnectivityService;
import com.cyclerouteplanner.backend.features.osm.domain.OsmConnectivityStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/osm")
public class OsmConnectivityController {

    private final OsmConnectivityService osmConnectivityService;

    public OsmConnectivityController(OsmConnectivityService osmConnectivityService) {
        this.osmConnectivityService = osmConnectivityService;
    }

    @GetMapping("/connectivity")
    public ResponseEntity<OsmConnectivityResponse> connectivity() {
        OsmConnectivityStatus status = osmConnectivityService.checkConnectivity();
        OsmConnectivityResponse response = new OsmConnectivityResponse(
                status.provider(),
                status.reachable(),
                status.details(),
                status.checkedAt()
        );

        if (status.reachable()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
