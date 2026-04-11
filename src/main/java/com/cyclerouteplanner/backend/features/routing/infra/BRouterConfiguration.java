package com.cyclerouteplanner.backend.features.routing.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires up everything BRouterService needs to talk to the BRouter engine.
 *
 * What it does:
 *   Activates {@link BRouterProperties} (so Spring reads the URL from config)
 *   and creates a ready-to-use HTTP client with that URL baked in.
 *
 * Why a separate file:
 *   Keeps wiring logic out of the service. BRouterService only cares about
 *   "call BRouter and get a route" — it never needs to know the URL or how
 *   the HTTP client was built. This follows the same pattern as
 *   {@code AdsClientConfiguration} and {@code OverpassClientConfiguration}.
 */
@Configuration
@EnableConfigurationProperties(BRouterProperties.class)
public class BRouterConfiguration {

    /**
     * Creates an HTTP client pre-configured with the BRouter base URL.
     * Spring passes this client to any constructor that asks for a
     * {@code RestClient} named {@code brouterRestClient} (e.g. BRouterService).
     */
    @Bean
    RestClient brouterRestClient(BRouterProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
