CREATE TABLE geo.routing_edge_cache (
    id BIGSERIAL PRIMARY KEY,
    source_key TEXT NOT NULL UNIQUE,
    origin_source TEXT NOT NULL,
    osm_source_id TEXT,
    tallinn_source_id TEXT,
    profile_hint TEXT NOT NULL,
    merge_type TEXT NOT NULL,
    quality_score DOUBLE PRECISION NOT NULL,
    geom geometry(Geometry, 4326) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_edge_cache_geom
    ON geo.routing_edge_cache
    USING GIST (geom);

CREATE INDEX idx_routing_edge_cache_merge_type
    ON geo.routing_edge_cache (merge_type);

CREATE INDEX idx_routing_edge_cache_origin_source
    ON geo.routing_edge_cache (origin_source);

CREATE INDEX idx_routing_edge_cache_active
    ON geo.routing_edge_cache (active);
