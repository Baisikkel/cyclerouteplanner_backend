package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeExportStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingAutomationProperties;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingEdgeProperties;
import com.cyclerouteplanner.backend.features.routing.application.RouteOptionService;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRefreshStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeoRoutingAutomationServiceTest {

    @Test
    void triggerBootstrapIfNeededAsyncRunsPreparePipeline() {
        GeoRoutingAutomationProperties automationProperties = new GeoRoutingAutomationProperties();
        automationProperties.setBootstrapEnabled(true);
        automationProperties.setRefreshRouteOptionsAfterPrepare(true);

        GeoRoutingEdgeProperties edgeProperties = new GeoRoutingEdgeProperties();
        edgeProperties.setExportPseudoTagsOutputPath("brouter/build-input/db_tags.csv.gz");

        GeoRoutingEdgeBuildService edgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        when(edgeBuildService.status()).thenReturn(new GeoRoutingEdgeStatus(Instant.now(), 0, 0, 0, 0));
        when(edgeBuildService.rebuildFromGeoCaches()).thenReturn(new GeoRoutingEdgeBuildStatus(
                true,
                "osm_with_optional_tallinn_merge",
                10,
                4,
                2,
                12,
                "ok",
                Instant.now()
        ));
        when(edgeBuildService.exportPseudoTagsForSegmentBuild()).thenReturn(new GeoRoutingEdgeExportStatus(
                true,
                "routing_edge_cache_pseudo_tags",
                6,
                "brouter/build-input/db_tags.csv.gz",
                "ok",
                Instant.now()
        ));

        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        when(routeOptionService.refreshFromGeoCaches()).thenReturn(new RouteOptionRefreshStatus(
                true,
                "routing_options",
                6,
                "ok",
                Instant.now()
        ));

        OsmGeoRefreshService osmGeoRefreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnGeoRefreshService = mock(TallinnGeoRefreshService.class);

        TaskExecutor inlineExecutor = Runnable::run;
        GeoRoutingAutomationService service = new GeoRoutingAutomationService(
                automationProperties,
                edgeProperties,
                edgeBuildService,
                routeOptionService,
                osmGeoRefreshService,
                tallinnGeoRefreshService,
                inlineExecutor
        );

        service.triggerBootstrapIfNeededAsync();

        verify(edgeBuildService).rebuildFromGeoCaches();
        verify(edgeBuildService).exportPseudoTagsForSegmentBuild();
        verify(routeOptionService).refreshFromGeoCaches();
        verify(osmGeoRefreshService, never()).refreshTallinnCycleNetwork();
        verify(tallinnGeoRefreshService, never()).refreshFromConfiguredSource();
    }

    @Test
    void triggerBootstrapIfNeededAsyncSkipsWhenStateAlreadyPresent() throws Exception {
        GeoRoutingAutomationProperties automationProperties = new GeoRoutingAutomationProperties();
        automationProperties.setBootstrapEnabled(true);

        Path tempDirectory = Files.createTempDirectory("routing-automation-bootstrap-test");
        Path pseudoTagsPath = tempDirectory.resolve("db_tags.csv.gz");
        Files.writeString(pseudoTagsPath, "placeholder");

        GeoRoutingEdgeProperties edgeProperties = new GeoRoutingEdgeProperties();
        edgeProperties.setExportPseudoTagsOutputPath(pseudoTagsPath.toString());

        GeoRoutingEdgeBuildService edgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        when(edgeBuildService.status()).thenReturn(new GeoRoutingEdgeStatus(Instant.now(), 5, 2, 2, 1));

        RouteOptionService routeOptionService = mock(RouteOptionService.class);
        OsmGeoRefreshService osmGeoRefreshService = mock(OsmGeoRefreshService.class);
        TallinnGeoRefreshService tallinnGeoRefreshService = mock(TallinnGeoRefreshService.class);

        TaskExecutor inlineExecutor = Runnable::run;
        GeoRoutingAutomationService service = new GeoRoutingAutomationService(
                automationProperties,
                edgeProperties,
                edgeBuildService,
                routeOptionService,
                osmGeoRefreshService,
                tallinnGeoRefreshService,
                inlineExecutor
        );

        service.triggerBootstrapIfNeededAsync();

        verify(edgeBuildService, never()).rebuildFromGeoCaches();
        verify(edgeBuildService, never()).exportPseudoTagsForSegmentBuild();
        verify(routeOptionService, never()).refreshFromGeoCaches();
    }

    @Test
    void triggerScheduledRunAsyncRunsRefreshesAndPreparePipeline() {
        GeoRoutingAutomationProperties automationProperties = new GeoRoutingAutomationProperties();
        automationProperties.setSchedulerEnabled(true);
        automationProperties.setSchedulerOsmRefreshEnabled(true);
        automationProperties.setSchedulerTallinnRefreshEnabled(true);
        automationProperties.setRefreshRouteOptionsAfterPrepare(false);

        GeoRoutingEdgeProperties edgeProperties = new GeoRoutingEdgeProperties();
        edgeProperties.setExportPseudoTagsOutputPath("brouter/build-input/db_tags.csv.gz");

        GeoRoutingEdgeBuildService edgeBuildService = mock(GeoRoutingEdgeBuildService.class);
        when(edgeBuildService.rebuildFromGeoCaches()).thenReturn(new GeoRoutingEdgeBuildStatus(
                true,
                "osm_with_optional_tallinn_merge",
                10,
                4,
                2,
                12,
                "ok",
                Instant.now()
        ));
        when(edgeBuildService.exportPseudoTagsForSegmentBuild()).thenReturn(new GeoRoutingEdgeExportStatus(
                true,
                "routing_edge_cache_pseudo_tags",
                6,
                "brouter/build-input/db_tags.csv.gz",
                "ok",
                Instant.now()
        ));

        RouteOptionService routeOptionService = mock(RouteOptionService.class);

        OsmGeoRefreshService osmGeoRefreshService = mock(OsmGeoRefreshService.class);
        when(osmGeoRefreshService.refreshTallinnCycleNetwork()).thenReturn(new GeoCacheIngestStatus(
                true,
                "osm_geofabrik_estonia",
                10,
                10,
                "ok",
                Instant.now()
        ));

        TallinnGeoRefreshService tallinnGeoRefreshService = mock(TallinnGeoRefreshService.class);
        when(tallinnGeoRefreshService.refreshFromConfiguredSource()).thenReturn(new GeoCacheIngestStatus(
                true,
                "tallinn_open_data",
                8,
                8,
                "ok",
                Instant.now()
        ));

        TaskExecutor inlineExecutor = Runnable::run;
        GeoRoutingAutomationService service = new GeoRoutingAutomationService(
                automationProperties,
                edgeProperties,
                edgeBuildService,
                routeOptionService,
                osmGeoRefreshService,
                tallinnGeoRefreshService,
                inlineExecutor
        );

        service.triggerScheduledRunAsync();

        verify(osmGeoRefreshService).refreshTallinnCycleNetwork();
        verify(tallinnGeoRefreshService).refreshFromConfiguredSource();
        verify(edgeBuildService).rebuildFromGeoCaches();
        verify(edgeBuildService).exportPseudoTagsForSegmentBuild();
        verify(routeOptionService, never()).refreshFromGeoCaches();
    }
}
