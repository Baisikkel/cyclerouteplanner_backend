package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAutomationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GeoRoutingAutomationStartupRunnerTest {

    @Test
    void onApplicationReadyTriggersBootstrapCheck() {
        GeoRoutingAutomationService automationService = mock(GeoRoutingAutomationService.class);
        GeoRoutingAutomationStartupRunner runner = new GeoRoutingAutomationStartupRunner(automationService);

        runner.onApplicationReady();

        verify(automationService).triggerBootstrapIfNeededAsync();
    }
}
