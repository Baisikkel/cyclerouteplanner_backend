package com.cyclerouteplanner.backend.features.routing.infra;

import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionPort;
import com.cyclerouteplanner.backend.features.routing.domain.RouteOptionRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile({"local", "docker"})
public class PostgresRouteOptionRepository implements RouteOptionPort {

    private static final String REBUILD_SQL = """
            with candidates as (
                select
                    o.source_id,
                    o.name,
                    o.feature_type,
                    o.tags,
                    ST_LineMerge(ST_CollectionExtract(o.geom, 2)) as geom,
                    exists (
                        select 1
                        from geo.tallinn_layer_cache t
                        where ST_DWithin(o.geom::geography, t.geom::geography, 12.0)
                    ) as tallinn_overlap
                from geo.osm_feature_cache o
                where GeometryType(o.geom) in ('LINESTRING', 'MULTILINESTRING')
                  and (
                    o.feature_type in (
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
                    or o.tags ? 'cycleway'
                    or coalesce(o.tags ->> 'bicycle', '') in ('yes', 'designated', 'permissive')
                  )
            ),
            normalized as (
                select
                    source_id,
                    name,
                    geom,
                    case
                        when coalesce(tags ->> 'surface', '') in ('gravel', 'fine_gravel', 'compacted', 'dirt', 'ground')
                             or coalesce(tags ->> 'tracktype', '') in ('grade2', 'grade3', 'grade4', 'grade5')
                            then 'gravel'
                        when feature_type in ('primary', 'primary_link', 'secondary', 'secondary_link')
                            then 'safety'
                        else 'fastbike'
                    end as profile_hint,
                    case
                        when tallinn_overlap then 'osm_plus_tallinn'
                        else 'osm_only'
                    end as enrichment_type,
                    least(
                        1.0,
                        0.45
                        + case
                            when feature_type = 'cycleway' then 0.25
                            when coalesce(tags ->> 'cycleway', '') <> '' then 0.15
                            else 0
                          end
                        + case
                            when coalesce(tags ->> 'bicycle', '') in ('yes', 'designated', 'permissive') then 0.1
                            else 0
                          end
                        + case
                            when tallinn_overlap then 0.2
                            else 0
                          end
                    ) as quality_score,
                    jsonb_build_object(
                        'featureType', feature_type,
                        'highway', tags ->> 'highway',
                        'surface', tags ->> 'surface',
                        'cycleway', tags ->> 'cycleway',
                        'bicycle', tags ->> 'bicycle',
                        'tallinnOverlap', tallinn_overlap
                    ) as metadata
                from candidates
                where geom is not null and not ST_IsEmpty(geom)
            )
            insert into routing.route_option (
                source_id,
                origin_source,
                name,
                profile_hint,
                quality_score,
                enrichment_type,
                geom,
                metadata,
                active
            )
            select
                n.source_id,
                'osm',
                n.name,
                n.profile_hint,
                n.quality_score,
                n.enrichment_type,
                n.geom,
                n.metadata,
                true
            from normalized n
            on conflict (source_id) do update set
                origin_source = excluded.origin_source,
                name = excluded.name,
                profile_hint = excluded.profile_hint,
                quality_score = excluded.quality_score,
                enrichment_type = excluded.enrichment_type,
                geom = excluded.geom,
                metadata = excluded.metadata,
                active = true,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresRouteOptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int rebuildFromGeoCaches() {
        return jdbcTemplate.update(REBUILD_SQL);
    }

    @Override
    public List<RouteOptionRecord> findActive(int limit) {
        return jdbcTemplate.query(
                """
                select id, source_id, name, profile_hint, quality_score, enrichment_type, st_astext(geom) as wkt_geom
                from routing.route_option
                where active = true
                order by quality_score desc, id asc
                limit ?
                """,
                (rs, rowNum) -> new RouteOptionRecord(
                        rs.getLong("id"),
                        rs.getString("source_id"),
                        rs.getString("name"),
                        rs.getString("profile_hint"),
                        rs.getDouble("quality_score"),
                        rs.getString("enrichment_type"),
                        rs.getString("wkt_geom")
                ),
                limit
        );
    }

    @Override
    public Optional<String> findBestProfileHint(double startLat, double startLon, double endLat, double endLon) {
        List<String> hints = jdbcTemplate.query(
                """
                with input_points as (
                    select
                        ST_SetSRID(ST_Point(?, ?), 4326) as start_point,
                        ST_SetSRID(ST_Point(?, ?), 4326) as end_point
                )
                select ro.profile_hint
                from routing.route_option ro, input_points ip
                where ro.active = true
                  and (
                    ST_DWithin(ro.geom::geography, ip.start_point::geography, 2000.0)
                    or ST_DWithin(ro.geom::geography, ip.end_point::geography, 2000.0)
                  )
                order by
                    (
                        ST_Distance(ro.geom::geography, ip.start_point::geography)
                        + ST_Distance(ro.geom::geography, ip.end_point::geography)
                    ) / greatest(ro.quality_score, 0.05) asc,
                    ro.quality_score desc,
                    ro.id asc
                limit 1
                """,
                (rs, rowNum) -> rs.getString("profile_hint"),
                startLon,
                startLat,
                endLon,
                endLat
        );
        return hints.stream().findFirst();
    }
}
