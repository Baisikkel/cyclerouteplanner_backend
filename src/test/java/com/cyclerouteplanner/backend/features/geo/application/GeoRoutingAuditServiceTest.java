package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeoRoutingAuditServiceTest {

    @Test
    void auditMarksMergedGraphReadyWhenOsmAndTallinnCoverageExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(
                        Map.of(
                                "total_rows", 1000L,
                                "line_rows", 800L,
                                "bike_candidate_line_rows", 520L,
                                "line_rows_with_bicycle_tag", 400L,
                                "line_rows_with_cycleway_tag", 350L,
                                "line_rows_with_route_relation_tag", 210L,
                                "line_rows_with_surface_tag", 380L,
                                "line_rows_with_tracktype_tag", 120L
                        ),
                        Map.of(
                                "total_rows", 300L,
                                "line_rows", 290L,
                                "rows_with_properties", 290L,
                                "rows_with_name", 200L,
                                "distinct_source_layers", 1L,
                                "line_rows_overlapping_osm", 250L
                        )
                );

        GeoRoutingAuditService service = new GeoRoutingAuditService(jdbcTemplate);

        GeoRoutingAuditResponse response = service.audit();

        assertTrue(response.readiness().readyForOsmOnlyGraphBuild());
        assertTrue(response.readiness().readyForMergedOsmTallinnGraphBuild());
        assertTrue(response.readiness().gaps().isEmpty());
        assertEquals(0.65, response.readiness().osmBikeCoverageRatio(), 0.00001);
    }

    @Test
    void auditReturnsGapsWhenTallinnDataIsMissingAndOsmHasNoBikeCandidates() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(
                        Map.of(
                                "total_rows", 250L,
                                "line_rows", 120L,
                                "bike_candidate_line_rows", 0L,
                                "line_rows_with_bicycle_tag", 0L,
                                "line_rows_with_cycleway_tag", 0L,
                                "line_rows_with_route_relation_tag", 0L,
                                "line_rows_with_surface_tag", 40L,
                                "line_rows_with_tracktype_tag", 8L
                        ),
                        Map.of(
                                "total_rows", 0L,
                                "line_rows", 0L,
                                "rows_with_properties", 0L,
                                "rows_with_name", 0L,
                                "distinct_source_layers", 0L,
                                "line_rows_overlapping_osm", 0L
                        )
                );

        GeoRoutingAuditService service = new GeoRoutingAuditService(jdbcTemplate);

        GeoRoutingAuditResponse response = service.audit();

        assertFalse(response.readiness().readyForOsmOnlyGraphBuild());
        assertFalse(response.readiness().readyForMergedOsmTallinnGraphBuild());
        assertFalse(response.readiness().gaps().isEmpty());
        assertTrue(response.readiness().gaps().stream().anyMatch(message -> message.contains("Tallinn layer cache is empty")));
    }
}
