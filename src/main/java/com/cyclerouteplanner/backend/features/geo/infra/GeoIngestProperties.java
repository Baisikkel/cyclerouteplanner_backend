package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.ingest")
public class GeoIngestProperties {

    private double defaultBboxSouth = 59.30;
    private double defaultBboxWest = 24.50;
    private double defaultBboxNorth = 59.52;
    private double defaultBboxEast = 24.93;
    private int overpassTimeoutSeconds = 25;
    private String tallinnSourceUrl = "";
    private String tallinnSourceLayer = "tallinn_open_data";
    private String tallinnFeatureIdProperty = "id";
    private String tallinnFeatureNameProperty = "name";

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
