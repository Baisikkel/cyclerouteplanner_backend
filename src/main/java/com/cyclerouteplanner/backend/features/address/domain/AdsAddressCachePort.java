package com.cyclerouteplanner.backend.features.address.domain;

public interface AdsAddressCachePort {

    void upsert(AdsAddressCacheEntry entry);
}
