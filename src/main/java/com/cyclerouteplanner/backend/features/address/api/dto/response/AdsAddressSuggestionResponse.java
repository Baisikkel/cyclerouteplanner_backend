package com.cyclerouteplanner.backend.features.address.api.dto.response;

public record AdsAddressSuggestionResponse(
        String id,
        String label,
        String address,
        String settlement,
        String municipality,
        String county,
        double lat,
        double lon
) {
}
