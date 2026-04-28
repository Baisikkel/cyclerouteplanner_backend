package com.cyclerouteplanner.backend.features.address.application;

import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCacheEntry;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCachePort;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsCacheRefreshStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsGatewayPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdsCacheRefreshServiceTest {

    @Test
    void refreshParsesAndUpsertsSupportedPayloadShape() {
        List<AdsAddressCacheEntry> upsertedEntries = new ArrayList<>();
        List<String> snapshotSources = new ArrayList<>();

        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "OK";
            }

            @Override
            public List<AdsAddressSuggestion> search(String query, int limit) {
                return List.of();
            }

            @Override
            public String searchRaw(String query, int limit) {
                return """
                        {
                          "results": [
                            { "ads_oid": "1001", "full_address": "Tallinn, Test 1", "lat": 59.4370, "lon": 24.7536 },
                            { "id": "1002", "address": "Tallinn, Test 2", "y": 59.4380, "x": 24.7540 }
                          ]
                        }
                        """;
            }
        };
        AdsAddressCachePort cachePort = upsertedEntries::add;
        DataSnapshotPort snapshotPort = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(
                    String source,
                    String sourceVersion,
                    Instant sourceTimestamp,
                    String checksum,
                    Map<String, Object> metadata
            ) {
                snapshotSources.add(source);
                return new DataSnapshotRecord(source, sourceVersion, sourceTimestamp, checksum, metadata, Instant.now());
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                return List.of();
            }
        };

        AdsCacheRefreshService service = new AdsCacheRefreshService(gateway, cachePort, snapshotPort);

        AdsCacheRefreshStatus status = service.refresh("Tallinn", 10);

        assertTrue(status.reachable());
        assertEquals(2, status.upsertedCount());
        assertEquals(2, upsertedEntries.size());
        assertEquals("1001", upsertedEntries.get(0).adsOid());
        assertEquals("tallinn, test 1", upsertedEntries.get(0).normalizedAddress());
        assertEquals(List.of("maaamet_inads"), snapshotSources);
    }

    @Test
    void refreshReturnsUnavailableWhenGatewayFails() {
        AdsGatewayPort gateway = new AdsGatewayPort() {
            @Override
            public String fetchStatus() {
                return "OK";
            }

            @Override
            public List<AdsAddressSuggestion> search(String query, int limit) {
                return List.of();
            }

            @Override
            public String searchRaw(String query, int limit) {
                throw new IllegalStateException("ADS unavailable");
            }
        };
        AdsAddressCachePort cachePort = entry -> {
        };
        DataSnapshotPort snapshotPort = new DataSnapshotPort() {
            @Override
            public DataSnapshotRecord upsert(
                    String source,
                    String sourceVersion,
                    Instant sourceTimestamp,
                    String checksum,
                    Map<String, Object> metadata
            ) {
                return new DataSnapshotRecord(source, sourceVersion, sourceTimestamp, checksum, metadata, Instant.now());
            }

            @Override
            public List<DataSnapshotRecord> findLatest(int limit) {
                return List.of();
            }
        };

        AdsCacheRefreshService service = new AdsCacheRefreshService(gateway, cachePort, snapshotPort);
        AdsCacheRefreshStatus status = service.refresh("Tallinn", 10);

        assertFalse(status.reachable());
        assertEquals(0, status.upsertedCount());
        assertEquals("ADS unavailable", status.details());
    }
}
