package com.cyclerouteplanner.backend.features.address.infra;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MaaAmetAdsClientTest {

    @Test
    void fetchStatusCallsConfiguredStatusPath() {
        AdsClientProperties properties = new AdsClientProperties();
        properties.setBaseUrl("https://ads.example");
        properties.setStatusPath("/health");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MaaAmetAdsClient client = new MaaAmetAdsClient(restClient, properties);

        server.expect(requestTo("https://ads.example/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        String response = client.fetchStatus();

        assertEquals("OK", response);
        server.verify();
    }

    @Test
    void searchCallsConfiguredPathAndParams() {
        AdsClientProperties properties = new AdsClientProperties();
        properties.setBaseUrl("https://ads.example");
        properties.setSearchPath("/api/search");
        properties.setSearchQueryParam("q");
        properties.setSearchLimitParam("limit");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MaaAmetAdsClient client = new MaaAmetAdsClient(restClient, properties);

        server.expect(requestTo("https://ads.example/api/search?q=tartu&limit=3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        String response = client.search("tartu", 3);

        assertEquals("{\"items\":[]}", response);
        server.verify();
    }
}
