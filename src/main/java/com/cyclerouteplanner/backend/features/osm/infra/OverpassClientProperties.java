package com.cyclerouteplanner.backend.features.osm.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "osm.overpass")
public class OverpassClientProperties {

    private String baseUrl = "https://overpass-api.de/api";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
