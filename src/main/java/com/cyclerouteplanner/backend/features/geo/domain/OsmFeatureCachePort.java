package com.cyclerouteplanner.backend.features.geo.domain;

public interface OsmFeatureCachePort {

    void upsert(OsmFeatureCacheEntry entry);
}
