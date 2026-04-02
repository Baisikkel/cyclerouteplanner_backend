package com.cyclerouteplanner.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseMigrationIntegrationTest extends AbstractPostgisIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsCreatePostgisAndFeatureSchemas() {
        Integer postgisInstalled = jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = 'postgis'",
                Integer.class
        );
        Integer osmSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'osm'",
                Integer.class
        );
        Integer addressSchemaExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = 'address'",
                Integer.class
        );
        Integer osmTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'osm' and table_name = 'osm_place_cache'",
                Integer.class
        );
        Integer addressTableExists = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'address' and table_name = 'ads_address_cache'",
                Integer.class
        );

        assertEquals(1, postgisInstalled);
        assertEquals(1, osmSchemaExists);
        assertEquals(1, addressSchemaExists);
        assertEquals(1, osmTableExists);
        assertEquals(1, addressTableExists);
    }
}
