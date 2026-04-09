package com.cyclerouteplanner.backend.features.geo.domain;

public interface TallinnLayerCachePort {

    void upsert(TallinnLayerCacheEntry entry);
}
