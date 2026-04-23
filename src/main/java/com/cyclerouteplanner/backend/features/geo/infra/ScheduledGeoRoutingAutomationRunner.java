package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAutomationService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class ScheduledGeoRoutingAutomationRunner {

    private final GeoRoutingAutomationService geoRoutingAutomationService;

    public ScheduledGeoRoutingAutomationRunner(GeoRoutingAutomationService geoRoutingAutomationService) {
        this.geoRoutingAutomationService = geoRoutingAutomationService;
    }

    @Scheduled(cron = "${geo.routing-automation.scheduler-cron:0 30 2 * * *}")
    public void run() {
        geoRoutingAutomationService.triggerScheduledRunAsync();
    }
}
