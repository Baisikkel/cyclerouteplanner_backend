package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAutomationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class GeoRoutingAutomationStartupRunner {

    private final GeoRoutingAutomationService geoRoutingAutomationService;

    public GeoRoutingAutomationStartupRunner(GeoRoutingAutomationService geoRoutingAutomationService) {
        this.geoRoutingAutomationService = geoRoutingAutomationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        geoRoutingAutomationService.triggerBootstrapIfNeededAsync();
    }
}
