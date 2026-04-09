package com.cyclerouteplanner.backend.features.geo.application;

import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheSourceStatusResponse;
import com.cyclerouteplanner.backend.features.geo.api.dto.response.GeoCacheStatusResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Service
@Profile({"local", "docker"})
public class GeoCacheStatusService {

    private final JdbcTemplate jdbcTemplate;

    public GeoCacheStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GeoCacheStatusResponse status() {
        GeoCacheSourceStatusResponse ads = buildSourceStatus(
                "maaamet_inads",
                "address.ads_address_cache"
        );
        GeoCacheSourceStatusResponse osm = buildSourceStatus(
                "osm_geofabrik_estonia",
                "geo.osm_feature_cache"
        );
        GeoCacheSourceStatusResponse tallinn = buildSourceStatus(
                "tallinn_open_data",
                "geo.tallinn_layer_cache"
        );

        return new GeoCacheStatusResponse(Instant.now(), ads, osm, tallinn);
    }

    private GeoCacheSourceStatusResponse buildSourceStatus(String source, String tableName) {
        Long rowCountResult = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        long rowCount = rowCountResult == null ? 0L : rowCountResult;

        SnapshotTimes times = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                """
                select max(created_at) as created_at, max(source_timestamp) as source_timestamp
                from meta.data_snapshot
                where source = ?
                """,
                (resultSet, rowNum) -> new SnapshotTimes(
                        timestampToInstant(resultSet, "created_at"),
                        timestampToInstant(resultSet, "source_timestamp")
                ),
                source
                ),
                "Snapshot query returned null"
        );

        return new GeoCacheSourceStatusResponse(
                source,
                rowCount,
                times.createdAt(),
                times.sourceTimestamp()
        );
    }

    private Instant timestampToInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private record SnapshotTimes(Instant createdAt, Instant sourceTimestamp) {
    }
}
