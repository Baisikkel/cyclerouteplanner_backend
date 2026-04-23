package com.cyclerouteplanner.backend.features.routing.api;

import com.cyclerouteplanner.backend.features.routing.application.BRouterService;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.api.dto.response.RouteOptionRefreshResponse;
import com.cyclerouteplanner.backend.features.routing.api.dto.response.RouteOptionResponse;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
/*
 * CORS for local SPA (Vite on e.g. :5173) calling this API on :8080.
 *
 * The web app uses axios withCredentials=true (see cycle_route_planner_web src/api/client.ts).
 * Browsers reject Access-Control-Allow-Origin: * for credentialed cross-origin requests, which
 * surfaces as "Network Error" with no HTTP status in the frontend — not a missing "connection".
 *
 * originPatterns + allowCredentials lets Spring echo a concrete Origin (e.g. http://localhost:5173).
 * Widen patterns for staging/production when you know real front-end origins; avoid * with credentials.
 */
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
@Profile({"local", "docker"})
public class RouteController {

    private final BRouterService brouterService;
    private final RouteOptionService routeOptionService;

    public RouteController(BRouterService brouterService, RouteOptionService routeOptionService) {
        this.brouterService = brouterService;
        this.routeOptionService = routeOptionService;
    }

    /**
     * Proxies BRouter GeoJSON so the browser only talks to this backend (raw JSON bytes avoid
     * double-encoding a String as a JSON quoted string).
     */
    @GetMapping(value = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> calculateRoute(
            @RequestParam double startLat,
            @RequestParam double startLon,
            @RequestParam double endLat,
            @RequestParam double endLon) {
        String body = brouterService.getRoute(startLat, startLon, endLat, endLon);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/options/refresh")
    public ResponseEntity<RouteOptionRefreshResponse> refreshRouteOptions() {
        RouteOptionRefreshStatus status = routeOptionService.refreshFromGeoCaches();
        RouteOptionRefreshResponse response = new RouteOptionRefreshResponse(
                status.source(),
                status.successful(),
                status.upsertedCount(),
                status.details(),
                status.checkedAt()
        );
        if (status.successful()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/options")
    public List<RouteOptionResponse> listRouteOptions(@RequestParam(defaultValue = "100") int limit) {
        return routeOptionService.activeOptions(limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private RouteOptionResponse toResponse(RouteOptionRecord option) {
        return new RouteOptionResponse(
                option.id(),
                option.sourceId(),
                option.name(),
                option.profileHint(),
                option.qualityScore(),
                option.enrichmentType(),
                option.wktGeometry()
        );
    }
}
