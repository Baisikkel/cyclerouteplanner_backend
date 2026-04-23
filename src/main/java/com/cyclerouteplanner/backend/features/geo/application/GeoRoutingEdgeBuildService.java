package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@Profile({"local", "docker"})
public class GeoRoutingEdgeBuildService {

    private static final String SNAPSHOT_SOURCE = "routing_edges";
    private static final String STATUS_SOURCE = "osm_with_optional_tallinn_merge";

    private static final String REBUILD_SQL = """
            with deactivated as (
                update geo.routing_edge_cache
                set active = false, updated_at = now()
                where active = true
            ),
            osm_candidates as (
                select
                    o.source_id,
                    o.name,
                    o.feature_type,
                    o.tags,
                    ST_LineMerge(ST_CollectionExtract(o.geom, 2)) as geom
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
            osm_with_tallinn_match as (
                select
                    o.source_id,
                    o.name,
                    o.feature_type,
                    o.tags,
                    o.geom,
                    t.source_layer as tallinn_source_layer,
                    t.source_id as tallinn_source_id
                from osm_candidates o
                left join lateral (
                    select tl.source_layer, tl.source_id
                    from geo.tallinn_layer_cache tl
                    where GeometryType(tl.geom) in ('LINESTRING', 'MULTILINESTRING')
                      and ST_DWithin(o.geom::geography, tl.geom::geography, 12.0)
                    order by ST_Distance(o.geom::geography, tl.geom::geography)
                    limit 1
                ) t on true
                where o.geom is not null and not ST_IsEmpty(o.geom)
            ),
            upsert_osm as (
                insert into geo.routing_edge_cache (
                    source_key,
                    origin_source,
                    osm_source_id,
                    tallinn_source_id,
                    profile_hint,
                    merge_type,
                    quality_score,
                    geom,
                    metadata,
                    active
                )
                select
                    'osm:' || o.source_id,
                    'osm',
                    o.source_id,
                    case
                        when o.tallinn_source_id is null then null
                        else o.tallinn_source_layer || '/' || o.tallinn_source_id
                    end,
                    case
                        when coalesce(o.tags ->> 'surface', '') in ('gravel', 'fine_gravel', 'compacted', 'dirt', 'ground')
                             or coalesce(o.tags ->> 'tracktype', '') in ('grade2', 'grade3', 'grade4', 'grade5')
                            then 'gravel'
                        when o.feature_type in ('primary', 'primary_link', 'secondary', 'secondary_link')
                            then 'safety'
                        else 'fastbike'
                    end,
                    case
                        when o.tallinn_source_id is null then 'osm'
                        else 'osm_plus_tallinn'
                    end,
                    least(
                        1.0,
                        0.45
                        + case
                            when o.feature_type = 'cycleway' then 0.25
                            when coalesce(o.tags ->> 'cycleway', '') <> '' then 0.15
                            else 0
                          end
                        + case
                            when coalesce(o.tags ->> 'bicycle', '') in ('yes', 'designated', 'permissive') then 0.1
                            else 0
                          end
                        + case
                            when o.tallinn_source_id is not null then 0.2
                            else 0
                          end
                    ),
                    o.geom,
                    jsonb_build_object(
                        'name', o.name,
                        'featureType', o.feature_type,
                        'highway', o.tags ->> 'highway',
                        'surface', o.tags ->> 'surface',
                        'cycleway', o.tags ->> 'cycleway',
                        'bicycle', o.tags ->> 'bicycle',
                        'tallinnSourceLayer', o.tallinn_source_layer,
                        'tallinnSourceId', o.tallinn_source_id
                    ),
                    true
                from osm_with_tallinn_match o
                on conflict (source_key) do update set
                    origin_source = excluded.origin_source,
                    osm_source_id = excluded.osm_source_id,
                    tallinn_source_id = excluded.tallinn_source_id,
                    profile_hint = excluded.profile_hint,
                    merge_type = excluded.merge_type,
                    quality_score = excluded.quality_score,
                    geom = excluded.geom,
                    metadata = excluded.metadata,
                    active = true,
                    updated_at = now()
                returning merge_type
            ),
            tallinn_candidates as (
                select
                    t.source_layer,
                    t.source_id,
                    t.name,
                    t.properties,
                    ST_LineMerge(ST_CollectionExtract(t.geom, 2)) as geom
                from geo.tallinn_layer_cache t
                where GeometryType(t.geom) in ('LINESTRING', 'MULTILINESTRING')
            ),
            tallinn_only as (
                select tc.*
                from tallinn_candidates tc
                where tc.geom is not null
                  and not ST_IsEmpty(tc.geom)
                  and not exists (
                        select 1
                        from osm_candidates o
                        where ST_DWithin(o.geom::geography, tc.geom::geography, 12.0)
                  )
            ),
            upsert_tallinn as (
                insert into geo.routing_edge_cache (
                    source_key,
                    origin_source,
                    osm_source_id,
                    tallinn_source_id,
                    profile_hint,
                    merge_type,
                    quality_score,
                    geom,
                    metadata,
                    active
                )
                select
                    'tallinn:' || t.source_layer || ':' || t.source_id,
                    'tallinn',
                    null,
                    t.source_layer || '/' || t.source_id,
                    case
                        when coalesce(t.properties ->> 'surface', '') in ('gravel', 'fine_gravel', 'compacted', 'dirt', 'ground')
                            then 'gravel'
                        else 'safety'
                    end,
                    'tallinn_only',
                    0.75,
                    t.geom,
                    jsonb_build_object(
                        'name', t.name,
                        'sourceLayer', t.source_layer,
                        'sourceId', t.source_id,
                        'properties', t.properties
                    ),
                    true
                from tallinn_only t
                on conflict (source_key) do update set
                    origin_source = excluded.origin_source,
                    osm_source_id = excluded.osm_source_id,
                    tallinn_source_id = excluded.tallinn_source_id,
                    profile_hint = excluded.profile_hint,
                    merge_type = excluded.merge_type,
                    quality_score = excluded.quality_score,
                    geom = excluded.geom,
                    metadata = excluded.metadata,
                    active = true,
                    updated_at = now()
                returning 1
            )
            select
                (select count(*) from upsert_osm)::int as osm_upserted,
                (select count(*) from upsert_osm where merge_type = 'osm_plus_tallinn')::int as osm_plus_tallinn_upserted,
                (select count(*) from upsert_tallinn)::int as tallinn_only_upserted
            """;

    private static final String STATUS_SQL = """
            select
                count(*) filter (where active = true) as active_total_count,
                count(*) filter (where active = true and merge_type = 'osm') as active_osm_count,
                count(*) filter (where active = true and merge_type = 'osm_plus_tallinn') as active_osm_plus_tallinn_count,
                count(*) filter (where active = true and merge_type = 'tallinn_only') as active_tallinn_only_count
            from geo.routing_edge_cache
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DataSnapshotPort dataSnapshotPort;

    public GeoRoutingEdgeBuildService(JdbcTemplate jdbcTemplate, DataSnapshotPort dataSnapshotPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSnapshotPort = dataSnapshotPort;
    }

    public GeoRoutingEdgeBuildStatus rebuildFromGeoCaches() {
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(REBUILD_SQL);
            int osmUpserted = asInt(result.get("osm_upserted"));
            int osmPlusTallinnUpserted = asInt(result.get("osm_plus_tallinn_upserted"));
            int tallinnOnlyUpserted = asInt(result.get("tallinn_only_upserted"));
            int totalUpserted = osmUpserted + tallinnOnlyUpserted;

            dataSnapshotPort.upsert(
                    SNAPSHOT_SOURCE,
                    LocalDate.now(ZoneOffset.UTC).toString(),
                    Instant.now(),
                    null,
                    Map.of(
                            "strategy", "osm_base_tallinn_overlay",
                            "osmUpserted", osmUpserted,
                            "osmPlusTallinnUpserted", osmPlusTallinnUpserted,
                            "tallinnOnlyUpserted", tallinnOnlyUpserted,
                            "totalUpserted", totalUpserted
                    )
            );

            return new GeoRoutingEdgeBuildStatus(
                    true,
                    STATUS_SOURCE,
                    osmUpserted,
                    osmPlusTallinnUpserted,
                    tallinnOnlyUpserted,
                    totalUpserted,
                    "Routing edge rebuild completed",
                    Instant.now()
            );
        } catch (RuntimeException ex) {
            return new GeoRoutingEdgeBuildStatus(
                    false,
                    STATUS_SOURCE,
                    0,
                    0,
                    0,
                    0,
                    ex.getMessage(),
                    Instant.now()
            );
        }
    }

    public GeoRoutingEdgeStatus status() {
        Map<String, Object> result = jdbcTemplate.queryForMap(STATUS_SQL);
        return new GeoRoutingEdgeStatus(
                Instant.now(),
                asLong(result.get("active_total_count")),
                asLong(result.get("active_osm_count")),
                asLong(result.get("active_osm_plus_tallinn_count")),
                asLong(result.get("active_tallinn_only_count"))
        );
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
