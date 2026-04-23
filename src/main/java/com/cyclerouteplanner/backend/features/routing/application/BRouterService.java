package com.cyclerouteplanner.backend.features.routing.application;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Calls the BRouter routing engine and returns a cycling route as GeoJSON.
 *
 * How it works:
 *   Takes a start and end point, sends them to BRouter, and gets back the
 *   route as GeoJSON (a standard format that map libraries can display).
 *
 * Where BRouter lives:
 *   The URL is not hardcoded here. A pre-configured HTTP client
 *   ({@code brouterRestClient}) is injected by Spring — its base URL comes
 *   from {@code routing.brouter.base-url} in application.yml so we can
 *   switch between the public server and a self-hosted instance without
 *   changing any code.
 *
 * @see com.cyclerouteplanner.backend.features.routing.infra.BRouterConfiguration
 * @see com.cyclerouteplanner.backend.features.routing.infra.BRouterProperties
 */
@Service
public class BRouterService {

    private final RestClient brouterRestClient;

    public BRouterService(RestClient brouterRestClient) {
        this.brouterRestClient = brouterRestClient;
    }

    public String getRoute(double startLat, double startLon, double endLat, double endLon) {
        return getRoute(startLat, startLon, endLat, endLon, "fastbike");
    }

    public String getRoute(double startLat, double startLon, double endLat, double endLon, String profile) {
        String lonlats = startLon + "," + startLat + "|" + endLon + "," + endLat;

        return brouterRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("lonlats", lonlats)
                        .queryParam("profile", profile)
                        .queryParam("alternativeidx", 0)
                        .queryParam("format", "geojson")
                        .build())
                .retrieve()
                .body(String.class);
    }
}
