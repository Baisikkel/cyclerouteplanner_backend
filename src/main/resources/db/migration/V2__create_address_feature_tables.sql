CREATE TABLE geo.osm_feature_cache (
    id BIGSERIAL PRIMARY KEY,
    source_id TEXT NOT NULL UNIQUE,
    name TEXT,
    feature_type TEXT,
    tags JSONB NOT NULL DEFAULT '{}'::jsonb,
    geom geometry(Geometry, 4326) NOT NULL,
    raw_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_osm_feature_cache_geom
    ON geo.osm_feature_cache
    USING GIST (geom);

CREATE INDEX idx_osm_feature_cache_feature_type
    ON geo.osm_feature_cache (feature_type);

CREATE TABLE geo.tallinn_layer_cache (
    id BIGSERIAL PRIMARY KEY,
    source_layer TEXT NOT NULL,
    source_id TEXT NOT NULL,
    name TEXT,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    geom geometry(Geometry, 4326) NOT NULL,
    raw_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tallinn_layer_source UNIQUE (source_layer, source_id)
);

CREATE INDEX idx_tallinn_layer_cache_geom
    ON geo.tallinn_layer_cache
    USING GIST (geom);

CREATE INDEX idx_tallinn_layer_cache_source_layer
    ON geo.tallinn_layer_cache (source_layer);
