package com.cyclerouteplanner.backend.features.geo.infra;

import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCacheEntry;
import com.cyclerouteplanner.backend.features.geo.domain.TallinnLayerCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@Profile({"local", "docker"})
public class PostgresTallinnLayerCacheRepository implements TallinnLayerCachePort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostgresTallinnLayerCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(TallinnLayerCacheEntry entry) {
        jdbcTemplate.update(
                """
                insert into geo.tallinn_layer_cache (
                    source_layer,
                    source_id,
                    name,
                    properties,
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
                on conflict (source_layer, source_id) do update set
                    name = excluded.name,
                    properties = excluded.properties,
                    geom = excluded.geom,
                    raw_payload = excluded.raw_payload,
                    updated_at = now()
                """,
                entry.sourceLayer(),
                entry.sourceId(),
                entry.name(),
                toJson(entry.properties()),
                entry.wktGeometry(),
                entry.rawPayload()
        );
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize Tallinn properties", ex);
        }
    }
}
