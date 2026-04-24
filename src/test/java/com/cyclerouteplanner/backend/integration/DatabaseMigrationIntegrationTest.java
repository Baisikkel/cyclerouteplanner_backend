package com.cyclerouteplanner.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseMigrationIntegrationTest extends AbstractPostgisIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsCreatePostgisAndMvpDataSchemas() {
        Integer postgisInstalled = jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = 'postgis'",
                Integer.class
        );
        Integer metaSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'meta'",
                Integer.class
        );
        Integer addressSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'address'",
                Integer.class
        );
        Integer geoSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'geo'",
                Integer.class
        );
        Integer routingSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'routing'",
                Integer.class
        );
        Integer dataSnapshotTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'meta' and table_name = 'data_snapshot'",
                Integer.class
        );
        Integer addressCacheTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'address' and table_name = 'ads_address_cache'",
                Integer.class
        );
        Integer osmFeatureCacheTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'geo' and table_name = 'osm_feature_cache'",
                Integer.class
        );
        Integer tallinnLayerCacheTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'geo' and table_name = 'tallinn_layer_cache'",
                Integer.class
        );
        Integer routingEdgeCacheTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'geo' and table_name = 'routing_edge_cache'",
                Integer.class
        );
        Integer routeOptionTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'routing' and table_name = 'route_option'",
                Integer.class
        );

        assertEquals(1, postgisInstalled);
        assertEquals(1, metaSchemaExists);
        assertEquals(1, addressSchemaExists);
        assertEquals(1, geoSchemaExists);
        assertEquals(1, routingSchemaExists);
        assertEquals(1, dataSnapshotTableExists);
        assertEquals(1, addressCacheTableExists);
        assertEquals(1, osmFeatureCacheTableExists);
        assertEquals(1, tallinnLayerCacheTableExists);
        assertEquals(1, routingEdgeCacheTableExists);
        assertEquals(1, routeOptionTableExists);
    }
}
