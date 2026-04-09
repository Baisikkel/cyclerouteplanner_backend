package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.api.dto.response.AdsConnectivityResponse;
import com.cyclerouteplanner.backend.features.address.api.dto.response.AdsSearchResponse;
import com.cyclerouteplanner.backend.features.address.application.AdsService;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsSearchStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address")
public class AdsController {

    private final AdsService adsService;

    public AdsController(AdsService adsService) {
        this.adsService = adsService;
    }

    /**
     * Lightweight health-style probe for Maa-amet ADS availability used during integration setup and diagnostics.
     */
    @GetMapping("/connectivity")
    public ResponseEntity<AdsConnectivityResponse> connectivity() {
        AdsConnectivityStatus status = adsService.checkConnectivity();
        AdsConnectivityResponse response = new AdsConnectivityResponse(
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

    @GetMapping("/search")
    public ResponseEntity<AdsSearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        AdsSearchStatus status = adsService.search(query, limit);
        AdsSearchResponse response = new AdsSearchResponse(
                status.provider(),
                status.reachable(),
                status.query(),
                status.limit(),
                status.details(),
                status.payloadSnippet(),
                status.checkedAt()
        );

        if (status.reachable()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
