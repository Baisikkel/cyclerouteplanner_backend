package com.cyclerouteplanner.backend.features.address.domain;

public record AdsAddressCacheEntry(
        String adsOid,
        String fullAddress,
        String normalizedAddress,
        String etakCode,
        Double longitude,
        Double latitude,
        String rawPayload
) {
}
