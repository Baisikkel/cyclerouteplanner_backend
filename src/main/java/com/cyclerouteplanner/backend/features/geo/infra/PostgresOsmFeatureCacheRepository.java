package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.OsmFeatureCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@Profile({"local", "docker"})
public class PostgresOsmFeatureCacheRepository implements OsmFeatureCachePort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostgresOsmFeatureCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(OsmFeatureCacheEntry entry) {
        jdbcTemplate.update(
                """
                insert into geo.osm_feature_cache (
                    source_id,
                    name,
                    feature_type,
                    tags,
                    geom,
                    raw_payload
                )
                values (
                    ?,
                    ?,
                    ?,
                    cast(? as jsonb),
                    ST_SetSRID(ST_GeomFromText(?), 4326),
                    cast(? as jsonb)
                )
                on conflict (source_id) do update set
                    name = excluded.name,
                    feature_type = excluded.feature_type,
                    tags = excluded.tags,
                    geom = excluded.geom,
                    raw_payload = excluded.raw_payload,
                    updated_at = now()
                """,
                entry.sourceId(),
                entry.name(),
                entry.featureType(),
                toJson(entry.tags()),
                entry.wktGeometry(),
                entry.rawPayload()
        );
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize OSM tags", ex);
        }
    }
}
