package com.cyclerouteplanner.backend.features.address.domain;

public record AdsAddressSuggestion(
        String id,
        String label,
        String address,
        String settlement,
        String municipality,
        String county,
        double latitude,
        double longitude
) {
}
