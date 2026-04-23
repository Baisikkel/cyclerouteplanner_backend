package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.routing-automation")
public class GeoRoutingAutomationProperties {

    private boolean bootstrapEnabled = true;
    private boolean schedulerEnabled = false;
    private String schedulerCron = "0 30 2 * * *";
    private boolean schedulerOsmRefreshEnabled = false;
    private boolean schedulerTallinnRefreshEnabled = false;
    private boolean refreshRouteOptionsAfterPrepare = true;

    public boolean isBootstrapEnabled() {
        return bootstrapEnabled;
    }

    public void setBootstrapEnabled(boolean bootstrapEnabled) {
        this.bootstrapEnabled = bootstrapEnabled;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public String getSchedulerCron() {
        return schedulerCron;
    }

    public void setSchedulerCron(String schedulerCron) {
        this.schedulerCron = schedulerCron;
    }

    public boolean isSchedulerOsmRefreshEnabled() {
        return schedulerOsmRefreshEnabled;
    }

    public void setSchedulerOsmRefreshEnabled(boolean schedulerOsmRefreshEnabled) {
        this.schedulerOsmRefreshEnabled = schedulerOsmRefreshEnabled;
    }

    public boolean isSchedulerTallinnRefreshEnabled() {
        return schedulerTallinnRefreshEnabled;
    }

    public void setSchedulerTallinnRefreshEnabled(boolean schedulerTallinnRefreshEnabled) {
        this.schedulerTallinnRefreshEnabled = schedulerTallinnRefreshEnabled;
    }

    public boolean isRefreshRouteOptionsAfterPrepare() {
        return refreshRouteOptionsAfterPrepare;
    }

    public void setRefreshRouteOptionsAfterPrepare(boolean refreshRouteOptionsAfterPrepare) {
        this.refreshRouteOptionsAfterPrepare = refreshRouteOptionsAfterPrepare;
    }
}
