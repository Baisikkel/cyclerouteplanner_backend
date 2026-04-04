package com.cyclerouteplanner.backend.features.address.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "address.ads")
public class AdsClientProperties {

    private String baseUrl = "https://inaadress.maaamet.ee";
    private String statusPath = "/inaadress/";
    private String searchPath = "/inaadress/gazetteer";
    private String searchQueryParam = "address";
    private String searchLimitParam = "results";
    private String cacheRefreshDefaultQuery = "Tallinn";
    private int cacheRefreshDefaultLimit = 100;

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

    public String getCacheRefreshDefaultQuery() {
        return cacheRefreshDefaultQuery;
    }

    public void setCacheRefreshDefaultQuery(String cacheRefreshDefaultQuery) {
        this.cacheRefreshDefaultQuery = cacheRefreshDefaultQuery;
    }

    public int getCacheRefreshDefaultLimit() {
        return cacheRefreshDefaultLimit;
    }

    public void setCacheRefreshDefaultLimit(int cacheRefreshDefaultLimit) {
        this.cacheRefreshDefaultLimit = cacheRefreshDefaultLimit;
    }
}
