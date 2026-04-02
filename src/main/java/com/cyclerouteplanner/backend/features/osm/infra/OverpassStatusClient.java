package com.cyclerouteplanner.backend.features.osm.infra;

import com.cyclerouteplanner.backend.features.osm.domain.OsmStatusPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OverpassStatusClient implements OsmStatusPort {

    private final RestClient overpassRestClient;

    public OverpassStatusClient(RestClient overpassRestClient) {
        this.overpassRestClient = overpassRestClient;
    }

    @Override
    public String fetchStatus() {
        return overpassRestClient.get()
                .uri("/status")
                .retrieve()
                .body(String.class);
    }
}
