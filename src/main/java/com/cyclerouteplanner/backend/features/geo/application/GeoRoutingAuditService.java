package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditOsmResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditReadinessResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoRoutingAuditTallinnResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class GeoRoutingAuditService {

    private static final String OSM_AUDIT_SQL = """
            select
                count(*) as total_rows,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                ) as line_rows,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and (
                        feature_type in (
                            'cycleway',
                            'path',
                            'track',
                            'residential',
                            'living_street',
                            'service',
                            'unclassified',
                            'tertiary',
                            'tertiary_link',
                            'secondary',
                            'secondary_link',
                            'primary',
                            'primary_link',
                            'road',
                            'footway',
                            'pedestrian',
                            'bridleway',
                            'steps'
                        )
                        or tags ? 'cycleway'
                        or coalesce(tags ->> 'bicycle', '') in ('yes', 'designated', 'permissive')
                      )
                ) as bike_candidate_line_rows,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and tags ? 'bicycle'
                ) as line_rows_with_bicycle_tag,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and (
                        tags ? 'cycleway'
                        or tags ? 'cycleway:left'
                        or tags ? 'cycleway:right'
                        or tags ? 'cycleway:both'
                      )
                ) as line_rows_with_cycleway_tag,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and (
                        tags ? 'route_bicycle_lcn'
                        or tags ? 'route_bicycle_rcn'
                        or tags ? 'route_bicycle_ncn'
                        or tags ? 'route_bicycle_icn'
                      )
                ) as line_rows_with_route_relation_tag,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and tags ? 'surface'
                ) as line_rows_with_surface_tag,
                count(*) filter (
                    where GeometryType(geom) in ('LINESTRING', 'MULTILINESTRING')
                      and tags ? 'tracktype'
                ) as line_rows_with_tracktype_tag
            from geo.osm_feature_cache
            """;

    private static final String TALLINN_AUDIT_SQL = """
            select
                count(*) as total_rows,
                count(*) filter (
                    where GeometryType(t.geom) in ('LINESTRING', 'MULTILINESTRING')
                ) as line_rows,
                count(*) filter (
                    where t.properties <> '{}'::jsonb
                ) as rows_with_properties,
                count(*) filter (
                    where coalesce(t.name, '') <> ''
                ) as rows_with_name,
                count(distinct t.source_layer) as distinct_source_layers,
                count(*) filter (
                    where GeometryType(t.geom) in ('LINESTRING', 'MULTILINESTRING')
                      and exists (
                        select 1
                        from geo.osm_feature_cache o
                        where GeometryType(o.geom) in ('LINESTRING', 'MULTILINESTRING')
                          and ST_DWithin(o.geom::geography, t.geom::geography, 12.0)
                      )
                ) as line_rows_overlapping_osm
            from geo.tallinn_layer_cache t
            """;

    private final JdbcTemplate jdbcTemplate;

    public GeoRoutingAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GeoRoutingAuditResponse audit() {
        Map<String, Object> osmMap = jdbcTemplate.queryForMap(OSM_AUDIT_SQL);
        Map<String, Object> tallinnMap = jdbcTemplate.queryForMap(TALLINN_AUDIT_SQL);

        GeoRoutingAuditOsmResponse osm = new GeoRoutingAuditOsmResponse(
                asLong(osmMap.get("total_rows")),
                asLong(osmMap.get("line_rows")),
                asLong(osmMap.get("bike_candidate_line_rows")),
                asLong(osmMap.get("line_rows_with_bicycle_tag")),
                asLong(osmMap.get("line_rows_with_cycleway_tag")),
                asLong(osmMap.get("line_rows_with_route_relation_tag")),
                asLong(osmMap.get("line_rows_with_surface_tag")),
                asLong(osmMap.get("line_rows_with_tracktype_tag"))
        );
        GeoRoutingAuditTallinnResponse tallinn = new GeoRoutingAuditTallinnResponse(
                asLong(tallinnMap.get("total_rows")),
                asLong(tallinnMap.get("line_rows")),
                asLong(tallinnMap.get("rows_with_properties")),
                asLong(tallinnMap.get("rows_with_name")),
                asLong(tallinnMap.get("distinct_source_layers")),
                asLong(tallinnMap.get("line_rows_overlapping_osm"))
        );

        GeoRoutingAuditReadinessResponse readiness = buildReadiness(osm, tallinn);
        return new GeoRoutingAuditResponse(Instant.now(), osm, tallinn, readiness);
    }

    private GeoRoutingAuditReadinessResponse buildReadiness(
            GeoRoutingAuditOsmResponse osm,
            GeoRoutingAuditTallinnResponse tallinn) {
        boolean readyForOsmOnly = osm.lineRows() > 0 && osm.bikeCandidateLineRows() > 0;
        double osmBikeCoverageRatio = ratio(osm.bikeCandidateLineRows(), osm.lineRows());
        double tallinnOverlapRatio = ratio(tallinn.lineRowsOverlappingOsm(), tallinn.lineRows());

        List<String> gaps = new ArrayList<>();
        if (osm.lineRows() == 0) {
            gaps.add("OSM cache has no line geometries. Refresh OSM cache before building routing graph.");
        }
        if (osm.bikeCandidateLineRows() == 0) {
            gaps.add("OSM cache has no bike-candidate line features with current filter criteria.");
        }
        if (tallinn.totalRows() == 0) {
            gaps.add("Tallinn layer cache is empty. Refresh Tallinn source to include local cycling lanes.");
        } else if (tallinn.lineRows() == 0) {
            gaps.add("Tallinn layer cache has no line geometries that can be merged into a routing graph.");
        }
        if (tallinn.lineRows() > 0 && tallinn.lineRowsOverlappingOsm() == 0) {
            gaps.add("Tallinn lines have zero overlap with OSM lines within 12m. Check CRS, geometry quality, or matching logic.");
        }

        return new GeoRoutingAuditReadinessResponse(
                readyForOsmOnly,
                gaps.isEmpty(),
                osmBikeCoverageRatio,
                tallinnOverlapRatio,
                List.copyOf(gaps)
        );
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (double) numerator / (double) denominator;
    }
}
