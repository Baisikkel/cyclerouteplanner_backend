package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoCacheIngestStatus;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnGeoSourcePort;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.cyclerouteplanner.backend.features.geo.infra.GeoIngestProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TallinnGeoRefreshServiceTest {

    @Test
    void refreshFromConfiguredSourceParsesGeoJsonFeatures() {
        TallinnGeoSourcePort sourcePort = mock(TallinnGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();
        properties.setTallinnSourceUrl("https://example.test/tallinn-bike.geojson");
        properties.setTallinnSourceLayer("bike_network");
        properties.setTallinnFeatureIdProperty("segment_id");
        properties.setTallinnFeatureNameProperty("segment_name");

        when(sourcePort.fetchGeoJson("https://example.test/tallinn-bike.geojson")).thenReturn("""
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {"segment_id":"s-1","segment_name":"Track A"},
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[24.70,59.43],[24.71,59.431]]
                      }
                    }
                  ]
                }
                """);
        when(ingestService.ingestTallinnLayer(org.mockito.ArgumentMatchers.eq("bike_network"), anyList()))
                .thenAnswer(invocation -> {
                    List<TallinnLayerCacheEntry> entries = invocation.getArgument(1);
                    assertEquals(1, entries.size());
                    assertEquals("s-1", entries.getFirst().sourceId());
                    assertEquals("Track A", entries.getFirst().name());
                    return new GeoCacheIngestStatus(
                            true,
                            "tallinn_open_data",
                            1,
                            1,
                            "Geo cache ingest completed",
                            Instant.parse("2026-04-04T00:00:00Z")
                    );
                });

        TallinnGeoRefreshService service = new TallinnGeoRefreshService(sourcePort, ingestService, properties);
        GeoCacheIngestStatus status = service.refreshFromConfiguredSource();

        assertEquals("tallinn_open_data", status.source());
        assertEquals(1, status.upsertedCount());
    }

    @Test
    void refreshFromConfiguredSourceFailsWhenUrlMissing() {
        TallinnGeoSourcePort sourcePort = mock(TallinnGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();
        properties.setTallinnSourceUrl("   ");

        TallinnGeoRefreshService service = new TallinnGeoRefreshService(sourcePort, ingestService, properties);

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::refreshFromConfiguredSource);
        assertEquals("geo.ingest.tallinn-source-url is not configured", exception.getMessage());
    }

    @Test
    void refreshFromConfiguredSourcePaginatesArcGisSource() {
        TallinnGeoSourcePort sourcePort = mock(TallinnGeoSourcePort.class);
        GeoCacheIngestService ingestService = mock(GeoCacheIngestService.class);
        GeoIngestProperties properties = new GeoIngestProperties();
        properties.setTallinnSourceUrl(
                "https://gis.tallinn.ee/arcgis/rest/services/Rattateed/MapServer/0/query?where=1%3D1&outFields=*&f=geojson");
        properties.setTallinnSourceLayer("bike_network");
        properties.setTallinnFeatureIdProperty("segment_id");
        properties.setTallinnFeatureNameProperty("segment_name");
        properties.setTallinnPageSize(2);
        properties.setTallinnMaxPages(5);

        when(sourcePort.fetchGeoJson(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0, String.class);
            assertTrue(url.contains("where=1%3D1"));
            if (url.contains("resultOffset=0")) {
                return """
                        {
                          "type": "FeatureCollection",
                          "exceededTransferLimit": true,
                          "features": [
                            {
                              "type": "Feature",
                              "properties": {"segment_id":"s-1","segment_name":"Track A"},
                              "geometry": {
                                "type": "LineString",
                                "coordinates": [[24.70,59.43],[24.71,59.431]]
                              }
                            },
                            {
                              "type": "Feature",
                              "properties": {"segment_id":"s-2","segment_name":"Track B"},
                              "geometry": {
                                "type": "LineString",
                                "coordinates": [[24.72,59.432],[24.73,59.433]]
                              }
                            }
                          ]
                        }
                        """;
            }
            if (url.contains("resultOffset=2")) {
                return """
                        {
                          "type": "FeatureCollection",
                          "exceededTransferLimit": false,
                          "features": [
                            {
                              "type": "Feature",
                              "properties": {"segment_id":"s-3","segment_name":"Track C"},
                              "geometry": {
                                "type": "LineString",
                                "coordinates": [[24.74,59.434],[24.75,59.435]]
                              }
                            }
                          ]
                        }
                        """;
            }
            throw new IllegalStateException("Unexpected pagination URL: " + url);
        });
        when(ingestService.ingestTallinnLayer(org.mockito.ArgumentMatchers.eq("bike_network"), anyList()))
                .thenAnswer(invocation -> {
                    List<TallinnLayerCacheEntry> entries = invocation.getArgument(1);
                    assertEquals(3, entries.size());
                    assertEquals("s-1", entries.get(0).sourceId());
                    assertEquals("s-2", entries.get(1).sourceId());
                    assertEquals("s-3", entries.get(2).sourceId());
                    return new GeoCacheIngestStatus(
                            true,
                            "tallinn_open_data",
                            3,
                            3,
                            "Geo cache ingest completed",
                            Instant.parse("2026-04-04T00:00:00Z")
                    );
                });

        TallinnGeoRefreshService service = new TallinnGeoRefreshService(sourcePort, ingestService, properties);
        GeoCacheIngestStatus status = service.refreshFromConfiguredSource();

        assertEquals("tallinn_open_data", status.source());
        assertEquals(3, status.upsertedCount());
        verify(sourcePort, times(2)).fetchGeoJson(anyString());
    }
}
