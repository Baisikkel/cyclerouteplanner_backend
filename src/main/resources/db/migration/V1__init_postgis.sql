CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SCHEMA IF NOT EXISTS osm;

CREATE TABLE osm.osm_place_cache (
    id BIGSERIAL PRIMARY KEY,
    source_type TEXT NOT NULL,
    source_id TEXT NOT NULL,
    name TEXT,
    tags JSONB NOT NULL DEFAULT '{}'::jsonb,
    location geometry(Point, 4326),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_osm_place_cache_source UNIQUE (source_type, source_id)
);

CREATE INDEX idx_osm_place_cache_location
    ON osm.osm_place_cache
    USING GIST (location);
