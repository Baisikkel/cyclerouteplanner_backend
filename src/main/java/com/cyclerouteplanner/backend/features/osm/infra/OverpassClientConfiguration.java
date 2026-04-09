package com.cyclerouteplanner.backend.features.osm.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OverpassClientProperties.class)
public class OverpassClientConfiguration {

    @Bean
    RestClient.Builder overpassRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient overpassRestClient(RestClient.Builder overpassRestClientBuilder, OverpassClientProperties properties) {
        return overpassRestClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
