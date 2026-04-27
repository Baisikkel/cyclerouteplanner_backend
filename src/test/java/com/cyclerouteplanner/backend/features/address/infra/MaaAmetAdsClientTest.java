package com.cyclerouteplanner.backend.features.address.infra;

import com.cyclerouteplanner.backend.features.address.application.AdsSearchPayloadParser;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

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
        MaaAmetAdsClient client = new MaaAmetAdsClient(restClient, properties, new AdsSearchPayloadParser());

        server.expect(requestTo("https://ads.example/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        String response = client.fetchStatus();

        assertEquals("OK", response);
        server.verify();
    }

    @Test
    void searchCallsConfiguredPathAndParamsAndMapsSuggestions() {
        AdsClientProperties properties = new AdsClientProperties();
        properties.setBaseUrl("https://ads.example");
        properties.setSearchPath("/api/search");
        properties.setSearchQueryParam("q");
        properties.setSearchLimitParam("limit");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MaaAmetAdsClient client = new MaaAmetAdsClient(restClient, properties, new AdsSearchPayloadParser());

        server.expect(requestTo("https://ads.example/api/search?q=tartu&limit=3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "ads_oid": "ME01087725",
                              "ipikkaadress": "Mustamäe tee 51, Tallinn",
                              "aadresstekst": "Mustamäe tee 51",
                              "viitepunkt_b": 59.421047,
                              "viitepunkt_l": 24.697966
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<AdsAddressSuggestion> response = client.search("tartu", 3);

        assertEquals(1, response.size());
        assertEquals("ME01087725", response.getFirst().id());
        server.verify();
    }

    @Test
    void searchRawCallsConfiguredPathAndParams() {
        AdsClientProperties properties = new AdsClientProperties();
        properties.setBaseUrl("https://ads.example");
        properties.setSearchPath("/api/search");
        properties.setSearchQueryParam("q");
        properties.setSearchLimitParam("limit");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MaaAmetAdsClient client = new MaaAmetAdsClient(restClient, properties, new AdsSearchPayloadParser());

        server.expect(requestTo("https://ads.example/api/search?q=tartu&limit=3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        String response = client.searchRaw("tartu", 3);

        assertEquals("{\"items\":[]}", response);
        server.verify();
    }
}
