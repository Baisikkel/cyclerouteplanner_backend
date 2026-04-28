package com.cyclerouteplanner.backend.features.routing.application;

import com.cyclerouteplanner.backend.features.routing.domain.RouteWaypoint;
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

class BRouterServiceTest {

    @Test
    void getRouteBuildsLonlatsForOrderedWaypoints() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brouter.example/brouter");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        BRouterService service = new BRouterService(restClient);

        server.expect(requestTo("https://brouter.example/brouter?lonlats=24.72,59.43%7C24.73,59.44%7C24.74,59.45&profile=fastbike&alternativeidx=0&format=geojson"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"type\":\"FeatureCollection\",\"features\":[]}", MediaType.APPLICATION_JSON));

        String response = service.getRoute(List.of(
                new RouteWaypoint(59.43, 24.72),
                new RouteWaypoint(59.44, 24.73),
                new RouteWaypoint(59.45, 24.74)
        ), "fastbike");

        assertEquals("{\"type\":\"FeatureCollection\",\"features\":[]}", response);
        server.verify();
    }

    @Test
    void buildLonlatsUsesBrouterLonLatOrder() {
        RestClient restClient = RestClient.builder().baseUrl("https://brouter.example/brouter").build();
        BRouterService service = new BRouterService(restClient);

        String lonlats = service.buildLonlats(List.of(
                new RouteWaypoint(59.43, 24.72),
                new RouteWaypoint(59.44, 24.73),
                new RouteWaypoint(59.45, 24.74)
        ));

        assertEquals("24.72,59.43|24.73,59.44|24.74,59.45", lonlats);
    }
}
