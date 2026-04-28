package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeExportStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingAutomationProperties;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingEdgeProperties;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile({"local", "docker"})
public class GeoRoutingAutomationService {

    private static final Logger LOG = LoggerFactory.getLogger(GeoRoutingAutomationService.class);

    private final GeoRoutingAutomationProperties automationProperties;
    private final GeoRoutingEdgeProperties routingEdgeProperties;
    private final GeoRoutingEdgeBuildService geoRoutingEdgeBuildService;
    private final RouteOptionService routeOptionService;
    private final OsmGeoRefreshService osmGeoRefreshService;
    private final TallinnGeoRefreshService tallinnGeoRefreshService;
    private final TaskExecutor taskExecutor;
    private final AtomicBoolean runInProgress = new AtomicBoolean(false);

    public GeoRoutingAutomationService(
            GeoRoutingAutomationProperties automationProperties,
            GeoRoutingEdgeProperties routingEdgeProperties,
            GeoRoutingEdgeBuildService geoRoutingEdgeBuildService,
            RouteOptionService routeOptionService,
            OsmGeoRefreshService osmGeoRefreshService,
            TallinnGeoRefreshService tallinnGeoRefreshService,
            @Qualifier("applicationTaskExecutor")
            TaskExecutor taskExecutor
    ) {
        this.automationProperties = automationProperties;
        this.routingEdgeProperties = routingEdgeProperties;
        this.geoRoutingEdgeBuildService = geoRoutingEdgeBuildService;
        this.routeOptionService = routeOptionService;
        this.osmGeoRefreshService = osmGeoRefreshService;
        this.tallinnGeoRefreshService = tallinnGeoRefreshService;
        this.taskExecutor = taskExecutor;
    }

    public void triggerBootstrapIfNeededAsync() {
        if (!automationProperties.isBootstrapEnabled()) {
            return;
        }
        if (!needsBootstrap()) {
            LOG.info("Routing automation bootstrap skipped - active edges and pseudo-tags already exist.");
            return;
        }
        triggerAsync("startup-bootstrap", false, false);
    }

    public void triggerScheduledRunAsync() {
        if (!automationProperties.isSchedulerEnabled()) {
            return;
        }
        triggerAsync(
                "scheduled",
                automationProperties.isSchedulerOsmRefreshEnabled(),
                automationProperties.isSchedulerTallinnRefreshEnabled()
        );
    }

    boolean needsBootstrap() {
        try {
            GeoRoutingEdgeStatus status = geoRoutingEdgeBuildService.status();
            boolean hasActiveRoutingEdges = status.activeTotalCount() > 0;
            boolean hasPseudoTagExport = hasPseudoTagExportFile();
            return !hasActiveRoutingEdges || !hasPseudoTagExport;
        } catch (RuntimeException ex) {
            LOG.warn("Routing automation bootstrap check failed, defaulting to run: {}", ex.getMessage());
            return true;
        }
    }

    void runAutomationNow(String trigger, boolean refreshOsm, boolean refreshTallinn) {
        if (!runInProgress.compareAndSet(false, true)) {
            LOG.info("Routing automation '{}' skipped because another run is already in progress.", trigger);
            return;
        }

        try {
            if (refreshOsm) {
                GeoCacheIngestStatus osmStatus = osmGeoRefreshService.refreshTallinnCycleNetwork();
                LOG.info(
                        "Routing automation '{}' OSM refresh finished, successful={}, upsertedCount={}, details={}",
                        trigger,
                        osmStatus.successful(),
                        osmStatus.upsertedCount(),
                        osmStatus.details()
                );
            }

            if (refreshTallinn) {
                GeoCacheIngestStatus tallinnStatus = tallinnGeoRefreshService.refreshFromConfiguredSource();
                LOG.info(
                        "Routing automation '{}' Tallinn refresh finished, successful={}, upsertedCount={}, details={}",
                        trigger,
                        tallinnStatus.successful(),
                        tallinnStatus.upsertedCount(),
                        tallinnStatus.details()
                );
            }

            GeoRoutingEdgeBuildStatus buildStatus = geoRoutingEdgeBuildService.rebuildFromGeoCaches();
            if (!buildStatus.successful()) {
                LOG.warn(
                        "Routing automation '{}' failed during rebuild, details={}",
                        trigger,
                        buildStatus.details()
                );
                return;
            }

            GeoRoutingEdgeExportStatus exportStatus = geoRoutingEdgeBuildService.exportPseudoTagsForSegmentBuild();
            if (!exportStatus.successful()) {
                LOG.warn(
                        "Routing automation '{}' failed during pseudo-tag export, details={}",
                        trigger,
                        exportStatus.details()
                );
                return;
            }

            if (automationProperties.isRefreshRouteOptionsAfterPrepare()) {
                RouteOptionRefreshStatus routeOptionStatus = routeOptionService.refreshFromGeoCaches();
                LOG.info(
                        "Routing automation '{}' route-option refresh finished, successful={}, upsertedCount={}, details={}",
                        trigger,
                        routeOptionStatus.successful(),
                        routeOptionStatus.upsertedCount(),
                        routeOptionStatus.details()
                );
            }

            LOG.info(
                    "Routing automation '{}' completed, pseudoTagsExportedCount={}, outputPath={}",
                    trigger,
                    exportStatus.exportedCount(),
                    exportStatus.outputPath()
            );
        } catch (RuntimeException ex) {
            LOG.warn("Routing automation '{}' failed: {}", trigger, ex.getMessage(), ex);
        } finally {
            runInProgress.set(false);
        }
    }

    private void triggerAsync(String trigger, boolean refreshOsm, boolean refreshTallinn) {
        taskExecutor.execute(() -> runAutomationNow(trigger, refreshOsm, refreshTallinn));
    }

    private boolean hasPseudoTagExportFile() {
        String configuredPath = routingEdgeProperties.getExportPseudoTagsOutputPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            return false;
        }

        try {
            Path outputPath = Path.of(configuredPath).toAbsolutePath().normalize();
            return Files.isRegularFile(outputPath) && Files.size(outputPath) > 0;
        } catch (Exception ex) {
            LOG.warn("Routing automation pseudo-tag file check failed: {}", ex.getMessage());
            return false;
        }
    }
}
