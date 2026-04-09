package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.ingest")
public class GeoIngestProperties {

    private double defaultBboxSouth;
    private double defaultBboxWest;
    private double defaultBboxNorth;
    private double defaultBboxEast;
    private int overpassTimeoutSeconds;
    private String tallinnSourceUrl;
    private String tallinnSourceLayer;
    private String tallinnFeatureIdProperty;
    private String tallinnFeatureNameProperty;

    public double getDefaultBboxSouth() {
        return defaultBboxSouth;
    }

    public void setDefaultBboxSouth(double defaultBboxSouth) {
        this.defaultBboxSouth = defaultBboxSouth;
    }

    public double getDefaultBboxWest() {
        return defaultBboxWest;
    }

    public void setDefaultBboxWest(double defaultBboxWest) {
        this.defaultBboxWest = defaultBboxWest;
    }

    public double getDefaultBboxNorth() {
        return defaultBboxNorth;
    }

    public void setDefaultBboxNorth(double defaultBboxNorth) {
        this.defaultBboxNorth = defaultBboxNorth;
    }

    public double getDefaultBboxEast() {
        return defaultBboxEast;
    }

    public void setDefaultBboxEast(double defaultBboxEast) {
        this.defaultBboxEast = defaultBboxEast;
    }

    public int getOverpassTimeoutSeconds() {
        return overpassTimeoutSeconds;
    }

    public void setOverpassTimeoutSeconds(int overpassTimeoutSeconds) {
        this.overpassTimeoutSeconds = overpassTimeoutSeconds;
    }

    public String getTallinnSourceUrl() {
        return tallinnSourceUrl;
    }

    public void setTallinnSourceUrl(String tallinnSourceUrl) {
        this.tallinnSourceUrl = tallinnSourceUrl;
    }

    public String getTallinnSourceLayer() {
        return tallinnSourceLayer;
    }

    public void setTallinnSourceLayer(String tallinnSourceLayer) {
        this.tallinnSourceLayer = tallinnSourceLayer;
    }

    public String getTallinnFeatureIdProperty() {
        return tallinnFeatureIdProperty;
    }

    public void setTallinnFeatureIdProperty(String tallinnFeatureIdProperty) {
        this.tallinnFeatureIdProperty = tallinnFeatureIdProperty;
    }

    public String getTallinnFeatureNameProperty() {
        return tallinnFeatureNameProperty;
    }

    public void setTallinnFeatureNameProperty(String tallinnFeatureNameProperty) {
        this.tallinnFeatureNameProperty = tallinnFeatureNameProperty;
    }
}
