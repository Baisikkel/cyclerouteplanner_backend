package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.address.application.AdsCacheRefreshService;
import com.cyclerouteplanner.backend.features.address.infra.AdsClientProperties;
import com.cyclerouteplanner.backend.features.geo.application.OsmGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.application.TallinnGeoRefreshService;
import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class ScheduledGeoRefreshRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledGeoRefreshRunner.class);

    private final GeoRefreshSchedulerProperties schedulerProperties;
    private final AdsCacheRefreshService adsCacheRefreshService;
    private final AdsClientProperties adsClientProperties;
    private final OsmGeoRefreshService osmGeoRefreshService;
    private final TallinnGeoRefreshService tallinnGeoRefreshService;

    public ScheduledGeoRefreshRunner(
            GeoRefreshSchedulerProperties schedulerProperties,
            AdsCacheRefreshService adsCacheRefreshService,
            AdsClientProperties adsClientProperties,
            OsmGeoRefreshService osmGeoRefreshService,
            TallinnGeoRefreshService tallinnGeoRefreshService
    ) {
        this.schedulerProperties = schedulerProperties;
        this.adsCacheRefreshService = adsCacheRefreshService;
        this.adsClientProperties = adsClientProperties;
        this.osmGeoRefreshService = osmGeoRefreshService;
        this.tallinnGeoRefreshService = tallinnGeoRefreshService;
    }

    @Scheduled(cron = "${geo.refresh.scheduler.cron:0 0 */6 * * *}")
    public void run() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }

        if (schedulerProperties.isAdsEnabled()) {
            runAdsRefresh();
        }
        if (schedulerProperties.isOsmEnabled()) {
            runOsmRefresh();
        }
        if (schedulerProperties.isTallinnEnabled()) {
            runTallinnRefresh();
        }
    }

    private void runAdsRefresh() {
        var status = adsCacheRefreshService.refresh(
                adsClientProperties.getCacheRefreshDefaultQuery(),
                adsClientProperties.getCacheRefreshDefaultLimit()
        );
        LOG.info(
                "Scheduled ADS refresh finished, reachable={}, upsertedCount={}, details={}",
                status.reachable(),
                status.upsertedCount(),
                status.details()
        );
    }

    private void runOsmRefresh() {
        GeoCacheIngestStatus status = osmGeoRefreshService.refreshTallinnCycleNetwork();
        LOG.info(
                "Scheduled OSM refresh finished, successful={}, upsertedCount={}, details={}",
                status.successful(),
                status.upsertedCount(),
                status.details()
        );
    }

    private void runTallinnRefresh() {
        try {
            GeoCacheIngestStatus status = tallinnGeoRefreshService.refreshFromConfiguredSource();
            LOG.info(
                    "Scheduled Tallinn refresh finished, successful={}, upsertedCount={}, details={}",
                    status.successful(),
                    status.upsertedCount(),
                    status.details()
            );
        } catch (RuntimeException ex) {
            LOG.warn("Scheduled Tallinn refresh failed: {}", ex.getMessage());
        }
    }
}
