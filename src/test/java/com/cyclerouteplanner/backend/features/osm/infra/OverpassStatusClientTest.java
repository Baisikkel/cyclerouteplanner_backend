package com.cyclerouteplanner.backend.features.osm.infra;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OverpassStatusClientTest {

    @Test
    void fetchStatusCallsExpectedEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://overpass.example");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        OverpassStatusClient client = new OverpassStatusClient(restClient);

        server.expect(requestTo("https://overpass.example/status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Connected as: 123", MediaType.TEXT_PLAIN));

        String response = client.fetchStatus();

        assertEquals("Connected as: 123", response);
        server.verify();
    }
}
