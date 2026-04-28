package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.application.GeoRoutingAutomationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScheduledGeoRoutingAutomationRunnerTest {

    @Test
    void runTriggersScheduledAutomation() {
        GeoRoutingAutomationService automationService = mock(GeoRoutingAutomationService.class);
        ScheduledGeoRoutingAutomationRunner runner = new ScheduledGeoRoutingAutomationRunner(automationService);

        runner.run();

        verify(automationService).triggerScheduledRunAsync();
    }
}
