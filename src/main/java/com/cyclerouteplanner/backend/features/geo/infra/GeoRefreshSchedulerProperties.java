package com.cyclerouteplanner.backend.features.geo.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geo.refresh.scheduler")
public class GeoRefreshSchedulerProperties {

    private boolean enabled = false;
    private String cron = "0 0 */6 * * *";
    private boolean adsEnabled = true;
    private boolean osmEnabled = true;
    private boolean tallinnEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public boolean isAdsEnabled() {
        return adsEnabled;
    }

    public void setAdsEnabled(boolean adsEnabled) {
        this.adsEnabled = adsEnabled;
    }

    public boolean isOsmEnabled() {
        return osmEnabled;
    }

    public void setOsmEnabled(boolean osmEnabled) {
        this.osmEnabled = osmEnabled;
    }

    public boolean isTallinnEnabled() {
        return tallinnEnabled;
    }

    public void setTallinnEnabled(boolean tallinnEnabled) {
        this.tallinnEnabled = tallinnEnabled;
    }
}
