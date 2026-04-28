package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeBuildStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeExportStatus;
import com.cyclerouteplanner.backend.features.geo.domain.GeoRoutingEdgeStatus;
import com.cyclerouteplanner.backend.features.geo.infra.GeoRoutingEdgeProperties;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

@Service
@Profile({"local", "docker"})
public class GeoRoutingEdgeBuildService {

    private static final String SNAPSHOT_SOURCE = "routing_edges";
    private static final String STATUS_SOURCE = "osm_with_optional_tallinn_merge";
    private static final String EXPORT_SOURCE = "routing_edge_cache";
    private static final String EXPORT_PSEUDO_TAGS_SOURCE = "routing_edge_cache_pseudo_tags";

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

    private static final String EXPORT_SQL = """
            select
                source_key,
                origin_source,
                osm_source_id,
                tallinn_source_id,
                profile_hint,
                merge_type,
                quality_score,
                ST_AsGeoJSON(geom, 6) as geom_geojson,
                metadata::text as metadata_json
            from geo.routing_edge_cache
            where active = true
            order by id asc
            """;

    private static final String EXPORT_PSEUDO_TAGS_SQL = """
            select
                osm_source_id,
                merge_type,
                profile_hint,
                quality_score
            from geo.routing_edge_cache
            where active = true
              and osm_source_id is not null
            order by id asc
            """;

    private final JdbcTemplate jdbcTemplate;
    private final DataSnapshotPort dataSnapshotPort;
    private final GeoRoutingEdgeProperties geoRoutingEdgeProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeoRoutingEdgeBuildService(
            JdbcTemplate jdbcTemplate,
            DataSnapshotPort dataSnapshotPort,
            GeoRoutingEdgeProperties geoRoutingEdgeProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSnapshotPort = dataSnapshotPort;
        this.geoRoutingEdgeProperties = geoRoutingEdgeProperties;
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

    public GeoRoutingEdgeExportStatus exportActiveEdgesToBrouterInput() {
        String configuredPath = geoRoutingEdgeProperties.getExportOutputPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            return new GeoRoutingEdgeExportStatus(
                    false,
                    EXPORT_SOURCE,
                    0,
                    null,
                    "Missing geo.routing-edges.export-output-path configuration",
                    Instant.now()
            );
        }

        Path outputPath = Path.of(configuredPath).toAbsolutePath().normalize();
        Path tempPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");
        AtomicInteger exportedCount = new AtomicInteger(0);
        AtomicBoolean firstFeature = new AtomicBoolean(true);

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                writer.write("{\"type\":\"FeatureCollection\",\"name\":\"routing_edge_cache\",\"features\":[");

                jdbcTemplate.query(EXPORT_SQL, rs -> {
                    try {
                        if (!firstFeature.get()) {
                            writer.write(",");
                        }
                        firstFeature.set(false);

                        String geometry = rs.getString("geom_geojson");
                        String sourceKey = rs.getString("source_key");
                        String originSource = rs.getString("origin_source");
                        String osmSourceId = rs.getString("osm_source_id");
                        String tallinnSourceId = rs.getString("tallinn_source_id");
                        String profileHint = rs.getString("profile_hint");
                        String mergeType = rs.getString("merge_type");
                        double qualityScore = rs.getDouble("quality_score");
                        String metadataJson = rs.getString("metadata_json");
                        String metadata = safeJsonObject(metadataJson);

                        writer.write("{\"type\":\"Feature\",\"geometry\":");
                        writer.write(geometry == null ? "null" : geometry);
                        writer.write(",\"properties\":{");
                        writer.write("\"sourceKey\":\"");
                        writer.write(escapeJson(sourceKey));
                        writer.write("\",\"originSource\":\"");
                        writer.write(escapeJson(originSource));
                        writer.write("\",\"osmSourceId\":");
                        writeNullableString(writer, osmSourceId);
                        writer.write(",\"tallinnSourceId\":");
                        writeNullableString(writer, tallinnSourceId);
                        writer.write(",\"profileHint\":\"");
                        writer.write(escapeJson(profileHint));
                        writer.write("\",\"mergeType\":\"");
                        writer.write(escapeJson(mergeType));
                        writer.write("\",\"qualityScore\":");
                        writer.write(Double.toString(qualityScore));
                        writer.write(",\"metadata\":");
                        writer.write(metadata);
                        writer.write("}}");
                        exportedCount.incrementAndGet();
                    } catch (IOException ioException) {
                        throw new IllegalStateException("Failed to write routing edge export file", ioException);
                    }
                });

                writer.write("]}");
            }

            Files.move(tempPath, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return new GeoRoutingEdgeExportStatus(
                    true,
                    EXPORT_SOURCE,
                    exportedCount.get(),
                    outputPath.toString(),
                    "Routing edge export completed",
                    Instant.now()
            );
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
            return new GeoRoutingEdgeExportStatus(
                    false,
                    EXPORT_SOURCE,
                    0,
                    outputPath.toString(),
                    ex.getMessage(),
                    Instant.now()
            );
        }
    }

    public GeoRoutingEdgeExportStatus exportPseudoTagsForSegmentBuild() {
        String configuredPath = geoRoutingEdgeProperties.getExportPseudoTagsOutputPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            return new GeoRoutingEdgeExportStatus(
                    false,
                    EXPORT_PSEUDO_TAGS_SOURCE,
                    0,
                    null,
                    "Missing geo.routing-edges.export-pseudo-tags-output-path configuration",
                    Instant.now()
            );
        }

        Path outputPath = Path.of(configuredPath).toAbsolutePath().normalize();
        Path tempPath = outputPath.resolveSibling(outputPath.getFileName() + ".tmp");
        Map<Long, PseudoTagRow> rowsByOsmWayId = new LinkedHashMap<>();

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            jdbcTemplate.query(EXPORT_PSEUDO_TAGS_SQL, rs -> {
                Long osmWayId = extractOsmWayId(rs.getString("osm_source_id"));
                if (osmWayId == null) {
                    return;
                }

                String mergeType = rs.getString("merge_type");
                double qualityScore = rs.getDouble("quality_score");
                String profileHint = rs.getString("profile_hint");

                String trafficClass = null;
                String noiseClass = null;
                if ("osm_plus_tallinn".equals(mergeType)) {
                    if (qualityScore >= 0.9d) {
                        trafficClass = "1";
                    } else if (qualityScore >= 0.75d) {
                        trafficClass = "2";
                    } else {
                        trafficClass = "3";
                    }
                    noiseClass = "safety".equals(profileHint) ? "1" : "2";
                }

                if (trafficClass == null && noiseClass == null) {
                    return;
                }

                PseudoTagRow existing = rowsByOsmWayId.get(osmWayId);
                if (existing == null) {
                    rowsByOsmWayId.put(osmWayId, new PseudoTagRow(noiseClass, null, null, null, trafficClass));
                } else {
                    rowsByOsmWayId.put(
                            osmWayId,
                            new PseudoTagRow(
                                    minPseudoClass(existing.noiseClass(), noiseClass),
                                    minPseudoClass(existing.riverClass(), null),
                                    minPseudoClass(existing.forestClass(), null),
                                    minPseudoClass(existing.townClass(), null),
                                    minPseudoClass(existing.trafficClass(), trafficClass)
                            )
                    );
                }
            });

            try (OutputStream fileOutput = Files.newOutputStream(tempPath);
                 OutputStream wrappedOutput = outputPath.toString().endsWith(".gz")
                         ? new GZIPOutputStream(fileOutput)
                         : fileOutput;
                 BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(wrappedOutput, StandardCharsets.UTF_8))) {
                writer.write("#####waytags#####");
                writer.newLine();

                for (Map.Entry<Long, PseudoTagRow> entry : rowsByOsmWayId.entrySet()) {
                    PseudoTagRow row = entry.getValue();
                    writer.write(Long.toString(entry.getKey()));
                    writer.write(";");
                    writer.write(nullToEmpty(row.noiseClass()));
                    writer.write(";");
                    writer.write(nullToEmpty(row.riverClass()));
                    writer.write(";");
                    writer.write(nullToEmpty(row.forestClass()));
                    writer.write(";");
                    writer.write(nullToEmpty(row.townClass()));
                    writer.write(";");
                    writer.write(nullToEmpty(row.trafficClass()));
                    writer.newLine();
                }
            }

            Files.move(tempPath, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return new GeoRoutingEdgeExportStatus(
                    true,
                    EXPORT_PSEUDO_TAGS_SOURCE,
                    rowsByOsmWayId.size(),
                    outputPath.toString(),
                    "Routing edge pseudo-tag export completed",
                    Instant.now()
            );
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
            return new GeoRoutingEdgeExportStatus(
                    false,
                    EXPORT_PSEUDO_TAGS_SOURCE,
                    0,
                    outputPath.toString(),
                    ex.getMessage(),
                    Instant.now()
            );
        }
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

    private String safeJsonObject(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            if (!node.isObject()) {
                return "{}";
            }
            return node.toString();
        } catch (Exception ex) {
            return "{}";
        }
    }

    private void writeNullableString(BufferedWriter writer, String value) throws IOException {
        if (value == null) {
            writer.write("null");
            return;
        }
        writer.write("\"");
        writer.write(escapeJson(value));
        writer.write("\"");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Long extractOsmWayId(String osmSourceId) {
        if (osmSourceId == null || osmSourceId.isBlank()) {
            return null;
        }
        int slashIndex = osmSourceId.lastIndexOf('/');
        String numericPart = slashIndex >= 0 ? osmSourceId.substring(slashIndex + 1) : osmSourceId;
        try {
            return Long.parseLong(numericPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String minPseudoClass(String current, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return candidate;
        }
        try {
            int currentValue = Integer.parseInt(current);
            int candidateValue = Integer.parseInt(candidate);
            return Integer.toString(Math.min(currentValue, candidateValue));
        } catch (NumberFormatException ex) {
            return current;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record PseudoTagRow(
            String noiseClass,
            String riverClass,
            String forestClass,
            String townClass,
            String trafficClass
    ) {
    }
}
