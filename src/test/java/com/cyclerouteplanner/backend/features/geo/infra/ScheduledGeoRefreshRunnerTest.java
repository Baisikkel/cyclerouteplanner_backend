package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.address.application.AdsCacheRefreshService;
import com.cyclerouteplanner.backend.features.address.domain.AdsCacheRefreshStatus;
import com.cyclerouteplanner.backend.features.address.infra.AdsClientProperties;
import com.cyclerouteplanner.backend.features.geo.application.OsmGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.application.TallinnGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledGeoRefreshRunnerTest {

    @Test
    void runExecutesOnlyEnabledRefreshes() {
        GeoRefreshSchedulerProperties schedulerProperties = new GeoRefreshSchedulerProperties();
        schedulerProperties.setEnabled(true);
        schedulerProperties.setAdsEnabled(true);
        schedulerProperties.setOsmEnabled(false);
        schedulerProperties.setTallinnEnabled(true);

        AdsClientProperties adsClientProperties = new AdsClientProperties();
        adsClientProperties.setCacheRefreshDefaultQuery("Tallinn");
        adsClientProperties.setCacheRefreshDefaultLimit(20);

        AdsCacheRefreshService adsService = mock(AdsCacheRefreshService.class);
        when(adsService.refresh("Tallinn", 20)).thenReturn(
                new AdsCacheRefreshStatus(true, "maa-amet-ads", "Tallinn", 20, 20, "ok", Instant.now())
        );

        OsmGeoRefreshService osmService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnService = mock(TallinnGeoRefreshService.class);
        when(tallinnService.refreshFromConfiguredSource()).thenReturn(
                new GeoCacheIngestStatus(true, "tallinn_open_data", 10, 10, "ok", Instant.now())
        );

        ScheduledGeoRefreshRunner runner = new ScheduledGeoRefreshRunner(
                schedulerProperties,
                adsService,
                adsClientProperties,
                osmService,
                tallinnService
        );

        runner.run();

        verify(adsService).refresh("Tallinn", 20);
        verify(osmService, never()).refreshTallinnCycleNetwork();
        verify(tallinnService).refreshFromConfiguredSource();
    }

    @Test
    void runSkipsWhenSchedulerDisabled() {
        GeoRefreshSchedulerProperties schedulerProperties = new GeoRefreshSchedulerProperties();
        schedulerProperties.setEnabled(false);

        AdsCacheRefreshService adsService = mock(AdsCacheRefreshService.class);
        AdsClientProperties adsClientProperties = new AdsClientProperties();
        OsmGeoRefreshService osmService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnService = mock(TallinnGeoRefreshService.class);

        ScheduledGeoRefreshRunner runner = new ScheduledGeoRefreshRunner(
                schedulerProperties,
                adsService,
                adsClientProperties,
                osmService,
                tallinnService
        );

        runner.run();

        verify(adsService, never()).refresh(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(osmService, never()).refreshTallinnCycleNetwork();
        verify(tallinnService, never()).refreshFromConfiguredSource();
    }
}
