package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.domain.TallinnGeoSourcePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile({"local", "docker"})
public class HttpTallinnGeoSourceClient implements TallinnGeoSourcePort {

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public String fetchGeoJson(String sourceUrl) {
        return restClient.get()
                .uri(sourceUrl)
                .retrieve()
                .body(String.class);
    }
}
