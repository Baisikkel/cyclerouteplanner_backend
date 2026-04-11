package com.cyclerouteplanner.backend.features.routing.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised settings for the BRouter routing engine.
 * Values are bound from {@code routing.brouter.*} in application.yml and can be
 * overridden with the environment variable {@code ROUTING_BROUTER_BASE_URL}.
 *
 * @see com.cyclerouteplanner.backend.features.routing.infra.BRouterConfiguration
 */
@ConfigurationProperties(prefix = "routing.brouter")
public class BRouterProperties {

    private String baseUrl = "https://brouter.de/brouter";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
