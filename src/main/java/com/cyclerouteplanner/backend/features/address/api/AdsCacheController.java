package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.api.dto.response.AdsCacheRefreshResponse;
import com.cyclerouteplanner.backend.features.address.application.AdsCacheRefreshService;
import com.cyclerouteplanner.backend.features.address.domain.AdsCacheRefreshStatus;
import com.cyclerouteplanner.backend.features.address.infra.AdsClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address/cache")
@Profile({"local", "docker"})
public class AdsCacheController {

    private final AdsCacheRefreshService adsCacheRefreshService;
    private final AdsClientProperties adsClientProperties;

    public AdsCacheController(AdsCacheRefreshService adsCacheRefreshService, AdsClientProperties adsClientProperties) {
        this.adsCacheRefreshService = adsCacheRefreshService;
        this.adsClientProperties = adsClientProperties;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AdsCacheRefreshResponse> refresh(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit
    ) {
        String effectiveQuery = query == null || query.isBlank()
                ? adsClientProperties.getCacheRefreshDefaultQuery()
                : query;
        int effectiveLimit = limit == null
                ? adsClientProperties.getCacheRefreshDefaultLimit()
                : limit;

        AdsCacheRefreshStatus status = adsCacheRefreshService.refresh(effectiveQuery, effectiveLimit);
        AdsCacheRefreshResponse response = new AdsCacheRefreshResponse(
                status.provider(),
                status.reachable(),
                status.query(),
                status.limit(),
                status.upsertedCount(),
                status.details(),
                status.checkedAt()
        );

        if (status.reachable()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
