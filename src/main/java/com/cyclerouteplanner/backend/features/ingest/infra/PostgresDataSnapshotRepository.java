package com.cyclerouteplanner.backend.features.ingest.infra;

import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotPort;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@Profile({"local", "docker"})
public class PostgresDataSnapshotRepository implements DataSnapshotPort {

    private static final TypeReference<Map<String, Object>> METADATA_MAP_TYPE = new TypeReference<>() {
    };
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostgresDataSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DataSnapshotRecord upsert(
            String source,
            String sourceVersion,
            Instant sourceTimestamp,
            String checksum,
            Map<String, Object> metadata
    ) {
        String metadataJson = toJson(metadata);
        return jdbcTemplate.queryForObject(
                """
                insert into meta.data_snapshot (source, source_version, source_timestamp, checksum, metadata)
                values (?, ?, ?, ?, cast(? as jsonb))
                on conflict (source, source_version) do update
                    set source_timestamp = excluded.source_timestamp,
                        checksum = excluded.checksum,
                        metadata = excluded.metadata
                returning source, source_version, source_timestamp, checksum, metadata::text, created_at
                """,
                rowMapper(),
                source,
                sourceVersion,
                Timestamp.from(sourceTimestamp),
                checksum,
                metadataJson
        );
    }

    @Override
    public List<DataSnapshotRecord> findLatest(int limit) {
        return jdbcTemplate.query(
                """
                select source, source_version, source_timestamp, checksum, metadata::text, created_at
                from meta.data_snapshot
                order by created_at desc
                limit ?
                """,
                rowMapper(),
                limit
        );
    }

    private RowMapper<DataSnapshotRecord> rowMapper() {
        return (rs, rowNum) -> new DataSnapshotRecord(
                rs.getString("source"),
                rs.getString("source_version"),
                getInstant(rs, "source_timestamp"),
                rs.getString("checksum"),
                fromJson(rs.getString("metadata")),
                getInstant(rs, "created_at")
        );
    }

    private Instant getInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize snapshot metadata", ex);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, METADATA_MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize snapshot metadata", ex);
        }
    }
}
