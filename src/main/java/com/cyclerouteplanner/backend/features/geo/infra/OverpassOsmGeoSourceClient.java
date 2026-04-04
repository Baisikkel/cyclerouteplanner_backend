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
                  way["highway"="cycleway"](%s);
                  way["cycleway"](%s);
                );
                out body geom;
                """.formatted(timeoutSeconds, bbox, bbox);

        return overpassRestClient.post()
                .uri("/interpreter")
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(String.class);
    }
}
