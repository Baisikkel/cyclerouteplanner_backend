package com.cyclerouteplanner.backend.features.address.infra;

import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MaaAmetAdsClient implements AdsGatewayPort {

    private final RestClient adsRestClient;
    private final AdsClientProperties properties;

    public MaaAmetAdsClient(RestClient adsRestClient, AdsClientProperties properties) {
        this.adsRestClient = adsRestClient;
        this.properties = properties;
    }

    @Override
    public String fetchStatus() {
        return adsRestClient.get()
                .uri(properties.getStatusPath())
                .retrieve()
                .body(String.class);
    }

    @Override
    public String search(String query, int limit) {
        return adsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.getSearchPath())
                        .queryParam(properties.getSearchQueryParam(), query)
                        .queryParam(properties.getSearchLimitParam(), limit)
                        .build())
                .retrieve()
                .body(String.class);
    }
}
