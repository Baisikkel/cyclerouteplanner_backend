package com.cyclerouteplanner.backend.features.address.infra;

import com.cyclerouteplanner.backend.features.address.application.AdsSearchPayloadParser;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MaaAmetAdsClient implements AdsGatewayPort {

    private final RestClient adsRestClient;
    private final AdsClientProperties properties;
    private final AdsSearchPayloadParser payloadParser;

    public MaaAmetAdsClient(RestClient adsRestClient, AdsClientProperties properties, AdsSearchPayloadParser payloadParser) {
        this.adsRestClient = adsRestClient;
        this.properties = properties;
        this.payloadParser = payloadParser;
    }

    @Override
    public String fetchStatus() {
        return adsRestClient.get()
                .uri(properties.getStatusPath())
                .retrieve()
                .body(String.class);
    }

    @Override
    public List<AdsAddressSuggestion> search(String query, int limit) {
        return payloadParser.parseSuggestions(searchRaw(query, limit));
    }

    @Override
    public String searchRaw(String query, int limit) {
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
