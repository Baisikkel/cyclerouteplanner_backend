CREATE SCHEMA IF NOT EXISTS routing;

CREATE TABLE routing.route_option (
    id BIGSERIAL PRIMARY KEY,
    source_id TEXT NOT NULL UNIQUE,
    origin_source TEXT NOT NULL,
    name TEXT,
    profile_hint TEXT NOT NULL,
    quality_score DOUBLE PRECISION NOT NULL,
    enrichment_type TEXT NOT NULL,
    geom geometry(Geometry, 4326) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_route_option_geom
    ON routing.route_option
    USING GIST (geom);

CREATE INDEX idx_route_option_profile_hint
    ON routing.route_option (profile_hint);

CREATE INDEX idx_route_option_enrichment_type
    ON routing.route_option (enrichment_type);

CREATE INDEX idx_route_option_active
    ON routing.route_option (active);
