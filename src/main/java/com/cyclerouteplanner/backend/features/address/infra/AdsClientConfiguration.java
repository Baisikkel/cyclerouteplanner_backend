package com.cyclerouteplanner.backend.features.address.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AdsClientProperties.class)
public class AdsClientConfiguration {

    @Bean
    RestClient.Builder adsRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient adsRestClient(RestClient.Builder adsRestClientBuilder, AdsClientProperties properties) {
        return adsRestClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
