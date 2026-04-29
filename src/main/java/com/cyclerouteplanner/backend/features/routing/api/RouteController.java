package com.cyclerouteplanner.backend.features.routing.api;

import com.cyclerouteplanner.backend.features.routing.api.dto.request.RouteCalculationRequest;
import com.cyclerouteplanner.backend.features.routing.api.dto.request.RouteWaypointRequest;
import com.cyclerouteplanner.backend.features.routing.application.RouteCalculationResult;
import com.cyclerouteplanner.backend.features.routing.application.RouteCalculationService;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.api.dto.response.RouteOptionRefreshResponse;
import com.cyclerouteplanner.backend.features.routing.api.dto.response.RouteOptionResponse;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import com.cyclerouteplanner.backend.features.routing.domain.RouteWaypoint;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://baisikkel.models.ee"}, allowCredentials = "true")
@Profile({"local", "docker"})
public class RouteController {

    private final RouteCalculationService routeCalculationService;
    private final RouteOptionService routeOptionService;

    public RouteController(RouteCalculationService routeCalculationService, RouteOptionService routeOptionService) {
        this.routeCalculationService = routeCalculationService;
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
            @RequestParam double endLon,
            @RequestParam(required = false) String profile) {
        try {
            RouteCalculationResult result = routeCalculationService.calculate(startLat, startLon, endLat, endLon, profile);
            return routeResponse(result);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(value = "/calculate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> calculateRoute(@RequestBody RouteCalculationRequest request) {
        try {
            RouteCalculationResult result = routeCalculationService.calculate(
                    toWaypoints(request),
                    request == null ? null : request.profile()
            );
            return routeResponse(result);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
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

    private ResponseEntity<byte[]> routeResponse(RouteCalculationResult result) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Route-Profile", result.resolvedProfile())
                .body(result.geoJson().getBytes(StandardCharsets.UTF_8));
    }

    private List<RouteWaypoint> toWaypoints(RouteCalculationRequest request) {
        if (request == null || request.waypoints() == null) {
            return null;
        }
        return request.waypoints().stream()
                .map(this::toWaypoint)
                .toList();
    }

    private RouteWaypoint toWaypoint(RouteWaypointRequest waypoint) {
        if (waypoint == null) {
            return null;
        }
        return new RouteWaypoint(waypoint.lat(), waypoint.lon());
    }
}
