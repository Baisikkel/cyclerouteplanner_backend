CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SCHEMA IF NOT EXISTS meta;
CREATE SCHEMA IF NOT EXISTS address;
CREATE SCHEMA IF NOT EXISTS geo;

CREATE TABLE meta.data_snapshot (
    id BIGSERIAL PRIMARY KEY,
    source TEXT NOT NULL,
    source_version TEXT,
    source_timestamp TIMESTAMPTZ,
    checksum TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_data_snapshot_source_version UNIQUE (source, source_version)
);

CREATE TABLE address.ads_address_cache (
    id BIGSERIAL PRIMARY KEY,
    ads_oid TEXT NOT NULL UNIQUE,
    full_address TEXT NOT NULL,
    normalized_address TEXT,
    etak_code TEXT,
    location geometry(Point, 4326),
    raw_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ads_address_cache_location
    ON address.ads_address_cache
    USING GIST (location);

CREATE INDEX idx_ads_address_cache_normalized_address
    ON address.ads_address_cache (normalized_address);
