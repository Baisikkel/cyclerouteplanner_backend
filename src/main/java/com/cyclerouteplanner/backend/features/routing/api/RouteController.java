package com.cyclerouteplanner.backend.features.routing.api;

import com.cyclerouteplanner.backend.features.routing.application.BRouterService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
public class RouteController {

    private final BRouterService brouterService;

    public RouteController(BRouterService brouterService) {
        this.brouterService = brouterService;
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
}
