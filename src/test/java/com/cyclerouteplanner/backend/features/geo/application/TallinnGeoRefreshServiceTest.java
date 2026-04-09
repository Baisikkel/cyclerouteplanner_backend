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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
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
}
