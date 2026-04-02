package com.cyclerouteplanner.backend.features.address.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "address.ads")
public class AdsClientProperties {

    private String baseUrl = "https://xgis.maaamet.ee/adsavalik";
    private String statusPath = "/";
    private String searchPath = "/api/search";
    private String searchQueryParam = "q";
    private String searchLimitParam = "limit";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getStatusPath() {
        return statusPath;
    }

    public void setStatusPath(String statusPath) {
        this.statusPath = statusPath;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }

    public String getSearchQueryParam() {
        return searchQueryParam;
    }

    public void setSearchQueryParam(String searchQueryParam) {
        this.searchQueryParam = searchQueryParam;
    }

    public String getSearchLimitParam() {
        return searchLimitParam;
    }

    public void setSearchLimitParam(String searchLimitParam) {
        this.searchLimitParam = searchLimitParam;
    }
}
