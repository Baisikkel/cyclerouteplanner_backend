package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.domain.OsmGeoSourcePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Locale;

@Component
@Profile({"local", "docker"})
public class OverpassOsmGeoSourceClient implements OsmGeoSourcePort {

    private final RestClient overpassRestClient;

    public OverpassOsmGeoSourceClient(@Qualifier("overpassRestClient") RestClient overpassRestClient) {
        this.overpassRestClient = overpassRestClient;
    }

    @Override
    public String fetchCycleNetwork(double south, double west, double north, double east, int timeoutSeconds) {
        String bbox = String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", south, west, north, east);
        String query = """
                [out:json][timeout:%d];
                (
                  relation["type"="route"]["route"="bicycle"](%s);
                )->.bikeRoutes;
                (
                  way["highway"~"cycleway|path|track|residential|living_street|service|unclassified|tertiary|tertiary_link|secondary|secondary_link|primary|primary_link|road|footway|pedestrian|bridleway|steps"]
                     ["access"!~"no|private"]
                     ["bicycle"!~"no|private"](%s);
                  way["bicycle"~"yes|designated|permissive"]["highway"](%s);
                  way["cycleway"](%s);
                  way["cycleway:left"](%s);
                  way["cycleway:right"](%s);
                  way(r.bikeRoutes);
                )->.bikeWays;
                (
                  node["barrier"](%s);
                )->.barrierNodes;
                .bikeRoutes out body;
                .bikeWays out body geom;
                .barrierNodes out body;
                """.formatted(timeoutSeconds, bbox, bbox, bbox, bbox, bbox, bbox, bbox);

        return overpassRestClient.post()
                .uri("/interpreter")
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(String.class);
    }
}
