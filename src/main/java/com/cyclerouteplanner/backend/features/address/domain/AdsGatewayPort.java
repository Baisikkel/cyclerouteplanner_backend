package com.cyclerouteplanner.backend.features.address.domain;

import java.util.List;

public interface AdsGatewayPort {

    String fetchStatus();

    List<AdsAddressSuggestion> search(String query, int limit);

    String searchRaw(String query, int limit);
}
