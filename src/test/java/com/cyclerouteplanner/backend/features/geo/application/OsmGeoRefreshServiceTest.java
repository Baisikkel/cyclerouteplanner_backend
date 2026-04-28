package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.infra.GeoIngestProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OsmGeoRefreshServiceTest {

    @Test
    void refreshTallinnCycleNetworkParsesOverpassElements() {
        OsmGeoSourcePort sourcePort = mock(OsmGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();

        when(sourcePort.fetchCycleNetwork(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn("""
                {
                  "elements": [
                    {
                      "type": "way",
                      "id": 1001,
                      "tags": {"highway":"cycleway","name":"Sample Track"},
                      "geometry": [
                        {"lat":59.4300,"lon":24.7200},
                        {"lat":59.4310,"lon":24.7210}
                      ]
                    }
                  ]
                }
                """);
        when(ingestService.ingestOsmFeatures(anyList())).thenAnswer(invocation -> {
            List<OsmFeatureCacheEntry> entries = invocation.getArgument(0);
            assertEquals(1, entries.size());
            assertEquals("way/1001", entries.getFirst().sourceId());
            assertNotNull(entries.getFirst().wktGeometry());
            return new GeoCacheIngestStatus(
                    true,
                    "osm_geofabrik_estonia",
                    entries.size(),
                    entries.size(),
                    "Geo cache ingest completed",
                    Instant.parse("2026-04-04T00:00:00Z")
            );
        });

        OsmGeoRefreshService service = new OsmGeoRefreshService(sourcePort, ingestService, properties);

        GeoCacheIngestStatus status = service.refreshTallinnCycleNetwork();

        assertEquals("osm_geofabrik_estonia", status.source());
        assertEquals(1, status.upsertedCount());
    }

    @Test
    void refreshTallinnCycleNetworkDerivesRouteNetworkTagsFromRelations() {
        OsmGeoSourcePort sourcePort = mock(OsmGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();

        when(sourcePort.fetchCycleNetwork(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn("""
                {
                  "elements": [
                    {
                      "type": "way",
                      "id": 1001,
                      "tags": {"highway":"residential","name":"Bike Street"},
                      "geometry": [
                        {"lat":59.4300,"lon":24.7200},
                        {"lat":59.4310,"lon":24.7210}
                      ]
                    },
                    {
                      "type": "relation",
                      "id": 3001,
                      "tags": {"type":"route","route":"bicycle","network":"lcn"},
                      "members": [
                        {"type":"way","ref":1001,"role":""}
                      ]
                    }
                  ]
                }
                """);
        when(ingestService.ingestOsmFeatures(anyList())).thenAnswer(invocation -> {
            List<OsmFeatureCacheEntry> entries = invocation.getArgument(0);
            OsmFeatureCacheEntry wayEntry = entries.stream()
                    .filter(item -> "way/1001".equals(item.sourceId()))
                    .findFirst()
                    .orElseThrow();

            assertEquals("residential", wayEntry.featureType());
            assertEquals("yes", wayEntry.tags().get("route_bicycle_lcn"));
            return new GeoCacheIngestStatus(
                    true,
                    "osm_geofabrik_estonia",
                    entries.size(),
                    entries.size(),
                    "Geo cache ingest completed",
                    Instant.parse("2026-04-04T00:00:00Z")
            );
        });

        OsmGeoRefreshService service = new OsmGeoRefreshService(sourcePort, ingestService, properties);

        GeoCacheIngestStatus status = service.refreshTallinnCycleNetwork();

        assertEquals("osm_geofabrik_estonia", status.source());
        assertEquals(1, status.upsertedCount());
    }

    @Test
    void refreshTallinnCycleNetworkIncludesBarrierNodes() {
        OsmGeoSourcePort sourcePort = mock(OsmGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();

        when(sourcePort.fetchCycleNetwork(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn("""
                {
                  "elements": [
                    {
                      "type": "node",
                      "id": 2002,
                      "lat": 59.4320,
                      "lon": 24.7300,
                      "tags": {"barrier":"bollard"}
                    }
                  ]
                }
                """);
        when(ingestService.ingestOsmFeatures(anyList())).thenAnswer(invocation -> {
            List<OsmFeatureCacheEntry> entries = invocation.getArgument(0);
            assertEquals(1, entries.size());
            assertEquals("node/2002", entries.getFirst().sourceId());
            assertEquals("barrier", entries.getFirst().featureType());
            assertTrue(entries.getFirst().wktGeometry().startsWith("POINT("));
            return new GeoCacheIngestStatus(
                    true,
                    "osm_geofabrik_estonia",
                    entries.size(),
                    entries.size(),
                    "Geo cache ingest completed",
                    Instant.parse("2026-04-04T00:00:00Z")
            );
        });

        OsmGeoRefreshService service = new OsmGeoRefreshService(sourcePort, ingestService, properties);

        GeoCacheIngestStatus status = service.refreshTallinnCycleNetwork();

        assertEquals("osm_geofabrik_estonia", status.source());
        assertEquals(1, status.upsertedCount());
    }
}
